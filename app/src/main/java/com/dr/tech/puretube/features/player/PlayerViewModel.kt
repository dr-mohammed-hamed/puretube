package com.dr.tech.puretube.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.model.SubtitleTrack
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.PlaybackPrefs
import com.dr.tech.puretube.core.data.repository.PlaylistRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.VideoDetailsRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.player.PurePlayerManager
import com.dr.tech.puretube.features.player.sheets.SleepTimerOption
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

data class PlayerUiState(
    val isLoading: Boolean = true,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val isAudioOnly: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentQuality: PlaybackQuality = PlaybackQuality.AUTO,
    val playbackSpeed: Float = 1.0f,
    val videoDetails: ExtractedVideoDetails? = null,
    val isSubscribed: Boolean = false,
    val isSavedToWatchLater: Boolean = false,
    val relatedVideos: List<FeedVideoItem> = emptyList(),
    val errorMessage: String? = null,
    val toggleErrorMessage: String? = null,
    val isZoomToFill: Boolean = false,
    val activeSleepTimer: SleepTimerOption = SleepTimerOption.OFF
)

data class CaptionUiState(
    val ccEnabled: Boolean = false,
    val selectedLang: String? = null,
    val tracks: List<SubtitleTrack> = emptyList()
)

/**
 * ViewModel managing player state, video details extraction, and database syncing.
 * History loop delegated to [PlayerPlaybackSyncHelper]. Constitution V/VI/IX:
 * viewModelScope only, debounce on toggles, zero double-bang, Result<T>, Long ms.
 *
 * Playback errors ([PlayerUiState.errorMessage]) and bookmark/subscription errors
 * ([PlayerUiState.toggleErrorMessage]) use separate channels so a non-fatal toggle
 * failure never presents as a full-screen playback failure.
 */
