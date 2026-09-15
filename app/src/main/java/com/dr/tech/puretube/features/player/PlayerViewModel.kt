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
    val errorMessage: String? = null
)

/**
 * ViewModel managing player state, video details extraction, and database syncing.
 * History loop delegated to [PlayerPlaybackSyncHelper]. Constitution V/VI/IX:
 * viewModelScope only, debounce on toggles, zero double-bang, Result<T>, Long ms.
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
    private val syncHelper = PlayerPlaybackSyncHelper(historyRepository)
    private var loadVideoJob: Job? = null
    private var subObserveJob: Job? = null
    private var watchLaterObserveJob: Job? = null
    private var channelVideosJob: Job? = null
    private val collectorJobs = mutableListOf<Job>()
    private var isTogglingSubscription = false
    private var isTogglingWatchLater = false

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
        currentVideoId = videoId
        _uiState.update { it.copy(isLoading = true, errorMessage = null, durationMs = 0L, videoDetails = null, relatedVideos = emptyList()) }
        loadVideoJob?.cancel()
        syncHelper.stop()
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        channelVideosJob?.cancel()
        loadVideoJob = viewModelScope.launch {
            val requestId = videoId
            val savedPosition = historyRepository.getPlaybackPosition(videoId) ?: 0L
            videoDetailsRepository.extractVideoDetails(videoId).onSuccess { details ->
                if (requestId != currentVideoId) return@launch
                _uiState.update { it.copy(isLoading = false, videoDetails = details, durationMs = details.durationMs) }
                if (requestId != currentVideoId) return@launch
                if (details.videoStreams.isNotEmpty() || details.audioStreams.isNotEmpty()) {
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
                }
                if (requestId != currentVideoId) return@launch
                observeChannelAndBookmarkState(details.channelId, details.videoId)
                if (requestId != currentVideoId) return@launch
                loadChannelVideos(details.channelId)
                if (requestId != currentVideoId) return@launch
                playerManager.loadVideo(details, startPositionMs = savedPosition)
                if (requestId != currentVideoId) return@launch
                startHistorySync()
            }.onFailure { error ->
                if (requestId != currentVideoId) return@launch
                _uiState.update { it.copy(isLoading = false, videoDetails = null, relatedVideos = emptyList(), errorMessage = error.localizedMessage ?: "تعذر استخراج بيانات الفيديو") }
            }
        }
    }

    private fun observeChannelAndBookmarkState(channelId: String, videoId: String) {
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        subObserveJob = viewModelScope.launch {
            subscriptionRepository.isSubscribed(channelId).collect { sub -> _uiState.update { it.copy(isSubscribed = sub) } }
        }
        watchLaterObserveJob = viewModelScope.launch {
            watchLaterRepository.isInWatchLater(videoId).collect { saved -> _uiState.update { it.copy(isSavedToWatchLater = saved) } }
        }
    }

    private fun loadChannelVideos(channelId: String) {
        channelVideosJob?.cancel()
        val requestedVideoId = currentVideoId
        channelVideosJob = viewModelScope.launch {
            videoDetailsRepository.getChannelVideos(channelId, limit = 15).onSuccess { list ->
                if (requestedVideoId != currentVideoId) return@onSuccess
                _uiState.update { it.copy(relatedVideos = list.filter { item -> item.videoId != currentVideoId }) }
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
        if (isTogglingSubscription) return
        val details = _uiState.value.videoDetails ?: return
        isTogglingSubscription = true
        viewModelScope.launch {
            try {
                subscriptionRepository.toggleSubscription(
                    SubscriptionEntity(
                        channelId = details.channelId,
                        channelName = details.channelName,
                        avatarUrl = details.channelAvatarUrl
                    )
                ).onFailure {
                    _uiState.update { it.copy(errorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
            } finally {
                isTogglingSubscription = false
            }
        }
    }

    fun toggleWatchLater() {
        if (isTogglingWatchLater) return
        val details = _uiState.value.videoDetails ?: return
        isTogglingWatchLater = true
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
                    _uiState.update { it.copy(errorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(errorMessage = "تعذر حفظ التغيير، حاول مجدداً") }
            } finally {
                isTogglingWatchLater = false
            }
        }
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
