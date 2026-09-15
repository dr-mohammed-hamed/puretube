package com.dr.tech.puretube.features

import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.repository.FeedRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import com.dr.tech.puretube.features.feed.FeedViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FeedViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSubDao : SubscriptionDao {
        val subsFlow = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = subsFlow
        override suspend fun getAll(): List<SubscriptionEntity> = subsFlow.value
        override fun isSubscribed(channelId: String): Flow<Boolean> =
            MutableStateFlow(subsFlow.value.any { it.channelId == channelId })
        override suspend fun insert(entity: SubscriptionEntity) {
            val list = subsFlow.value.toMutableList()
            list.removeAll { it.channelId == entity.channelId }
            list.add(entity)
            subsFlow.value = list
        }
        override suspend fun insertAll(entities: List<SubscriptionEntity>) {
            entities.forEach { insert(it) }
        }
        override suspend fun deleteById(channelId: String): Int {
            val list = subsFlow.value.toMutableList()
            val removed = list.removeAll { it.channelId == channelId }
            subsFlow.value = list
            return if (removed) 1 else 0
        }
        override suspend fun getCount(): Int = subsFlow.value.size
    }

    private class FakeWlDao : WatchLaterDao {
        val wlFlow = MutableStateFlow<List<WatchLaterEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<WatchLaterEntity>> = wlFlow
        override fun isInWatchLater(videoId: String): Flow<Boolean> =
            MutableStateFlow(wlFlow.value.any { it.videoId == videoId })
        override suspend fun insert(entity: WatchLaterEntity) {
            val list = wlFlow.value.toMutableList()
            list.removeAll { it.videoId == entity.videoId }
            list.add(entity)
            wlFlow.value = list
        }
        override suspend fun deleteById(videoId: String): Int {
            val list = wlFlow.value.toMutableList()
            val removed = list.removeAll { it.videoId == videoId }
            wlFlow.value = list
            return if (removed) 1 else 0
        }
        override suspend fun clearAll(): Int = 0
        override suspend fun getCount(): Int = wlFlow.value.size
    }

    private lateinit var subDao: FakeSubDao
    private lateinit var wlDao: FakeWlDao
    private lateinit var feedRepo: FeedRepository
    private lateinit var subRepo: SubscriptionRepository
    private lateinit var wlRepo: WatchLaterRepository
    private lateinit var viewModel: FeedViewModel

    @Before
    fun setUp() {
        subDao = FakeSubDao()
        wlDao = FakeWlDao()
        val throttler = ExtractorThrottler(4)
        val dispatcher = mainDispatcherRule.testDispatcher
        feedRepo = FeedRepository(
            subscriptionDao = subDao,
            extractorThrottler = throttler,
            cacheTtlMs = 60000L,
            channelVideosFetcher = { _, _ ->
                Result.success(listOf(FeedVideoItem("v1", "Title 1", "c1", "Channel 1", null, 60000L, 1000L)))
            },
            ioDispatcher = dispatcher
        )
        subRepo = SubscriptionRepository(subDao, throttler, ImportExportService(), ioDispatcher = dispatcher)
        wlRepo = WatchLaterRepository(wlDao, ioDispatcher = dispatcher)
        viewModel = FeedViewModel(feedRepo, subRepo, wlRepo, ioDispatcher = dispatcher)
    }

    @Test
    fun initialState_whenSubscriptionsEmpty_marksEmptySubscriptions() {
        assertTrue(viewModel.uiState.value.isEmptySubscriptions)
        assertTrue(viewModel.uiState.value.videos.isEmpty())
    }

    @Test
    fun toggleBookmark_addsAndRemovesFromWatchLater() = runTest {
        val video = FeedVideoItem("v100", "Video 100", "c1", "Channel 1", null, 50000L, 1000L)

        viewModel.toggleBookmark(video)
        assertTrue(viewModel.uiState.value.bookmarkedVideoIds.contains("v100"))

        viewModel.toggleBookmark(video)
        assertTrue(viewModel.uiState.value.bookmarkedVideoIds.isEmpty())
    }

    @Test
    fun refresh_withSubscribedChannel_loadsVideos() = runTest {
        subDao.insert(SubscriptionEntity("c1", "Channel 1", "@c1", null, "100", true, 1000L))
        viewModel.refresh(force = true)

        assertEquals(1, viewModel.uiState.value.videos.size)
        assertEquals("v1", viewModel.uiState.value.videos.first().videoId)
    }
}