class PlayerViewModel(
    val playerManager: PurePlayerManager,
    private val videoDetailsRepository: VideoDetailsRepository,
    private val historyRepository: HistoryRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val playbackPrefs: PlaybackPrefs? = null,
    private val playlistRepository: PlaylistRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _captionUiState = MutableStateFlow(CaptionUiState())
    val captionUiState: StateFlow<CaptionUiState> = _captionUiState.asStateFlow()

    private val _playlistImportState = MutableStateFlow(com.dr.tech.puretube.features.playlists.PlaylistImportState())
    val playlistImportState: StateFlow<com.dr.tech.puretube.features.playlists.PlaylistImportState> = _playlistImportState.asStateFlow()

    val playlistsFlow: StateFlow<List<com.dr.tech.puretube.core.database.entity.PlaylistEntity>> =
        (playlistRepository?.getPlaylistsFlow()
            ?: kotlinx.coroutines.flow.flowOf(emptyList()))
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    private var currentVideoId: String? = null
    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("PlayerViewModel", "Uncaught player coroutine exception intercepted", throwable)
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = "حدث خطأ غير متوقع أثناء تشغيل المقطع"
            )
        }
    }
    private val syncHelper = PlayerPlaybackSyncHelper(historyRepository) { e ->
        android.util.Log.w("PlayerViewModel", "history sync failed", e)
    }
    private var loadVideoJob: Job? = null
    private var subObserveJob: Job? = null
    private var watchLaterObserveJob: Job? = null
    private var channelVideosJob: Job? = null
    private var sleepTimerJob: Job? = null
    private val collectorJobs = CopyOnWriteArrayList<Job>()
    private val togglingSubs = ConcurrentHashMap.newKeySet<String>()
    private val togglingWatch = ConcurrentHashMap.newKeySet<String>()
    private val togglingPlaylist = ConcurrentHashMap.newKeySet<Pair<Long, String>>()
    private val creatingPlaylist = AtomicBoolean(false)

    init {
        observePlayerManagerState()
    }

    private fun <T> trackCollect(flow: Flow<T>, update: (PlayerUiState, T) -> PlayerUiState) {
        collectorJobs += viewModelScope.launch(coroutineExceptionHandler) { flow.collect { v -> _uiState.update { update(it, v) } } }
    }

    private fun observePlayerManagerState() {
        trackCollect(playerManager.isPlaying) { s, v -> s.copy(isPlaying = v) }
        trackCollect(playerManager.isBuffering) { s, v -> s.copy(isBuffering = v) }
        trackCollect(playerManager.currentPositionMs) { s, v -> s.copy(currentPositionMs = v) }
        trackCollect(playerManager.durationMs) { s, v -> if (v > 0L) s.copy(durationMs = v) else s }
        trackCollect(playerManager.currentQuality) { s, v -> s.copy(currentQuality = v) }
        trackCollect(playerManager.isAudioOnly) { s, v -> s.copy(isAudioOnly = v) }
        trackCollect(playerManager.playbackSpeed) { s, v -> s.copy(playbackSpeed = v) }
        trackCollect(playerManager.errorMessage) { s, v -> s.copy(errorMessage = v) }
        collectorJobs += viewModelScope.launch(coroutineExceptionHandler) {
            playerManager.ccEnabled.collect { enabled ->
                _captionUiState.update { it.copy(ccEnabled = enabled) }
            }
        }
        collectorJobs += viewModelScope.launch(coroutineExceptionHandler) {
            playerManager.captionLang.collect { lang ->
                _captionUiState.update { it.copy(selectedLang = lang) }
            }
        }
    }

    fun loadVideo(videoId: String, force: Boolean = false) {
        val managerDetails = playerManager.currentVideoDetails.value
        if (!force && managerDetails?.videoId == videoId) {
            val outgoingId = currentVideoId
            val outgoingPos = _uiState.value.currentPositionMs
            if (outgoingId != null && outgoingId != videoId && outgoingPos > 0L) {
                syncHelper.persistAsync(viewModelScope, outgoingId, outgoingPos)
            }
            currentVideoId = videoId
            loadVideoJob?.cancel()
            subObserveJob?.cancel()
            watchLaterObserveJob?.cancel()
            channelVideosJob?.cancel()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    videoDetails = managerDetails,
                    durationMs = managerDetails.durationMs,
                    errorMessage = null
                )
            }
            if (managerDetails.channelId.isNotBlank()) {
                observeChannelAndBookmarkState(managerDetails.channelId, videoId)
                loadChannelVideos(managerDetails.channelId)
            }
            if (!syncHelper.isSyncing) startHistorySync()
            return
        }

        val existingDetails = _uiState.value.videoDetails
        if (!force && currentVideoId == videoId && existingDetails != null) {
            if (playerManager.currentVideoDetails.value?.videoId == videoId) {
                if (!syncHelper.isSyncing) startHistorySync()
                return
            } else {
                val savedPosition = _uiState.value.currentPositionMs
                playerManager.loadVideo(existingDetails, startPositionMs = savedPosition)
                startHistorySync()
                return
            }
        }
        // Persist outgoing video position before switching (in-app related-list
        // navigation reuses the ViewModel without disposing the screen).
        val outgoingId = currentVideoId
        val outgoingPos = _uiState.value.currentPositionMs
        syncHelper.stop()
        if (outgoingId != null && outgoingId != videoId && outgoingPos > 0L) {
            syncHelper.persistAsync(viewModelScope, outgoingId, outgoingPos)
        }
        currentVideoId = videoId
        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                toggleErrorMessage = null,
                durationMs = 0L,
                videoDetails = null,
                relatedVideos = emptyList(),
                isSubscribed = false,
                isSavedToWatchLater = false
            )
        }
        loadVideoJob?.cancel()
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        channelVideosJob?.cancel()
        if (_uiState.value.activeSleepTimer == SleepTimerOption.END_OF_VIDEO) {
            sleepTimerJob?.cancel()
            _uiState.update { it.copy(activeSleepTimer = SleepTimerOption.OFF) }
        }
        loadVideoJob = viewModelScope.launch(coroutineExceptionHandler) {
            val requestId = videoId
            val savedPosition = try {
                historyRepository.getPlaybackPosition(videoId) ?: 0L
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.w("PlayerViewModel", "getPlaybackPosition failed for $videoId", e)
                0L
            }
            videoDetailsRepository.extractVideoDetails(videoId).onSuccess { details ->
                if (requestId != currentVideoId) return@launch
                _uiState.update { it.copy(isLoading = false, videoDetails = details, durationMs = details.durationMs) }
                if (requestId != currentVideoId) return@launch
                if (details.videoStreams.isNotEmpty() || details.audioStreams.isNotEmpty()) {
                    try {
                        historyRepository.recordPlayback(
                            HistoryEntity(
                                videoId = details.videoId,
                                title = details.title,
                                channelId = details.channelId,
                                channelName = details.channelName,
                                thumbnailUrl = details.thumbnailUrl ?: details.channelAvatarUrl,
                                durationMs = details.durationMs,
                                lastPlaybackPositionMs = savedPosition,
                                lastPlayedAtMs = System.currentTimeMillis()
                            )
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Best-effort: never abort playback, but keep observable.
                        android.util.Log.w("PlayerViewModel", "recordPlayback failed for ${details.videoId}", e)
                    }
                }
                if (requestId != currentVideoId) return@launch
                if (details.channelId.isNotBlank()) {
                    observeChannelAndBookmarkState(details.channelId, details.videoId)
                    if (requestId != currentVideoId) return@launch
                    loadChannelVideos(details.channelId)
                    if (requestId != currentVideoId) return@launch
                }
                playbackPrefs?.let { prefs ->
                    playerManager.applyStoredPrefs(
                        speed = prefs.speed.value,
                        quality = prefs.quality.value,
                        captionLang = prefs.captionLang.value,
                        ccEnabled = prefs.ccEnabled.value
                    )
                    _captionUiState.update {
                        it.copy(
                            ccEnabled = prefs.ccEnabled.value,
                            selectedLang = prefs.captionLang.value,
                            tracks = details.subtitles
                        )
                    }
                } ?: run {
                    _captionUiState.update { it.copy(tracks = details.subtitles) }
                }
                playerManager.loadVideo(details, startPositionMs = savedPosition)
                if (requestId != currentVideoId) return@launch
                startHistorySync()
            }.onFailure {
                if (requestId != currentVideoId) return@launch
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videoDetails = null,
                        relatedVideos = emptyList(),
                        isSubscribed = false,
                        isSavedToWatchLater = false,
                        errorMessage = "تعذر استخراج بيانات الفيديو"
                    )
                }
            }
        }
    }

    private fun observeChannelAndBookmarkState(channelId: String, videoId: String) {
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        val requestVideoId = currentVideoId
        subObserveJob = viewModelScope.launch(coroutineExceptionHandler) {
            subscriptionRepository.isSubscribed(channelId).collect { sub ->
                if (requestVideoId != currentVideoId) return@collect
                _uiState.update { it.copy(isSubscribed = sub) }
            }
        }
        watchLaterObserveJob = viewModelScope.launch(coroutineExceptionHandler) {
            watchLaterRepository.isInWatchLater(videoId).collect { saved ->
                if (requestVideoId != currentVideoId) return@collect
                _uiState.update { it.copy(isSavedToWatchLater = saved) }
            }
        }
    }

    private fun loadChannelVideos(channelId: String) {
        channelVideosJob?.cancel()
        val requestedVideoId = currentVideoId
        channelVideosJob = viewModelScope.launch(coroutineExceptionHandler) {
            videoDetailsRepository.getChannelVideos(channelId, limit = 15)
                .onSuccess { list ->
                    if (requestedVideoId != currentVideoId) return@onSuccess
                    _uiState.update { it.copy(relatedVideos = list.filter { item -> item.videoId != currentVideoId }) }
                }
                .onFailure {
                    android.util.Log.w("PlayerViewModel", "getChannelVideos failed for $channelId", it)
                    // Non-fatal: keep empty related list, playback continues.
                    if (requestedVideoId != currentVideoId) return@onFailure
                }
        }
    }

    private fun startHistorySync() {
        syncHelper.start(viewModelScope, { currentVideoId }, { _uiState.value.currentPositionMs }, { _uiState.value.durationMs }, { playerManager.isPlaying.value })
    }

    fun play() = playerManager.play()
    fun pause() {
        playerManager.pause()
        persistCurrentPlaybackPosition()
    }
    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun seekTo(positionMs: Long) = playerManager.seekTo(positionMs)
    fun seekRelative(offsetMs: Long) = playerManager.seekRelative(offsetMs)
    fun setPlaybackSpeed(speed: Float) {
        playbackPrefs?.setSpeed(speed)
        playerManager.setPlaybackSpeed(playbackPrefs?.speed?.value ?: speed)
    }
    fun setQuality(quality: PlaybackQuality) {
        playbackPrefs?.setQuality(quality)
        playerManager.setQuality(quality)
    }
    fun setAudioOnly(enabled: Boolean) = playerManager.setAudioOnly(enabled)

    fun setCcEnabled(enabled: Boolean) {
        playbackPrefs?.setCcEnabled(enabled)
        playerManager.setCaption(
            languageCode = playbackPrefs?.captionLang?.value ?: _captionUiState.value.selectedLang,
            ccEnabled = enabled
        )
        _captionUiState.update { it.copy(ccEnabled = enabled) }
    }

    fun setCaptionLanguage(languageCode: String?) {
        val shouldEnable = !languageCode.isNullOrBlank()
        playbackPrefs?.setCaptionLang(languageCode)
        playbackPrefs?.setCcEnabled(shouldEnable)
        playerManager.setCaption(
            languageCode = languageCode,
            ccEnabled = shouldEnable
        )
        _captionUiState.update {
            it.copy(
                selectedLang = languageCode,
                ccEnabled = shouldEnable
            )
        }
    }

    suspend fun playlistMembership(videoId: String): Set<Long> {
        val repo = playlistRepository ?: return emptySet()
        if (videoId.isBlank()) return emptySet()
        return repo.getPlaylistIdsForVideo(videoId)
    }

    fun toggleInPlaylist(
        playlistId: Long,
        videoId: String,
        title: String,
        channelId: String,
        channelName: String,
        thumbnailUrl: String? = null,
        durationMs: Long = 0L,
        currentlyIn: Boolean,
        onDone: (Boolean) -> Unit = {}
    ) {
        val repo = playlistRepository ?: return
        val toggleKey = playlistId to videoId
        if (!togglingPlaylist.add(toggleKey)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                if (currentlyIn) {
                    repo.removeFromPlaylist(playlistId, videoId).onSuccess { onDone(false) }
                        .onFailure { _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") } }
                } else {
                    repo.addToPlaylist(
                        playlistId = playlistId,
                        videoId = videoId,
                        title = title,
                        channelId = channelId,
                        channelName = channelName,
                        thumbnailUrl = thumbnailUrl,
                        durationMs = durationMs
                    ).onSuccess { onDone(true) }
                        .onFailure { _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") } }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
            } finally {
                togglingPlaylist.remove(toggleKey)
            }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        if (!creatingPlaylist.compareAndSet(false, true)) return
        val repo = playlistRepository ?: run { creatingPlaylist.set(false); return }
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                repo.createPlaylist(name).onSuccess { onCreated(it) }
                    .onFailure { _uiState.update { it.copy(toggleErrorMessage = "تعذر إنشاء القائمة") } }
            } finally {
                creatingPlaylist.set(false)
            }
        }
    }

    fun importPlaylistFromUrl(url: String, onImported: (Long) -> Unit = {}) {
        if (_playlistImportState.value.isLoading) return
        val repo = playlistRepository ?: return
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _playlistImportState.update { it.copy(errorMessage = "رابط القائمة فارغ") }
            return
        }
        _playlistImportState.update { it.copy(isLoading = true, errorMessage = null, successPlaylistId = null) }
        viewModelScope.launch(coroutineExceptionHandler) {
            repo.importFromYoutubeUrl(trimmed).onSuccess { id ->
                _playlistImportState.update {
                    it.copy(isLoading = false, errorMessage = null, successPlaylistId = id)
                }
                onImported(id)
            }.onFailure {
                _playlistImportState.update {
                    it.copy(isLoading = false, errorMessage = "تعذر استيراد القائمة، تحقق من صحة الرابط والاتصال")
                }
            }
        }
    }

    fun clearPlaylistImportError() {
        _playlistImportState.update { it.copy(errorMessage = null) }
    }

    fun toggleZoomToFill() {
        _uiState.update { it.copy(isZoomToFill = !it.isZoomToFill) }
    }

    fun setSleepTimer(option: SleepTimerOption) {
        sleepTimerJob?.cancel()
        _uiState.update { it.copy(activeSleepTimer = option) }
        if (option == SleepTimerOption.OFF) return

        sleepTimerJob = viewModelScope.launch(coroutineExceptionHandler) {
            val durationMins = option.durationMinutes
            if (durationMins != null && durationMins > 0) {
                delay(durationMins * 60 * 1000L)
                pause()
                _uiState.update { it.copy(activeSleepTimer = SleepTimerOption.OFF) }
            } else if (option == SleepTimerOption.END_OF_VIDEO) {
                while (isActive) {
                    val pos = _uiState.value.currentPositionMs
                    val dur = _uiState.value.durationMs
                    if (dur > 0L && pos >= dur - 1500L) {
                        pause()
                        _uiState.update { it.copy(activeSleepTimer = SleepTimerOption.OFF) }
                        break
                    }
                    delay(1000L)
                }
            }
        }
    }

    fun toggleSubscription() {
        val details = _uiState.value.videoDetails ?: return
        if (!togglingSubs.add(details.channelId)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                subscriptionRepository.toggleSubscription(
                    SubscriptionEntity(
                        channelId = details.channelId,
                        channelName = details.channelName,
                        avatarUrl = details.channelAvatarUrl
                    )
                ).onFailure {
                    _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
                }.onSuccess {
                    _uiState.update { it.copy(toggleErrorMessage = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
            } finally {
                togglingSubs.remove(details.channelId)
            }
        }
    }

    fun toggleWatchLater() {
        val details = _uiState.value.videoDetails ?: return
        if (!togglingWatch.add(details.videoId)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                watchLaterRepository.toggleWatchLater(
                    WatchLaterEntity(
                        videoId = details.videoId,
                        title = details.title,
                        channelId = details.channelId,
                        channelName = details.channelName,
                        thumbnailUrl = details.thumbnailUrl ?: details.channelAvatarUrl,
                        durationMs = details.durationMs
                    )
                ).onFailure {
                    _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
                }.onSuccess {
                    _uiState.update { it.copy(toggleErrorMessage = null) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(toggleErrorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
            } finally {
                togglingWatch.remove(details.videoId)
            }
        }
    }

    fun clearToggleError() {
        _uiState.update { it.copy(toggleErrorMessage = null) }
    }

    fun persistCurrentPlaybackPosition() {
        syncHelper.persistAsync(viewModelScope, currentVideoId, _uiState.value.currentPositionMs)
    }

    fun stopPlaybackTracking() {
        syncHelper.stop()
        persistCurrentPlaybackPosition()
    }

    override fun onCleared() {
        super.onCleared()
        syncHelper.stop()
        loadVideoJob?.cancel()
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        channelVideosJob?.cancel()
        sleepTimerJob?.cancel()
        collectorJobs.forEach { it.cancel() }
        collectorJobs.clear()
    }
}
