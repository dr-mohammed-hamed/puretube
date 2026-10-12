package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.data.model.SearchFilter
import com.dr.tech.puretube.core.data.model.SearchResultItem
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Unit tests for SearchRepository: empty-query guard, item mapping,
 * fake-fetcher success/failure paths, and filter params mapping.
 */
class SearchRepositoryTest {

    private lateinit var throttler: ExtractorThrottler

    @Before
    fun setUp() {
        throttler = ExtractorThrottler(maxConcurrentCalls = 4)
    }

    @Test
    fun search_blankQuery_returnsEmptySuccessWithoutNetwork() = runTest {
        val repository = SearchRepositoryImpl(extractorThrottler = throttler)
        val result = repository.search("   ", SearchFilter.ALL)
        assertTrue(result.isSuccess)
        val (items, nextPage) = checkNotNull(result.getOrNull())
        assertTrue(items.isEmpty())
        assertNull(nextPage)
    }

    @Test
    fun getNextPage_blankToken_returnsEmptySuccess() = runTest {
        val repository = SearchRepositoryImpl(extractorThrottler = throttler)
        val result = repository.getNextPage("")
        assertTrue(result.isSuccess)
        assertTrue(checkNotNull(result.getOrNull()).first.isEmpty())
    }

    @Test
    fun mapStreamInfoItem_mapsVideoFields() {
        val item = StreamInfoItem(
            0,
            "https://www.youtube.com/watch?v=vid123",
            "درس التفسير",
            StreamType.VIDEO_STREAM
        ).apply {
            setUploaderName("قناة التزكية")
            setUploaderUrl("https://www.youtube.com/channel/UCabc")
            setDuration(300L)
            setViewCount(1500L)
            setTextualUploadDate("قبل يومين")
        }

        val mapped = mapStreamInfoItem(item)
        checkNotNull(mapped)
        assertEquals("vid123", mapped.videoId)
        assertEquals("درس التفسير", mapped.title)
        assertEquals("UCabc", mapped.channelId)
        assertEquals("قناة التزكية", mapped.channelName)
        assertEquals(300_000L, mapped.durationMs)
        assertEquals(1500L, mapped.viewCount)
        assertEquals("قبل يومين", mapped.uploadDateText)
    }

    @Test
    fun mapStreamInfoItem_blankUrl_returnsNull() {
        val item = StreamInfoItem(0, "", "بدون رابط", StreamType.VIDEO_STREAM)
        assertNull(mapStreamInfoItem(item))
    }

    @Test
    fun mapChannelInfoItem_mapsChannelFields() {
        val item = ChannelInfoItem(
            0,
            "https://www.youtube.com/channel/UCxyz",
            "قناة النور"
        ).apply {
            setSubscriberCount(25000L)
        }

        val mapped = mapChannelInfoItem(item)
        assertEquals("UCxyz", mapped.channelId)
        assertEquals("قناة النور", mapped.channelName)
        assertEquals("25000", mapped.subscriberCountText)
    }

    @Test
    fun mapSearchInfoItems_mixesVideosAndChannels() {
        val video = StreamInfoItem(
            0,
            "https://www.youtube.com/watch?v=v1",
            "فيديو",
            StreamType.VIDEO_STREAM
        )
        val channel = ChannelInfoItem(0, "https://www.youtube.com/@handle1", "قناة")
        val items = mapSearchInfoItems(listOf(video, channel))
        assertEquals(2, items.size)
        assertTrue(items[0] is SearchResultItem.VideoItem)
        assertTrue(items[1] is SearchResultItem.ChannelItem)
        assertEquals("@handle1", (items[1] as SearchResultItem.ChannelItem).handle)
    }

    @Test
    fun searchFilter_mapsToNewPipeParams() {
        // القيم مطابقة لثوابت YoutubeSearchQueryHandlerFactory v0.26.5 الموثقة.
        assertEquals(listOf("videos"), SearchFilter.VIDEOS.toContentFilter())
        assertEquals(listOf("channels"), SearchFilter.CHANNELS.toContentFilter())
        assertEquals(listOf("all"), SearchFilter.ALL.toContentFilter())
    }

    @Test
    fun searchFilter_fromStringFallsBackToAll() {
        assertEquals(SearchFilter.VIDEOS, SearchFilter.fromString("videos"))
        assertEquals(SearchFilter.CHANNELS, SearchFilter.fromString("channels"))
        assertEquals(SearchFilter.ALL, SearchFilter.fromString("playlists"))
        assertEquals(SearchFilter.ALL, SearchFilter.fromString(null))
    }

    @Test
    fun search_fakeFetcherSuccess_returnsItemsAndToken() = runTest {
        val fakeItems = listOf(
            SearchResultItem.VideoItem("v1", "عنوان", "c1", "قناة")
        )
        val repository = SearchRepositoryImpl(
            extractorThrottler = throttler,
            customSearchFetcher = { _, _ -> fakeItems to "token-1" }
        )
        val result = repository.search("قرآن", SearchFilter.VIDEOS)
        assertTrue(result.isSuccess)
        val (items, nextPage) = checkNotNull(result.getOrNull())
        assertEquals(1, items.size)
        assertEquals("token-1", nextPage)
    }

    @Test
    fun search_fakeFetcherFailure_returnsFailure() = runTest {
        val repository = SearchRepositoryImpl(
            extractorThrottler = throttler,
            customSearchFetcher = { _, _ -> throw java.io.IOException("network down") }
        )
        val result = repository.search("قرآن", SearchFilter.ALL)
        assertTrue(result.isFailure)
    }

    @Test
    fun getNextPage_fakeFetcherFailure_returnsFailure() = runTest {
        val repository = SearchRepositoryImpl(
            extractorThrottler = throttler,
            customNextPageFetcher = { throw java.io.IOException("stale page") }
        )
        val result = repository.getNextPage("expired-token")
        assertTrue(result.isFailure)
    }

    @Test
    fun getNextPage_unknownTokenWithoutFake_returnsFailure() = runTest {
        val repository = SearchRepositoryImpl(extractorThrottler = throttler)
        val result = repository.getNextPage("no-such-token")
        assertTrue(result.isFailure)
    }
}
