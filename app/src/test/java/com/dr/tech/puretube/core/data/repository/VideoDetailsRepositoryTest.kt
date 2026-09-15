package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.data.model.AudioStreamFormat
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.model.VideoStreamFormat
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for VideoDetailsRepository stream extraction and channel video listing.
 */
class VideoDetailsRepositoryTest {

    private lateinit var throttler: ExtractorThrottler
    private lateinit var repository: VideoDetailsRepository

    private val fakeVideoDetails = ExtractedVideoDetails(
        videoId = "test_vid_123",
        title = "تفسير سورة الفاتحة",
        channelId = "UC_islamic_channel",
        channelName = "قناة التزكية",
        thumbnailUrl = "https://mock.googlevideo.com/thumb.jpg",
        durationMs = 15 * 60 * 1000L,
        videoStreams = listOf(
            VideoStreamFormat(
                url = "https://mock.googlevideo.com/videoplayback?itag=137",
                resolution = "1080p",
                format = "mp4",
                width = 1920,
                height = 1080,
                isVideoOnly = true
            ),
            VideoStreamFormat(
                url = "https://mock.googlevideo.com/videoplayback?itag=22",
                resolution = "720p",
                format = "mp4",
                width = 1280,
                height = 720,
                isVideoOnly = false
            )
        ),
        audioStreams = listOf(
            AudioStreamFormat(
                url = "https://mock.googlevideo.com/videoplayback?itag=140",
                bitrate = 128,
                format = "m4a",
                averageBitrate = 128
            )
        )
    )

    private val fakeChannelVideos = listOf(
        FeedVideoItem(
            videoId = "rel_1",
            title = "المقطع التالي للقناة",
            channelId = "UC_islamic_channel",
            channelName = "قناة التزكية",
            durationMs = 300_000L
        )
    )

    @Before
    fun setUp() {
        throttler = ExtractorThrottler(maxConcurrentCalls = 4)
        repository = VideoDetailsRepositoryImpl(
            extractorThrottler = throttler,
            customStreamFetcher = { videoId ->
                if (videoId == "test_vid_123") fakeVideoDetails else throw IllegalArgumentException("Not found")
            },
            customChannelVideosFetcher = { channelId, _ ->
                if (channelId == "UC_islamic_channel") fakeChannelVideos else emptyList()
            }
        )
    }

    @Test
    fun extractVideoDetails_success_returnsParsedDetails() = runTest {
        val result = repository.extractVideoDetails("test_vid_123")
        assertTrue(result.isSuccess)
        val details = checkNotNull(result.getOrNull())
        assertEquals("test_vid_123", details.videoId)
        assertEquals("تفسير سورة الفاتحة", details.title)
        assertEquals("https://mock.googlevideo.com/thumb.jpg", details.thumbnailUrl)
        assertEquals(2, details.videoStreams.size)
        assertEquals(1, details.audioStreams.size)
        assertEquals(15 * 60 * 1000L, details.durationMs)
    }

    @Test
    fun extractVideoDetails_error_returnsFailure() = runTest {
        val result = repository.extractVideoDetails("invalid_id")
        assertTrue(result.isFailure)
    }

    @Test
    fun getChannelVideos_returnsAuthorVideosExclusively() = runTest {
        val result = repository.getChannelVideos("UC_islamic_channel", limit = 10)
        assertTrue(result.isSuccess)
        val list = checkNotNull(result.getOrNull())
        assertEquals(1, list.size)
        assertEquals("المقطع التالي للقناة", list.first().title)
    }
}
