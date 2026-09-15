package com.dr.tech.puretube.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.dr.tech.puretube.core.data.model.AudioStreamFormat
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.PlaybackQuality
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
     */
    fun createMediaSource(
        details: ExtractedVideoDetails,
        quality: PlaybackQuality = PlaybackQuality.AUTO,
        isAudioOnly: Boolean = false
    ): MediaSource? {
        val selectedAudio = selectBestAudioStream(details.audioStreams)

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
            return MergingMediaSource(videoSource, audioSource)
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

    private fun createSingleSource(uriString: String, details: ExtractedVideoDetails): MediaSource {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(details.title)
            .setArtist(details.channelName)
            .setArtworkUri(details.channelAvatarUrl?.let { Uri.parse(it) })
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uriString)
            .setMediaMetadata(mediaMetadata)
            .build()
        return ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
    }

    private fun selectBestAudioStream(audioStreams: List<AudioStreamFormat>): AudioStreamFormat? {
        return audioStreams.maxByOrNull { it.bitrate }
    }

    private fun selectVideoStream(
        videoStreams: List<VideoStreamFormat>,
        quality: PlaybackQuality
    ): VideoStreamFormat? {
        if (videoStreams.isEmpty()) return null

        return when (quality) {
            PlaybackQuality.AUTO -> {
                // Auto selects 1080p or highest available resolution
                videoStreams.firstOrNull { it.height == 1080 }
                    ?: videoStreams.firstOrNull { it.height == 720 }
                    ?: videoStreams.maxByOrNull { it.height }
            }
            PlaybackQuality.HD_1080 -> {
                videoStreams.firstOrNull { it.height == 1080 }
                    ?: selectVideoStream(videoStreams, PlaybackQuality.AUTO)
            }
            PlaybackQuality.HD_720 -> {
                videoStreams.firstOrNull { it.height == 720 }
                    ?: selectVideoStream(videoStreams, PlaybackQuality.AUTO)
            }
            PlaybackQuality.SD_480 -> {
                videoStreams.firstOrNull { it.height == 480 }
                    ?: selectVideoStream(videoStreams, PlaybackQuality.AUTO)
            }
            PlaybackQuality.SD_360 -> {
                videoStreams.firstOrNull { it.height == 360 }
                    ?: selectVideoStream(videoStreams, PlaybackQuality.AUTO)
            }
        }
    }
}
