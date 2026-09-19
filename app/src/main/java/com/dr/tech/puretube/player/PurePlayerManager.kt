package com.dr.tech.puretube.player

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecDecoderException
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import com.dr.tech.puretube.core.aibridge.AudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpAudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpVideoFrameHook
import com.dr.tech.puretube.core.aibridge.VideoFrameHook
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.repository.VideoDetailsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
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
    private val coroutineScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("PurePlayerManager", "Uncaught player coroutine error intercepted", throwable)
        }
    )
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

    private val _currentVideoDetails = MutableStateFlow<ExtractedVideoDetails?>(null)
    val currentVideoDetails: StateFlow<ExtractedVideoDetails?> = _currentVideoDetails.asStateFlow()

    private var positionTrackerJob: Job? = null
    private var isRecoveringFrom403 = false
    private var forbiddenRetryCount = 0
    private val maxForbiddenRetries = 1

    private var isRecoveringFromDecodingError = false
    private var decodingRetryCount = 0
    private val maxDecodingRetries = 1

    init {
        setupPlayerListener()
        startPositionTracker()
    }

    private fun buildExoPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setMediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
                val decoders = MediaCodecUtil.getDecoderInfos(
                    mimeType,
                    requiresSecureDecoder,
                    requiresTunnelingDecoder
                )
                // Filter out broken c2.android.opus.decoder on Android 10 (API 29) where ION ioctl fails
                if (Build.VERSION.SDK_INT == 29 &&
                    mimeType.equals(MimeTypes.AUDIO_OPUS, ignoreCase = true)
                ) {
                    decoders.filter { !it.name.equals("c2.android.opus.decoder", ignoreCase = true) }
                } else {
                    decoders
                }
            }

        return ExoPlayer.Builder(context, renderersFactory)
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
                    decodingRetryCount = 0
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
                try {
                    if (player.isPlaying) {
                        _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration
                        if (dur > 0L) {
                            _durationMs.value = dur
                        }
                    }
                } catch (_: Exception) {
                    // ExoPlayer released mid-loop: ignore, next tick retries.
                }
                delay(500)
            }
        }
    }

    /**
     * Loads and starts playback for the specified video details at the given starting position.
     */
    fun loadVideo(details: ExtractedVideoDetails, startPositionMs: Long = 0L) {
        if (_currentVideoDetails.value?.videoId != details.videoId) {
            forbiddenRetryCount = 0
            decodingRetryCount = 0
            isRecoveringFrom403 = false
            isRecoveringFromDecodingError = false
        }
        _currentVideoDetails.value = details
        _errorMessage.value = null
        try {
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
        } catch (e: Throwable) {
            android.util.Log.e("PurePlayerManager", "Failed to load video media source", e)
            _errorMessage.value = "تعذر إعداد مسار تشغيل الفيديو"
        }
    }

    fun play() {
        player.play()
        startPlaybackService()
    }

    fun pause() {
        player.pause()
    }

    fun stop() {
        player.stop()
        player.clearMediaItems()
        _currentVideoDetails.value = null
        _isPlaying.value = false
        _isBuffering.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        try {
            val intent = android.content.Intent(context, PurePlaybackService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            android.util.Log.w("PurePlayerManager", "Could not stop playback service", e)
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = android.content.Intent(context, PurePlaybackService::class.java)
            context.startService(intent)
        } catch (e: Exception) {
            // Guard against background start restrictions with diagnostic logging
            android.util.Log.w("PurePlayerManager", "Could not start playback service", e)
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

        val details = _currentVideoDetails.value ?: return
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

        val details = _currentVideoDetails.value ?: return
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
     * Automatic in-memory recovery from expiring YouTube DASH URLs (HTTP 403 / 410)
     * and codec decoding failures (ERROR_CODE_DECODING_FAILED).
     * Follows Constitution Principles IV, VII, and VIII.
     */
    private fun handlePlaybackError(error: PlaybackException) {
        val cause = error.cause
        val isHttpForbidden = (cause is HttpDataSource.InvalidResponseCodeException &&
                (cause.responseCode == 403 || cause.responseCode == 410))

        val isDecodingError = error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ||
                cause is MediaCodecDecoderException ||
                cause is MediaCodecRenderer.DecoderInitializationException ||
                (cause is MediaCodec.CodecException)

        val details = _currentVideoDetails.value
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
                try {
                    val refreshResult = videoDetailsRepository.extractVideoDetails(details.videoId)
                    refreshResult.onSuccess { freshDetails ->
                        if (_currentVideoDetails.value != null) {
                            _currentVideoDetails.value = freshDetails
                            loadVideo(freshDetails, savedPositionMs)
                        }
                    }.onFailure {
                        forbiddenRetryCount = 0
                        _errorMessage.value = "انتهت صلاحية الرابط وتعذر تجديده تلقائياً"
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    android.util.Log.w("PurePlayerManager", "HTTP 403 recovery attempt failed", e)
                    forbiddenRetryCount = 0
                    _errorMessage.value = "انتهت صلاحية الرابط وتعذر تجديده تلقائياً"
                } finally {
                    isRecoveringFrom403 = false
                }
            }
        } else if (isDecodingError && details != null && !isRecoveringFromDecodingError) {
            if (decodingRetryCount >= maxDecodingRetries) {
                decodingRetryCount = 0
                _errorMessage.value = "تعذر فك تشفير هذا المقطع على مشفرات الجهاز"
                return
            }
            decodingRetryCount++
            isRecoveringFromDecodingError = true
            val savedPositionMs = player.currentPosition.coerceAtLeast(0L)
            android.util.Log.w(
                "PurePlayerManager",
                "Decoding failure encountered (${error.errorCodeName}). Attempting self-healing fallback at ${savedPositionMs}ms",
                error
            )

            try {
                val fallbackMediaSource = mediaSourceFactory.createFallbackMediaSource(
                    details = details,
                    quality = _currentQuality.value,
                    isAudioOnly = _isAudioOnly.value
                )

                if (fallbackMediaSource != null) {
                    player.setMediaSource(fallbackMediaSource)
                    player.prepare()
                    if (savedPositionMs > 0L) {
                        player.seekTo(savedPositionMs)
                    }
                    player.playWhenReady = true
                    android.util.Log.i("PurePlayerManager", "Self-healing fallback media source successfully activated")
                } else {
                    decodingRetryCount = 0
                    _errorMessage.value = "تعذر العثور على مسار بديل متوافق مع مشفر الجهاز"
                }
            } catch (t: Throwable) {
                android.util.Log.e("PurePlayerManager", "Failed to activate fallback media source", t)
                decodingRetryCount = 0
                _errorMessage.value = "تعذر تشغيل الفيديو بمشفرات هذا الجهاز"
            } finally {
                isRecoveringFromDecodingError = false
            }
        } else {
            android.util.Log.e("PurePlayerManager", "Playback error encountered: ${error.errorCodeName}", error)
            _errorMessage.value = "حدث خطأ أثناء تشغيل الفيديو، يرجى المحاولة لاحقاً"
        }
    }

    fun release() {
        positionTrackerJob?.cancel()
        // Never cancel the singleton scope Job itself (it would permanently
        // break later launches); cancel children only.
        coroutineScope.coroutineContext.cancelChildren()
        player.release()
        try {
            val intent = android.content.Intent(context, PurePlaybackService::class.java)
            context.stopService(intent)
        } catch (e: Exception) {
            android.util.Log.w("PurePlayerManager", "Could not stop playback service on release", e)
        }
    }
}
