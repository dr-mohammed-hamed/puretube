package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ChannelRepository URL normalization and caching contracts.
 */
class ChannelRepositoryTest {

    private lateinit var throttler: ExtractorThrottler
    private lateinit var repository: ChannelRepository

    @Before
    fun setUp() {
        throttler = ExtractorThrottler(maxConcurrentCalls = 4)
        repository = ChannelRepository(throttler)
    }

    @Test
    fun resolveChannelUrl_withFullHttpUrl_returnsAsIs() {
        val url = "https://www.youtube.com/channel/UC_test_123"
        assertEquals(url, repository.resolveChannelUrl(url))
    }

    @Test
    fun resolveChannelUrl_withHandle_prependsBaseUrl() {
        val handle = "@DrMohamedHamed"
        assertEquals("https://www.youtube.com/@DrMohamedHamed", repository.resolveChannelUrl(handle))
    }

    @Test
    fun resolveChannelUrl_withChannelId_prependsChannelPath() {
        val ucId = "UC1234567890abcdef"
        assertEquals("https://www.youtube.com/channel/UC1234567890abcdef", repository.resolveChannelUrl(ucId))
    }

    @Test
    fun resolveChannelUrl_withPlainName_prependsHandlePath() {
        val name = "clean_science"
        assertEquals("https://www.youtube.com/@clean_science", repository.resolveChannelUrl(name))
    }

    @Test
    fun resolveChannelUrl_withTrailingSlash_trimsTrailingSlash() {
        val url = "https://www.youtube.com/channel/UC_test_123/"
        assertEquals("https://www.youtube.com/channel/UC_test_123", repository.resolveChannelUrl(url))
    }

    @Test
    fun clearCache_completesWithoutExceptions() = runTest {
        repository.clearCache()
        repository.clearCache("UC123")
    }
}
