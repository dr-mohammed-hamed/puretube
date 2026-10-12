package com.dr.tech.puretube.features.playlists

import com.dr.tech.puretube.core.data.repository.PlaylistRepository
import com.dr.tech.puretube.core.database.dao.PlaylistDao
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.features.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlaylistViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

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

        override suspend fun renamePlaylist(playlistId: Long, name: String, timestampMs: Long): Int {
            playlistsFlow.value = playlistsFlow.value.map {
                if (it.id == playlistId) it.copy(name = name) else it
            }
            return 1
        }

        override suspend fun touchPlaylist(playlistId: Long, timestampMs: Long): Int = 1
    }

    private lateinit var fakeDao: FakePlaylistDao
    private lateinit var repository: PlaylistRepository
    private lateinit var viewModel: PlaylistViewModel

    @Before
    fun setUp() {
        fakeDao = FakePlaylistDao()
        repository = PlaylistRepository(
            playlistDao = fakeDao,
            extractorThrottler = ExtractorThrottler(),
            customPlaylistFetcher = { _ ->
                "Imported" to listOf(
                    PlaylistItemEntity(playlistId = 0L, videoId = "v1", title = "T1", channelId = "c1", channelName = "C1", position = 0),
                    PlaylistItemEntity(playlistId = 0L, videoId = "v1", title = "T1-dup", channelId = "c1", channelName = "C1", position = 1),
                    PlaylistItemEntity(playlistId = 0L, videoId = "v2", title = "T2", channelId = "c2", channelName = "C2", position = 2)
                )
            },
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
        viewModel = PlaylistViewModel(repository)
    }

    @Test
    fun createPlaylist_emitsNewId() = runTest {
        var createdId = -1L
        viewModel.createPlaylist("My List") { createdId = it }
        testScheduler.advanceUntilIdle()

        assertTrue(createdId > 0L)
        assertEquals(1, fakeDao.playlistsFlow.value.size)
        assertEquals("My List", fakeDao.playlistsFlow.value.first().name)
    }

    @Test
    fun addDuplicateVideo_isRejected() = runTest {
        val id = repository.createPlaylist("Dup List").getOrThrow()
        val first = repository.addToPlaylist(id, "v1", "T1", "c1", "C1")
        val second = repository.addToPlaylist(id, "v1", "T1", "c1", "C1")

        assertTrue(first.isSuccess)
        assertTrue(second.isFailure)
        assertEquals(1, fakeDao.itemsFlow.value.count { it.playlistId == id })
    }

    @Test
    fun importFromUrl_dedupesAndEmitsSuccess() = runTest {
        var importedId = -1L
        viewModel.importFromUrl("https://www.youtube.com/playlist?list=PL123") { importedId = it }
        testScheduler.advanceUntilIdle()

        assertTrue(importedId > 0L)
        assertEquals(importedId, viewModel.importState.value.successPlaylistId)
        val stored = fakeDao.itemsFlow.value.filter { it.playlistId == importedId }
        assertEquals(2, stored.size)
        assertEquals(listOf("v1", "v2"), stored.map { it.videoId })

        viewModel.consumeImportSuccess()
        assertNull(viewModel.importState.value.successPlaylistId)
    }

    @Test
    fun selectPlaylist_exposesDetailItems() = runTest {
        val id = repository.createPlaylist("Detail").getOrThrow()
        repository.addToPlaylist(id, "v9", "T9", "c9", "C9").getOrThrow()

        viewModel.selectPlaylist(id)
        val items = viewModel.items.first { it.isNotEmpty() }
        assertEquals(1, items.size)
        assertEquals("v9", items.first().videoId)
    }
}
