package com.dr.tech.puretube.features

import com.dr.tech.puretube.core.data.model.ChannelProfileDetails
import com.dr.tech.puretube.core.data.model.ChannelSortOrder
import com.dr.tech.puretube.core.data.model.ChannelTabType
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.repository.ChannelRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import com.dr.tech.puretube.features.channel.ChannelViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ChannelViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeSubDao : SubscriptionDao {
        val subsFlow = MutableStateFlow<List<SubscriptionEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<SubscriptionEntity>> = subsFlow
        override suspend fun getAll(): List<SubscriptionEntity> = subsFlow.value
        override fun isSubscribed(channelId: String, handle: String?): Flow<Boolean> =
            subsFlow.map { list -> list.any { it.channelId == channelId || it.channelHandle == channelId || it.channelId == handle || it.channelHandle == handle } }
        override suspend fun insert(entity: SubscriptionEntity) {
            val list = subsFlow.value.toMutableList()
            list.removeAll { it.channelId == entity.channelId }
            list.add(entity)
            subsFlow.value = list
        }
        override suspend fun insertAll(entities: List<SubscriptionEntity>) {
            val list = subsFlow.value.toMutableList()
            entities.forEach { entity ->
                list.removeAll { it.channelId == entity.channelId }
                list.add(entity)
            }
            subsFlow.value = list
        }
        override suspend fun deleteById(channelId: String, handle: String?): Int {
            val list = subsFlow.value.toMutableList()
            val removed = list.removeAll { it.channelId == channelId || it.channelHandle == channelId || it.channelId == handle || it.channelHandle == handle }
            subsFlow.value = list
            return if (removed) 1 else 0
        }
        override suspend fun getCount(): Int = subsFlow.value.size
    }

    private class FakeWatchLaterDao : WatchLaterDao {
        val itemsFlow = MutableStateFlow<List<WatchLaterEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<WatchLaterEntity>> = itemsFlow
        override fun isInWatchLater(videoId: String): Flow<Boolean> =
            itemsFlow.map { list -> list.any { it.videoId == videoId } }
        override suspend fun insert(entity: WatchLaterEntity) {
            val list = itemsFlow.value.toMutableList()
            list.removeAll { it.videoId == entity.videoId }
            list.add(entity)
            itemsFlow.value = list
        }
        override suspend fun deleteById(videoId: String): Int {
            val list = itemsFlow.value.toMutableList()
            val removed = list.removeAll { it.videoId == videoId }
            itemsFlow.value = list
            return if (removed) 1 else 0
        }
        override suspend fun clearAll(): Int {
            val count = itemsFlow.value.size
            itemsFlow.value = emptyList()
            return count
        }
        override suspend fun getCount(): Int = itemsFlow.value.size
    }

    private class FakeChannelRepository(
        throttler: ExtractorThrottler,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) : ChannelRepository(throttler, dispatcher) {
        var profileToReturn: ChannelProfileDetails = ChannelProfileDetails(
            channelId = "UC123",
            channelName = "Test Channel",
            channelHandle = "@test",
            subscriberCount = 1000L,
            subscriberCountText = "1K"
        )
        var shouldThrowError: Boolean = false
        var shouldThrowFatalError: Boolean = false

        override suspend fun getChannelProfile(
            channelIdOrUrl: String,
            forceRefresh: Boolean
        ): Result<ChannelProfileDetails> {
            if (shouldThrowFatalError) {
                throw NoSuchMethodError("Simulated LinkageError/NoSuchMethodError")
            }
            if (shouldThrowError) {
                return Result.failure(java.io.IOException("Network unreachable"))
            }
            return Result.success(profileToReturn)
        }
    }

    private lateinit var subDao: FakeSubDao
    private lateinit var watchLaterDao: FakeWatchLaterDao
    private lateinit var channelRepo: FakeChannelRepository
    private lateinit var subRepo: SubscriptionRepository
    private lateinit var watchLaterRepo: WatchLaterRepository
    private lateinit var viewModel: ChannelViewModel

    @Before
    fun setUp() {
        val throttler = ExtractorThrottler(maxConcurrentCalls = 4)
        subDao = FakeSubDao()
        watchLaterDao = FakeWatchLaterDao()
        channelRepo = FakeChannelRepository(throttler, mainDispatcherRule.testDispatcher)
        subRepo = SubscriptionRepository(subDao, throttler, ImportExportService(), mainDispatcherRule.testDispatcher)
        watchLaterRepo = WatchLaterRepository(watchLaterDao, mainDispatcherRule.testDispatcher)
        viewModel = ChannelViewModel(channelRepo, subRepo, watchLaterRepo)
    }

    @Test
    fun selectTab_updatesSelectedTabInUiState() {
        assertEquals(ChannelTabType.VIDEOS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(ChannelTabType.PLAYLISTS)
        assertEquals(ChannelTabType.PLAYLISTS, viewModel.uiState.value.selectedTab)

        viewModel.selectTab(ChannelTabType.ABOUT)
        assertEquals(ChannelTabType.ABOUT, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun changeSortOrder_updatesSortOrderInUiState() {
        assertEquals(ChannelSortOrder.LATEST, viewModel.uiState.value.sortOrder)

        viewModel.changeSortOrder(ChannelSortOrder.POPULAR)
        assertEquals(ChannelSortOrder.POPULAR, viewModel.uiState.value.sortOrder)

        viewModel.changeSortOrder(ChannelSortOrder.OLDEST)
        assertEquals(ChannelSortOrder.OLDEST, viewModel.uiState.value.sortOrder)
    }

    @Test
    fun initChannel_loadsProfileAndObservesSubscription() = runTest {
        viewModel.initChannel("UC123")
        testScheduler.advanceUntilIdle()

        assertEquals("UC123", viewModel.uiState.value.profile?.channelId)
        assertEquals("Test Channel", viewModel.uiState.value.profile?.channelName)
        assertFalse(viewModel.uiState.value.isSubscribed)
    }

    @Test
    fun toggleSubscribe_togglesSubscriptionInDaoAndUpdatesState() = runTest {
        viewModel.initChannel("UC123")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubscribed)

        viewModel.toggleSubscribe()
        testScheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSubscribed)
        assertTrue(subDao.subsFlow.value.any { it.channelId == "UC123" })

        viewModel.toggleSubscribe()
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSubscribed)
        assertFalse(subDao.subsFlow.value.any { it.channelId == "UC123" })
    }

    @Test
    fun toggleBookmark_addsAndRemovesFromWatchLater() = runTest {
        val video = FeedVideoItem(
            videoId = "vid_123",
            title = "Test Video",
            channelId = "UC123",
            channelName = "Test Channel"
        )

        assertFalse(viewModel.uiState.value.bookmarkedVideoIds.contains(video.videoId))

        viewModel.toggleBookmark(video)
        testScheduler.advanceUntilIdle()
        assertTrue(watchLaterDao.itemsFlow.value.any { it.videoId == "vid_123" })

        viewModel.toggleBookmark(video)
        testScheduler.advanceUntilIdle()
        assertFalse(watchLaterDao.itemsFlow.value.any { it.videoId == "vid_123" })
    }

    @Test
    fun initChannel_onFailure_updatesUiStateWithErrorAndResetsLoading() = runTest {
        channelRepo.shouldThrowError = true

        viewModel.initChannel("UC_ERROR")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingProfile)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("الاتصال بالإنترنت") == true)
    }

    @Test
    fun initChannel_onFatalError_isCaughtByExceptionHandlerAndUpdatesError() = runTest {
        channelRepo.shouldThrowFatalError = true

        viewModel.initChannel("UC_FATAL")
        testScheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoadingProfile)
        assertTrue(viewModel.uiState.value.errorMessage?.contains("عدم توافق في النظام") == true)
    }
}
