package com.dr.tech.puretube.features.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.VideoDetailsRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.player.PurePlayerManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

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
    val toggleErrorMessage: String? = null
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
    private val watchLaterRepository: WatchLaterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentVideoId: String? = null
    private val syncHelper = PlayerPlaybackSyncHelper(historyRepository) { e ->
        android.util.Log.w("PlayerViewModel", "history sync failed", e)
    }
    private var loadVideoJob: Job? = null
    private var subObserveJob: Job? = null
    private var watchLaterObserveJob: Job? = null
    private var channelVideosJob: Job? = null
    private val collectorJobs = CopyOnWriteArrayList<Job>()
    private val togglingSubs = ConcurrentHashMap.newKeySet<String>()
    private val togglingWatch = ConcurrentHashMap.newKeySet<String>()

    init {
        observePlayerManagerState()
    }

    private fun <T> trackCollect(flow: Flow<T>, update: (PlayerUiState, T) -> PlayerUiState) {
        collectorJobs += viewModelScope.launch { flow.collect { v -> _uiState.update { update(it, v) } } }
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
    }

    fun loadVideo(videoId: String, force: Boolean = false) {
        if (!force && currentVideoId == videoId && _uiState.value.videoDetails != null) {
            if (!syncHelper.isSyncing) startHistorySync()
            return
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
        loadVideoJob = viewModelScope.launch {
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
        subObserveJob = viewModelScope.launch {
            subscriptionRepository.isSubscribed(channelId).collect { sub ->
                if (requestVideoId != currentVideoId) return@collect
                _uiState.update { it.copy(isSubscribed = sub) }
            }
        }
        watchLaterObserveJob = viewModelScope.launch {
            watchLaterRepository.isInWatchLater(videoId).collect { saved ->
                if (requestVideoId != currentVideoId) return@collect
                _uiState.update { it.copy(isSavedToWatchLater = saved) }
            }
        }
    }

    private fun loadChannelVideos(channelId: String) {
        channelVideosJob?.cancel()
        val requestedVideoId = currentVideoId
        channelVideosJob = viewModelScope.launch {
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
    fun setPlaybackSpeed(speed: Float) = playerManager.setPlaybackSpeed(speed)
    fun setQuality(quality: PlaybackQuality) = playerManager.setQuality(quality)
    fun setAudioOnly(enabled: Boolean) = playerManager.setAudioOnly(enabled)

    fun toggleSubscription() {
        val details = _uiState.value.videoDetails ?: return
        if (!togglingSubs.add(details.channelId)) return
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        collectorJobs.forEach { it.cancel() }
        collectorJobs.clear()
    }
}
