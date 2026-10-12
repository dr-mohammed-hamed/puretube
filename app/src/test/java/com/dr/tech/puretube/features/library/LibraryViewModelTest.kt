package com.dr.tech.puretube.features.library

import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.PlaylistRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.dao.HistoryDao
import com.dr.tech.puretube.core.database.dao.PlaylistDao
import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.features.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeHistoryDao : HistoryDao {
        val itemsFlow = MutableStateFlow<List<HistoryEntity>>(emptyList())

        override fun getRecentFlow(limit: Int): Flow<List<HistoryEntity>> =
            itemsFlow.map { it.take(limit) }

        override suspend fun getPlaybackPosition(videoId: String): Long? =
            itemsFlow.value.firstOrNull { it.videoId == videoId }?.lastPlaybackPositionMs

        override suspend fun getById(videoId: String): HistoryEntity? =
            itemsFlow.value.firstOrNull { it.videoId == videoId }

        override suspend fun upsert(entity: HistoryEntity) {
            val list = itemsFlow.value.toMutableList()
            list.removeAll { it.videoId == entity.videoId }
            list.add(0, entity)
            itemsFlow.value = list
        }

        override suspend fun updatePlaybackPosition(videoId: String, positionMs: Long, timestampMs: Long): Int {
            val list = itemsFlow.value.toMutableList()
            val index = list.indexOfFirst { it.videoId == videoId }
            if (index < 0) return 0
            list[index] = list[index].copy(lastPlaybackPositionMs = positionMs, lastPlayedAtMs = timestampMs)
            itemsFlow.value = list
            return 1
        }

        override suspend fun markCompleted(videoId: String): Int {
            val list = itemsFlow.value.toMutableList()
            val index = list.indexOfFirst { it.videoId == videoId }
            if (index < 0) return 0
            list[index] = list[index].copy(isCompleted = true, lastPlaybackPositionMs = 0L)
            itemsFlow.value = list
            return 1
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

    private class FakePlaylistDao : PlaylistDao {
        private var nextPlaylistId = 0L
        private var nextItemId = 0L
        val playlistsFlow = MutableStateFlow<List<PlaylistEntity>>(emptyList())
        val itemsFlow = MutableStateFlow<List<PlaylistItemEntity>>(emptyList())

        override fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> = playlistsFlow
        override fun getItemsFlow(playlistId: Long): Flow<List<PlaylistItemEntity>> =
            itemsFlow.map { list -> list.filter { it.playlistId == playlistId }.sortedBy { it.position } }

        override suspend fun getPlaylistCount(): Int = playlistsFlow.value.size
        override suspend fun getItemCount(playlistId: Long): Int =
            itemsFlow.value.count { it.playlistId == playlistId }

        override suspend fun isInPlaylist(playlistId: Long, videoId: String): Boolean =
            itemsFlow.value.any { it.playlistId == playlistId && it.videoId == videoId }

        override suspend fun getPlaylistIdsContainingVideo(videoId: String): List<Long> =
            itemsFlow.value.filter { it.videoId == videoId }.map { it.playlistId }.distinct()

        override suspend fun insertPlaylist(entity: PlaylistEntity): Long {
            val id = ++nextPlaylistId
            playlistsFlow.value = playlistsFlow.value + entity.copy(id = id)
            return id
        }

        override suspend fun insertItem(entity: PlaylistItemEntity): Long {
            if (itemsFlow.value.any { it.playlistId == entity.playlistId && it.videoId == entity.videoId }) {
                return -1L
            }
            val id = ++nextItemId
            itemsFlow.value = itemsFlow.value + entity.copy(id = id)
            return id
        }

        override suspend fun insertItems(entities: List<PlaylistItemEntity>): List<Long> =
            entities.map { insertItem(it) }

        override suspend fun removeItem(playlistId: Long, videoId: String): Int {
            val before = itemsFlow.value.size
            itemsFlow.value = itemsFlow.value.filterNot { it.playlistId == playlistId && it.videoId == videoId }
            return before - itemsFlow.value.size
        }

        override suspend fun deletePlaylist(playlistId: Long): Int {
            val before = playlistsFlow.value.size
            playlistsFlow.value = playlistsFlow.value.filterNot { it.id == playlistId }
            itemsFlow.value = itemsFlow.value.filterNot { it.playlistId == playlistId }
            return before - playlistsFlow.value.size
        }

        override suspend fun clearPlaylist(playlistId: Long): Int {
            val before = itemsFlow.value.size
            itemsFlow.value = itemsFlow.value.filterNot { it.playlistId == playlistId }
            return before - itemsFlow.value.size
        }

        override suspend fun updatePosition(itemId: Long, position: Int): Int = 1

        override suspend fun renamePlaylist(playlistId: Long, name: String, timestampMs: Long): Int = 1

        override suspend fun touchPlaylist(playlistId: Long, timestampMs: Long): Int = 1
    }

    private lateinit var historyDao: FakeHistoryDao
    private lateinit var watchLaterDao: FakeWatchLaterDao
    private lateinit var playlistDao: FakePlaylistDao
    private lateinit var viewModel: LibraryViewModel

    @Before
    fun setUp() {
        historyDao = FakeHistoryDao()
        watchLaterDao = FakeWatchLaterDao()
        playlistDao = FakePlaylistDao()
        viewModel = LibraryViewModel(
            historyRepository = HistoryRepository(historyDao, mainDispatcherRule.testDispatcher),
            watchLaterRepository = WatchLaterRepository(watchLaterDao, mainDispatcherRule.testDispatcher),
            playlistRepository = PlaylistRepository(
                playlistDao = playlistDao,
                extractorThrottler = ExtractorThrottler(),
                ioDispatcher = mainDispatcherRule.testDispatcher
            )
        )
    }

    @Test
    fun emptySources_emitEmptyState() = runTest {
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.history.isEmpty())
        assertTrue(state.watchLater.isEmpty())
        assertTrue(state.playlists.isEmpty())
    }

    @Test
    fun combine_mergesHistoryWatchLaterAndPlaylists() = runTest {
        historyDao.itemsFlow.value = listOf(
            HistoryEntity("v1", "T1", "c1", "C1", null, 1000L, 500L, 1L, false)
        )
        watchLaterDao.itemsFlow.value = listOf(
            WatchLaterEntity("v2", "T2", "c2", "C2", null, 0L, 2L)
        )
        playlistDao.playlistsFlow.value = listOf(
            PlaylistEntity(id = 1L, name = "My List")
        )

        val state = viewModel.uiState.first { !it.isLoading && it.history.isNotEmpty() }
        assertEquals(1, state.history.size)
        assertEquals("v1", state.history.first().videoId)
        assertEquals(1, state.watchLater.size)
        assertEquals("v2", state.watchLater.first().videoId)
        assertEquals(1, state.playlists.size)
        assertEquals("My List", state.playlists.first().name)
        assertFalse(state.isLoading)
    }

    @Test
    fun clearHistory_emptiesHistoryOnly() = runTest {
        historyDao.itemsFlow.value = listOf(
            HistoryEntity("v1", "T1", "c1", "C1", null, 1000L, 500L, 1L, false)
        )
        viewModel.uiState.first { !it.isLoading && it.history.isNotEmpty() }

        viewModel.clearHistory()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.first { !it.isLoading && it.history.isEmpty() }
        assertTrue(state.history.isEmpty())
    }
}
