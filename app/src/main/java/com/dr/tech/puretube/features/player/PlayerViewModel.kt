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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
 * Conforms to Constitution Principles I, IV, V, VI, VIII, and IX.
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
    private var historySyncJob: Job? = null
    private var loadVideoJob: Job? = null
    private var subObserveJob: Job? = null
    private var watchLaterObserveJob: Job? = null
    private var channelVideosJob: Job? = null
    private var isTogglingSubscription = false
    private var isTogglingWatchLater = false

    init {
        observePlayerManagerState()
    }

    private fun observePlayerManagerState() {
        viewModelScope.launch {
            playerManager.isPlaying.collect { playing ->
                _uiState.update { it.copy(isPlaying = playing) }
            }
        }
        viewModelScope.launch {
            playerManager.isBuffering.collect { buffering ->
                _uiState.update { it.copy(isBuffering = buffering) }
            }
        }
        viewModelScope.launch {
            playerManager.currentPositionMs.collect { pos ->
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
        }
        viewModelScope.launch {
            playerManager.durationMs.collect { dur ->
                if (dur > 0L) {
                    _uiState.update { it.copy(durationMs = dur) }
                }
            }
        }
        viewModelScope.launch {
            playerManager.currentQuality.collect { quality ->
                _uiState.update { it.copy(currentQuality = quality) }
            }
        }
        viewModelScope.launch {
            playerManager.isAudioOnly.collect { audioOnly ->
                _uiState.update { it.copy(isAudioOnly = audioOnly) }
            }
        }
        viewModelScope.launch {
            playerManager.playbackSpeed.collect { speed ->
                _uiState.update { it.copy(playbackSpeed = speed) }
            }
        }
        viewModelScope.launch {
            playerManager.errorMessage.collect { error ->
                _uiState.update { it.copy(errorMessage = error) }
            }
        }
    }

    fun loadVideo(videoId: String, force: Boolean = false) {
        if (!force && currentVideoId == videoId && _uiState.value.videoDetails != null) {
            if (historySyncJob?.isActive != true) startHistorySync()
            return
        }
        currentVideoId = videoId
        _uiState.update { it.copy(isLoading = true, errorMessage = null, durationMs = 0L) }

        loadVideoJob?.cancel()
        historySyncJob?.cancel()
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        channelVideosJob?.cancel()
        loadVideoJob = viewModelScope.launch {
            val requestId = videoId
            val savedPosition = historyRepository.getPlaybackPosition(videoId) ?: 0L

            val result = videoDetailsRepository.extractVideoDetails(videoId)
            result.onSuccess { details ->
                if (requestId != currentVideoId) return@launch
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        videoDetails = details,
                        durationMs = details.durationMs
                    )
                }

                // Record start in History (skip unplayable videos with no streams)
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

                // Sync initial subscription & watch later state
                if (requestId != currentVideoId) return@launch
                observeChannelAndBookmarkState(details.channelId, details.videoId)

                // Fetch other videos from the same channel (strictly no algorithms)
                if (requestId != currentVideoId) return@launch
                loadChannelVideos(details.channelId)

                // Prepare and load stream into ExoPlayer
                if (requestId != currentVideoId) return@launch
                playerManager.loadVideo(details, startPositionMs = savedPosition)

                // Start periodic history persistence
                if (requestId != currentVideoId) return@launch
                startHistorySync()
            }.onFailure { error ->
                if (requestId != currentVideoId) return@launch
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "تعذر استخراج بيانات الفيديو"
                    )
                }
            }
        }
    }

    private fun observeChannelAndBookmarkState(channelId: String, videoId: String) {
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        subObserveJob = viewModelScope.launch {
            subscriptionRepository.isSubscribed(channelId).collect { sub ->
                _uiState.update { it.copy(isSubscribed = sub) }
            }
        }
        watchLaterObserveJob = viewModelScope.launch {
            watchLaterRepository.isInWatchLater(videoId).collect { saved ->
                _uiState.update { it.copy(isSavedToWatchLater = saved) }
            }
        }
    }

    private fun loadChannelVideos(channelId: String) {
        channelVideosJob?.cancel()
        val requestedVideoId = currentVideoId
        channelVideosJob = viewModelScope.launch {
            val res = videoDetailsRepository.getChannelVideos(channelId, limit = 15)
            res.onSuccess { list ->
                if (requestedVideoId != currentVideoId) return@onSuccess
                val filtered = list.filter { it.videoId != currentVideoId }
                _uiState.update { it.copy(relatedVideos = filtered) }
            }
        }
    }

    private fun startHistorySync() {
        historySyncJob?.cancel()
        historySyncJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                val vid = currentVideoId ?: continue
                val isCurrentlyPlaying = playerManager.isPlaying.value
                val pos = _uiState.value.currentPositionMs
                val dur = _uiState.value.durationMs

                if (isCurrentlyPlaying && pos > 0L) {
                    historyRepository.updatePlaybackPosition(vid, pos)
                    if (dur > 0L && pos >= (dur * 0.95)) {
                        historyRepository.markCompleted(vid)
                    }
                }
            }
        }
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
                val isSub = _uiState.value.isSubscribed
                if (isSub) {
                    subscriptionRepository.unsubscribe(details.channelId)
                } else {
                    subscriptionRepository.subscribe(
                        SubscriptionEntity(
                            channelId = details.channelId,
                            channelName = details.channelName,
                            avatarUrl = details.channelAvatarUrl
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("PlayerViewModel", "Failed to toggle subscription", e)
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
                val isSaved = _uiState.value.isSavedToWatchLater
                if (isSaved) {
                    watchLaterRepository.removeFromWatchLater(details.videoId)
                } else {
                    watchLaterRepository.addToWatchLater(
                        WatchLaterEntity(
                            videoId = details.videoId,
                            title = details.title,
                            channelId = details.channelId,
                            channelName = details.channelName,
                            thumbnailUrl = details.thumbnailUrl ?: details.channelAvatarUrl,
                            durationMs = details.durationMs
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w("PlayerViewModel", "Failed to toggle watch later", e)
            } finally {
                isTogglingWatchLater = false
            }
        }
    }

    fun persistCurrentPlaybackPosition() {
        val vid = currentVideoId ?: return
        val pos = _uiState.value.currentPositionMs
        if (pos > 0L) {
            viewModelScope.launch {
                historyRepository.updatePlaybackPosition(vid, pos)
            }
        }
    }

    fun stopPlaybackTracking() {
        historySyncJob?.cancel()
        persistCurrentPlaybackPosition()
    }

    override fun onCleared() {
        super.onCleared()
        historySyncJob?.cancel()
        loadVideoJob?.cancel()
        subObserveJob?.cancel()
        watchLaterObserveJob?.cancel()
        channelVideosJob?.cancel()
    }
}
