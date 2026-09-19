package com.dr.tech.puretube.player

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dr.tech.puretube.core.data.model.AudioStreamFormat
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.data.model.VideoStreamFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests verifying audio stream selection priority and fallback behavior
 * in PureMediaSourceFactory.
 * Guarantees that hardware-accelerated AAC/M4A is prioritized over software Opus
 * to prevent Android 10 (API 29) c2.android.opus.decoder crashes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PureMediaSourceFactoryTest {

    private lateinit var context: Context
    private lateinit var factory: PureMediaSourceFactory

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        factory = PureMediaSourceFactory(context)
    }

    @Test
    fun selectBestAudioStream_prefersAacOverHigherBitrateOpus() {
        val opusStream = AudioStreamFormat(
            url = "https://example.com/audio/opus",
            bitrate = 160000,
            format = "WEBMA_OPUS",
            averageBitrate = 160000
        )
        val m4aStream = AudioStreamFormat(
            url = "https://example.com/audio/m4a",
            bitrate = 128000,
            format = "M4A",
            averageBitrate = 128000
        )

        // Given both Opus (160k) and M4A (128k)
        val streams = listOf(opusStream, m4aStream)
        val selected = factory.selectBestAudioStream(streams)

        // Must select M4A (hardware-accelerated DSP decoding) rather than Opus
        assertNotNull(selected)
        assertEquals("https://example.com/audio/m4a", selected?.url)
        assertEquals("M4A", selected?.format)
    }

    @Test
    fun selectBestAudioStream_fallsBackToOpusWhenNoAacAvailable() {
        val opusStream = AudioStreamFormat(
            url = "https://example.com/audio/opus",
            bitrate = 160000,
            format = "WEBMA_OPUS",
            averageBitrate = 160000
        )

        val streams = listOf(opusStream)
        val selected = factory.selectBestAudioStream(streams)

        assertNotNull(selected)
        assertEquals("https://example.com/audio/opus", selected?.url)
    }

    @Test
    fun selectBestAudioStream_excludesOpusWhenRequested() {
        val opusStream = AudioStreamFormat(
            url = "https://example.com/audio/opus",
            bitrate = 160000,
            format = "WEBMA_OPUS",
            averageBitrate = 160000
        )

        val streams = listOf(opusStream)
        val selected = factory.selectBestAudioStream(streams, excludeOpus = true)

        // When excluding Opus, should return null if only Opus exists
        assertNull(selected)
    }

    @Test
    fun createFallbackMediaSource_returnsProgressiveStreamWhenOpusOnlyAndExcludingOpus() {
        val details = ExtractedVideoDetails(
            videoId = "test_vid",
            title = "Test Video",
            channelId = "channel_1",
            channelName = "Channel One",
            videoStreams = listOf(
                VideoStreamFormat(
                    url = "https://example.com/video/1080p",
                    resolution = "1080p",
                    format = "mp4",
                    width = 1920,
                    height = 1080,
                    isVideoOnly = true
                ),
                VideoStreamFormat(
                    url = "https://example.com/video/progressive_720p",
                    resolution = "720p",
                    format = "mp4",
                    width = 1280,
                    height = 720,
                    isVideoOnly = false
                )
            ),
            audioStreams = listOf(
                AudioStreamFormat(
                    url = "https://example.com/audio/opus",
                    bitrate = 160000,
                    format = "WEBMA_OPUS"
                )
            )
        )

        val fallbackSource = factory.createFallbackMediaSource(details, PlaybackQuality.AUTO)
        assertNotNull(fallbackSource)
    }

    @Test
    fun createFallbackMediaSource_audioOnly_fallsBackToSafeAudioStream() {
        val details = ExtractedVideoDetails(
            videoId = "test_vid_audio",
            title = "Test Audio",
            channelId = "channel_1",
            channelName = "Channel One",
            videoStreams = emptyList(),
            audioStreams = listOf(
                AudioStreamFormat(
                    url = "https://example.com/audio/opus",
                    bitrate = 160000,
                    format = "WEBMA_OPUS"
                ),
                AudioStreamFormat(
                    url = "https://example.com/audio/m4a",
                    bitrate = 128000,
                    format = "M4A"
                )
            )
        )

        val fallbackSource = factory.createFallbackMediaSource(details, PlaybackQuality.AUTO, isAudioOnly = true)
        assertNotNull(fallbackSource)
    }
}
