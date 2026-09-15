package com.dr.tech.puretube.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import com.dr.tech.puretube.core.aibridge.AudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpAudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpVideoFrameHook
import com.dr.tech.puretube.core.aibridge.VideoFrameHook
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.repository.VideoDetailsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-level Media3 ExoPlayer manager.
 * Conforms to Constitution Principles II, IV, and VIII:
 * - AI Safety & Zero-Copy Hook readiness (VideoFrameHook, AudioFilterHook)
 * - Concurrency & DASH stream merging
 * - Ephemeral in-memory stream state with automatic HTTP 403 recovery
 * - Long millisecond precision for all position and duration values
 */
@OptIn(UnstableApi::class)
class PurePlayerManager(
    private val context: Context,
    private val mediaSourceFactory: PureMediaSourceFactory,
    private val videoDetailsRepository: VideoDetailsRepository,
    private val videoFrameHook: VideoFrameHook = NoOpVideoFrameHook(),
    private val audioFilterHook: AudioFilterHook = NoOpAudioFilterHook(),
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job())
) {

    val player: ExoPlayer = buildExoPlayer()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentQuality = MutableStateFlow(PlaybackQuality.AUTO)
    val currentQuality: StateFlow<PlaybackQuality> = _currentQuality.asStateFlow()

    private val _isAudioOnly = MutableStateFlow(false)
    val isAudioOnly: StateFlow<Boolean> = _isAudioOnly.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentVideoDetails: ExtractedVideoDetails? = null
    private var positionTrackerJob: Job? = null
    private var isRecoveringFrom403 = false
    private var forbiddenRetryCount = 0
    private val maxForbiddenRetries = 1

    init {
        setupPlayerListener()
        startPositionTracker()
    }

    private fun buildExoPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        return ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
    }

    private fun setupPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = (state == Player.STATE_BUFFERING)
                if (state == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)
                    _errorMessage.value = null
                    forbiddenRetryCount = 0
                }
            }

            override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                _isPlaying.value = isPlayingNow
                if (isPlayingNow) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                handlePlaybackError(error)
            }
        })
    }

    private fun startPositionTracker() {
        positionTrackerJob?.cancel()
        positionTrackerJob = coroutineScope.launch {
            while (isActive) {
                if (player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration
                    if (dur > 0L) {
                        _durationMs.value = dur
                    }
                }
                delay(500)
            }
        }
    }

    /**
     * Loads and starts playback for the specified video details at the given starting position.
     */
    fun loadVideo(details: ExtractedVideoDetails, startPositionMs: Long = 0L) {
        if (currentVideoDetails?.videoId != details.videoId) {
            forbiddenRetryCount = 0
        }
        currentVideoDetails = details
        _errorMessage.value = null
        val mediaSource = mediaSourceFactory.createMediaSource(
            details = details,
            quality = _currentQuality.value,
            isAudioOnly = _isAudioOnly.value
        )

        if (mediaSource != null) {
            player.setMediaSource(mediaSource)
            player.prepare()
            if (startPositionMs > 0L) {
                player.seekTo(startPositionMs)
            }
            player.playWhenReady = true
            startPlaybackService()
        } else {
            _errorMessage.value = "تعذر تحضير روابط البث المباشر"
        }
    }

    fun play() {
        player.play()
        startPlaybackService()
    }

    fun pause() {
        player.pause()
    }

    private fun startPlaybackService() {
        try {
            val intent = android.content.Intent(context, PurePlaybackService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            // Guard against background start restrictions
        }
    }

    fun seekTo(positionMs: Long) {
        val target = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    fun seekRelative(offsetMs: Long) {
        val base = _currentPositionMs.value
        val target = (base + offsetMs).coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        player.seekTo(target)
        _currentPositionMs.value = target
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        player.playbackParameters = PlaybackParameters(speed)
    }

    fun setQuality(quality: PlaybackQuality) {
        if (_currentQuality.value == quality) return
        _currentQuality.value = quality

        val details = currentVideoDetails ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val mediaSource = mediaSourceFactory.createMediaSource(
            details = details,
            quality = quality,
            isAudioOnly = _isAudioOnly.value
        ) ?: return

        player.setMediaSource(mediaSource)
        player.prepare()
        player.seekTo(currentPos)
        player.playWhenReady = wasPlaying
    }

    fun setAudioOnly(enabled: Boolean) {
        if (_isAudioOnly.value == enabled) return
        _isAudioOnly.value = enabled

        val details = currentVideoDetails ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val mediaSource = mediaSourceFactory.createMediaSource(
            details = details,
            quality = _currentQuality.value,
            isAudioOnly = enabled
        ) ?: return

        player.setMediaSource(mediaSource)
        player.prepare()
        player.seekTo(currentPos)
        player.playWhenReady = wasPlaying
    }

    /**
     * Automatic in-memory recovery from expiring YouTube DASH URLs (HTTP 403 / 410).
     * Follows Constitution Principle VIII.
     */
    private fun handlePlaybackError(error: PlaybackException) {
        val cause = error.cause
        val isHttpForbidden = (cause is HttpDataSource.InvalidResponseCodeException &&
                (cause.responseCode == 403 || cause.responseCode == 410))

        val details = currentVideoDetails
        if (isHttpForbidden && details != null && !isRecoveringFrom403) {
            if (forbiddenRetryCount >= maxForbiddenRetries) {
                forbiddenRetryCount = 0
                _errorMessage.value = "انتهت صلاحية الرابط وتعذر تجديده تلقائياً"
                return
            }
            forbiddenRetryCount++
            isRecoveringFrom403 = true
            val savedPositionMs = player.currentPosition.coerceAtLeast(0L)

            coroutineScope.launch {
                val refreshResult = videoDetailsRepository.extractVideoDetails(details.videoId)
                refreshResult.onSuccess { freshDetails ->
                    currentVideoDetails = freshDetails
                    isRecoveringFrom403 = false
                    loadVideo(freshDetails, savedPositionMs)
                }.onFailure {
                    isRecoveringFrom403 = false
                    forbiddenRetryCount = 0
                    _errorMessage.value = "انتهت صلاحية الرابط وتعذر تجديده تلقائياً"
                }
            }
        } else {
            _errorMessage.value = "حدث خطأ أثناء تشغيل الفيديو (${error.errorCodeName})"
        }
    }

    fun release() {
        positionTrackerJob?.cancel()
        player.release()
    }
}
