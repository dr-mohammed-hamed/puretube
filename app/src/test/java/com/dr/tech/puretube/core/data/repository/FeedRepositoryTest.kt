package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for FeedRepository caching and TTL validation (PM Decision 2).
 */
class FeedRepositoryTest {

    private class FakeSubscriptionDao : SubscriptionDao {
        val subscriptions = mutableListOf<SubscriptionEntity>()

        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = flowOf(subscriptions)
        override suspend fun getAll(): List<SubscriptionEntity> = subscriptions
        override fun isSubscribed(channelId: String): Flow<Boolean> =
            flowOf(subscriptions.any { it.channelId == channelId })

        override suspend fun insert(entity: SubscriptionEntity) {
            subscriptions.removeAll { it.channelId == entity.channelId }
            subscriptions.add(entity)
        }

        override suspend fun insertAll(entities: List<SubscriptionEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteById(channelId: String): Int {
            val removed = subscriptions.removeAll { it.channelId == channelId }
            return if (removed) 1 else 0
        }

        override suspend fun getCount(): Int = subscriptions.size
    }

    private lateinit var fakeDao: FakeSubscriptionDao
    private lateinit var throttler: ExtractorThrottler
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        fakeDao = FakeSubscriptionDao()
        throttler = ExtractorThrottler(maxConcurrentCalls = 4)
        // 500ms TTL for testing caching behavior
        repository = FeedRepository(fakeDao, throttler, cacheTtlMs = 500L)
    }

    @Test
    fun getFeed_withEmptySubscriptions_returnsEmptyListAndCaches() = runTest {
        val result = repository.getFeed()
        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
        assertTrue(repository.isCacheValid())
    }

    @Test
    fun clearCache_invalidatesCacheSuccessfully() = runTest {
        repository.getFeed()
        assertTrue(repository.isCacheValid())

        repository.clearCache()
        assertFalse(repository.isCacheValid())
    }

    @Test
    fun extractVideoId_extractsCleanIdsFromVariousYouTubeUrls() {
        assertEquals("dQw4w9WgXcQ", FeedRepository.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", FeedRepository.extractVideoId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
        assertEquals("dQw4w9WgXcQ", FeedRepository.extractVideoId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", FeedRepository.extractVideoId("https://www.youtube.com/shorts/dQw4w9WgXcQ"))
    }

    @Test
    fun resolveChannelUrl_formatsHandlesAndStandardIdsCorrectly() {
        assertEquals("https://www.youtube.com/@AliMuhammadAli", FeedRepository.resolveChannelUrl("@AliMuhammadAli"))
        assertEquals("https://www.youtube.com/channel/UCq4qX1L1N1yS1rL8B4L0j8g", FeedRepository.resolveChannelUrl("UCq4qX1L1N1yS1rL8B4L0j8g"))
        assertEquals("https://www.youtube.com/c/SomeCustomChannel", FeedRepository.resolveChannelUrl("https://www.youtube.com/c/SomeCustomChannel"))
        assertEquals("http://www.youtube.com/channel/UC123", FeedRepository.resolveChannelUrl("http://www.youtube.com/channel/UC123"))
    }

    @Test
    fun getFeed_whenAllChannelsFail_preservesExistingCache() = runTest {
        // Setup initial subscription
        fakeDao.insert(
            SubscriptionEntity(
                channelId = "UC123",
                channelName = "Test Channel"
            )
        )

        var shouldFail = false
        val sampleVideo = FeedVideoItem(
            videoId = "vid1",
            title = "Cached Video",
            channelId = "UC123",
            channelName = "Test Channel",
            thumbnailUrl = null,
            durationMs = 120_000L,
            uploadDateMs = 1000L
        )

        val testRepository = FeedRepository(
            subscriptionDao = fakeDao,
            extractorThrottler = throttler,
            cacheTtlMs = 500L,
            channelVideosFetcher = { _, _ ->
                if (shouldFail) {
                    Result.failure(IOException("Simulated Network Outage"))
                } else {
                    Result.success(listOf(sampleVideo))
                }
            }
        )

        // 1. Initial successful fetch populates the cache
        val initialResult = testRepository.getFeed(forceRefresh = true)
        assertTrue(initialResult.isSuccess)
        assertEquals(1, initialResult.getOrNull()?.size)
        assertEquals("vid1", initialResult.getOrNull()?.first()?.videoId)

        // 2. Simulate total network outage
        shouldFail = true

        // 3. Force refresh while offline: all channel fetches fail
        val failedRefreshResult = testRepository.getFeed(forceRefresh = true)

        // Verify that existing cache was preserved and returned
        assertTrue(failedRefreshResult.isSuccess)
        val fallbackFeed = failedRefreshResult.getOrNull()
        assertEquals(1, fallbackFeed?.size)
        assertEquals("vid1", fallbackFeed?.first()?.videoId)
        assertTrue(testRepository.isCacheValid())
    }

    @Test
    fun getFeed_whenAllChannelsFailAndNoCacheExists_returnsFailure() = runTest {
        fakeDao.insert(
            SubscriptionEntity(
                channelId = "UC123",
                channelName = "Test Channel"
            )
        )

        val testRepository = FeedRepository(
            subscriptionDao = fakeDao,
            extractorThrottler = throttler,
            cacheTtlMs = 500L,
            channelVideosFetcher = { _, _ ->
                Result.failure(IOException("Network unreachable"))
            }
        )

        val result = testRepository.getFeed(forceRefresh = true)
        assertTrue(result.isFailure)
    }
}
