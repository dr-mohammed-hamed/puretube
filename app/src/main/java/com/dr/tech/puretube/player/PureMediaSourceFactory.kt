package com.dr.tech.puretube.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import com.dr.tech.puretube.core.data.model.AudioStreamFormat
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.model.SubtitleTrack
import com.dr.tech.puretube.core.data.model.VideoStreamFormat
import com.dr.tech.puretube.core.extractor.PureDownloader

/**
 * Factory responsible for creating Media3 MediaSources.
 * Fulfills Constitution Principle IV:
 * Merges independent YouTube DASH video and audio streams seamlessly using MergingMediaSource.
 */
@OptIn(UnstableApi::class)
class PureMediaSourceFactory(
    private val context: Context
) {
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent(PureDownloader.USER_AGENT)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setAllowCrossProtocolRedirects(true)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    /**
     * Builds the appropriate MediaSource for the given video details, target quality, and audio-only preference.
     * عند تفعيل CC مع مسار ترجمة مطابق تُدمج الترجمة عبر MergingMediaSource؛ وإلا يُرجع الأساس كما كان.
     */
    fun createMediaSource(
        details: ExtractedVideoDetails,
        quality: PlaybackQuality = PlaybackQuality.AUTO,
        isAudioOnly: Boolean = false,
        excludeOpus: Boolean = false,
        subtitleTracks: List<SubtitleTrack> = emptyList(),
        selectedLang: String? = null,
        ccEnabled: Boolean = false
    ): MediaSource? {
        val baseSource = createBaseMediaSource(details, quality, isAudioOnly, excludeOpus)
            ?: return null
        if (!ccEnabled) return baseSource
        val selectedSubtitle = selectSubtitleTrack(subtitleTracks, selectedLang) ?: return baseSource
        val subtitleSource = createSubtitleSource(selectedSubtitle) ?: return baseSource
        return MergingMediaSource(
            /* adjustPeriodTimeOffsets = */ true,
            /* clipDurations = */ true,
            baseSource,
            subtitleSource
        )
    }

    private fun createBaseMediaSource(
        details: ExtractedVideoDetails,
        quality: PlaybackQuality = PlaybackQuality.AUTO,
        isAudioOnly: Boolean = false,
        excludeOpus: Boolean = false
    ): MediaSource? {
        val selectedAudio = selectBestAudioStream(details.audioStreams, excludeOpus = excludeOpus)

        // Case 1: Audio-Only Mode
        if (isAudioOnly) {
            val audioUrl = selectedAudio?.url
                ?: details.videoStreams.firstOrNull { !it.isVideoOnly }?.url
                ?: return null
            return createSingleSource(audioUrl, details)
        }

        val selectedVideo = selectVideoStream(details.videoStreams, quality)

        // Case 2: Separate DASH Video and Audio Streams (Standard for 1080p / 720p)
        if (selectedVideo != null && selectedVideo.isVideoOnly && selectedAudio != null) {
            val videoSource = createSingleSource(selectedVideo.url, details)
            val audioSource = createSingleSource(selectedAudio.url, details)
            return MergingMediaSource(
                /* adjustPeriodTimeOffsets = */ true,
                /* clipDurations = */ true,
                videoSource,
                audioSource
            )
        }

        // Case 3: Single progressive combined stream.
        // Avoid silent video-only playback when no audio stream is available.
        val fallbackUrl = if (selectedAudio == null) {
            details.videoStreams.firstOrNull { !it.isVideoOnly }?.url ?: return null
        } else {
            selectedVideo?.url
                ?: details.videoStreams.firstOrNull { !it.isVideoOnly }?.url
                ?: selectedAudio.url
                ?: return null
        }

        return createSingleSource(fallbackUrl, details)
    }

    /**
     * Creates a self-healing fallback MediaSource when playback fails due to a codec error
     * (e.g. MediaCodec ERROR_CODE_DECODING_FAILED or ION ioctl crash on c2.android.opus.decoder).
     *
     * Strategies:
     * 1. If in video mode, prioritize a progressive combined stream (<=720p standard H.264/AAC MP4)
     *    to guarantee flawless hardware-accelerated playback without complex DASH video/audio demuxing.
     * 2. If audio-only or no progressive stream exists, attempt DASH with Opus strictly excluded.
     * 3. Fall back to progressive stream if available.
     */
    fun createFallbackMediaSource(
        details: ExtractedVideoDetails,
        quality: PlaybackQuality = PlaybackQuality.AUTO,
        isAudioOnly: Boolean = false
    ): MediaSource? {
        // Strategy 1: In standard video mode, prioritize a single progressive combined stream (H.264/AAC)
        val progressiveStream = details.videoStreams.firstOrNull { !it.isVideoOnly }
        if (!isAudioOnly && progressiveStream != null) {
            return createSingleSource(progressiveStream.url, details)
        }

        // Strategy 2: Attempt DASH or audio-only with Opus strictly excluded
        val safeDashSource = createMediaSource(
            details = details,
            quality = quality,
            isAudioOnly = isAudioOnly,
            excludeOpus = true
        )
        if (safeDashSource != null) {
            return safeDashSource
        }

        // Strategy 3: Progressive combined stream as a last resort if audio-only had no safe audio
        if (progressiveStream != null) {
            return createSingleSource(progressiveStream.url, details)
        }

        return null
    }

    private fun createSingleSource(uriString: String, details: ExtractedVideoDetails): MediaSource {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(details.title)
            .setArtist(details.channelName)
            .setArtworkUri(details.channelAvatarUrl?.toUri())
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uriString)
            .setMediaMetadata(mediaMetadata)
            .build()
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    /**
     * Selects the optimal audio stream.
     * Prioritizes hardware-accelerated M4A/AAC streams (e.g. 128kbps LC-AAC)
     * over software-decoded Opus streams to eliminate codec crashes on devices
     * with broken Codec2 implementations (such as Android 10 / API 29 c2.android.opus.decoder).
     * Falls back to Opus or other available streams if no M4A stream is present.
     */
    fun selectBestAudioStream(
        audioStreams: List<AudioStreamFormat>,
        excludeOpus: Boolean = false
    ): AudioStreamFormat? {
        if (audioStreams.isEmpty()) return null

        val candidateStreams = if (excludeOpus) {
            audioStreams.filter { !it.isOpusOrWebm() }
        } else {
            audioStreams
        }

        // Priority 1: Hardware-accelerated M4A/AAC streams (highest bitrate first)
        val bestAac = candidateStreams
            .filter { it.isAacOrM4a() }
            .maxByOrNull { it.bitrate }
        if (bestAac != null) return bestAac

        // Priority 2: Other formats (e.g. Opus / WebM) if no AAC stream is available
        return candidateStreams.maxByOrNull { it.bitrate }
    }

    private fun selectVideoStream(
        videoStreams: List<VideoStreamFormat>,
        quality: PlaybackQuality
    ): VideoStreamFormat? {
        if (videoStreams.isEmpty()) return null

        if (quality == PlaybackQuality.AUTO) {
            // Auto selects 1080p or highest available resolution
            return videoStreams.firstOrNull { it.height == 1080 }
                ?: videoStreams.firstOrNull { it.height == 720 }
                ?: videoStreams.maxByOrNull { it.height }
        }

        val target = quality.targetHeight
        // تطابق تام أولاً، ثم أقرب ارتفاع <= المطلوب، وإلا الأقل المتاح (يدعم 144/240 وأي سلم قادم).
        videoStreams.firstOrNull { it.height == target }?.let { return it }
        return videoStreams.filter { it.height in 1..target }.maxByOrNull { it.height }
            ?: videoStreams.minByOrNull { it.height }
    }

    /**
     * يختار مسار الترجمة: اللغة المفضلة أولاً (مفضلاً اليدوي على التلقائي)،
     * ثم العربية فالإنجليزية فأي مسار يدوي فالأول.
     */
    fun selectSubtitleTrack(
        tracks: List<SubtitleTrack>,
        preferredLang: String?
    ): SubtitleTrack? {
        if (tracks.isEmpty()) return null
        val preferred = preferredLang
            ?.substringBefore("-")?.substringBefore("_")
            ?.lowercase()?.trim()
        if (!preferred.isNullOrBlank()) {
            val matches = tracks.filter { it.languageCode.lowercase().startsWith(preferred) }
            if (matches.isNotEmpty()) {
                return matches.firstOrNull { !it.isAutoGenerated } ?: matches.first()
            }
        }
        return tracks.firstOrNull { it.isArabic && !it.isAutoGenerated }
            ?: tracks.firstOrNull { it.isArabic }
            ?: tracks.firstOrNull { it.isEnglish && !it.isAutoGenerated }
            ?: tracks.firstOrNull { it.isEnglish }
            ?: tracks.firstOrNull { !it.isAutoGenerated }
            ?: tracks.first()
    }

    private fun createSubtitleSource(track: SubtitleTrack): MediaSource? {
        if (track.url.isBlank()) return null
        val mimeType = when {
            track.mimeType.contains("vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
            track.mimeType.contains("srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
            track.mimeType.contains("subrip", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
            track.mimeType.contains("ttml", ignoreCase = true) -> MimeTypes.APPLICATION_TTML
            track.url.contains(".vtt", ignoreCase = true) -> MimeTypes.TEXT_VTT
            track.url.contains(".srt", ignoreCase = true) -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.TEXT_VTT
        }
        return try {
            val config = MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
                .setMimeType(mimeType)
                .setLanguage(track.languageCode)
                .setLabel(track.label)
                .build()
            SingleSampleMediaSource.Factory(dataSourceFactory)
                .createMediaSource(config, C.TIME_UNSET)
        } catch (_: Exception) {
            null
        }
    }
}
