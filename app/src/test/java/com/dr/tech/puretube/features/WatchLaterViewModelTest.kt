package com.dr.tech.puretube.features

import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.features.watchlater.WatchLaterViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WatchLaterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeWatchLaterDao : WatchLaterDao {
        val itemsFlow = MutableStateFlow<List<WatchLaterEntity>>(emptyList())

        override fun getAllFlow(): Flow<List<WatchLaterEntity>> = itemsFlow
        override fun isInWatchLater(videoId: String): Flow<Boolean> =
            MutableStateFlow(itemsFlow.value.any { it.videoId == videoId })

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

    private lateinit var fakeDao: FakeWatchLaterDao
    private lateinit var repository: WatchLaterRepository
    private lateinit var viewModel: WatchLaterViewModel

    @Before
    fun setUp() {
        fakeDao = FakeWatchLaterDao()
        repository = WatchLaterRepository(fakeDao, mainDispatcherRule.testDispatcher)
        viewModel = WatchLaterViewModel(repository)
    }

    @Test
    fun initialState_isEmpty() {
        assertEquals(emptyList<WatchLaterEntity>(), viewModel.uiState.value.items)
    }

    @Test
    fun itemsFlow_updatesUiStateReactively() = runTest {
        val entity = WatchLaterEntity(
            videoId = "v1",
            title = "Test Video",
            channelId = "c1",
            channelName = "Test Channel",
            thumbnailUrl = null,
            durationMs = 120000L,
            addedAtMs = 1000L
        )

        repository.addToWatchLater(entity)
        assertEquals(1, viewModel.uiState.value.items.size)
        assertEquals("Test Video", viewModel.uiState.value.items.first().title)

        viewModel.removeItem("v1")
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }

    @Test
    fun clearAll_removesAllItems() = runTest {
        repository.addToWatchLater(WatchLaterEntity("v1", "T1", "c1", "C1", null, 0L, 0L))
        repository.addToWatchLater(WatchLaterEntity("v2", "T2", "c2", "C2", null, 0L, 0L))

        assertEquals(2, viewModel.uiState.value.items.size)
        viewModel.clearAll()
        assertTrue(viewModel.uiState.value.items.isEmpty())
    }
}
