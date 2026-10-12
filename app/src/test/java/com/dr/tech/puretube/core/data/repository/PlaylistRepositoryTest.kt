package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.database.dao.PlaylistDao
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Unit tests for PlaylistRepository with an in-memory fake DAO.
 * Covers CRUD, duplicate guards, distinctBy import, and stream mapping.
 */
class PlaylistRepositoryTest {

    private class FakePlaylistDao : PlaylistDao {
        private var nextPlaylistId = 0L
        private var nextItemId = 0L
        val playlists = mutableListOf<PlaylistEntity>()
        val items = mutableListOf<PlaylistItemEntity>()

        override fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> = flow {
            emit(playlists.sortedByDescending { it.updatedAtMs })
        }

        override fun getItemsFlow(playlistId: Long): Flow<List<PlaylistItemEntity>> = flow {
            emit(items.filter { it.playlistId == playlistId }.sortedBy { it.position })
        }

        override suspend fun getPlaylistCount(): Int = playlists.size

        override suspend fun getItemCount(playlistId: Long): Int =
            items.count { it.playlistId == playlistId }

        override suspend fun isInPlaylist(playlistId: Long, videoId: String): Boolean =
            items.any { it.playlistId == playlistId && it.videoId == videoId }

        override suspend fun getPlaylistIdsContainingVideo(videoId: String): List<Long> =
            items.filter { it.videoId == videoId }.map { it.playlistId }.distinct()

        override suspend fun insertPlaylist(entity: PlaylistEntity): Long {
            val id = ++nextPlaylistId
            playlists.add(entity.copy(id = id))
            return id
        }

        override suspend fun insertItem(entity: PlaylistItemEntity): Long {
            if (items.any { it.playlistId == entity.playlistId && it.videoId == entity.videoId }) {
                return -1L
            }
            val id = ++nextItemId
            items.add(entity.copy(id = id))
            return id
        }

        override suspend fun insertItems(entities: List<PlaylistItemEntity>): List<Long> =
            entities.map { insertItem(it) }

        override suspend fun removeItem(playlistId: Long, videoId: String): Int {
            val before = items.size
            items.removeIf { it.playlistId == playlistId && it.videoId == videoId }
            return before - items.size
        }

        override suspend fun deletePlaylist(playlistId: Long): Int {
            val removed = playlists.removeIf { it.id == playlistId }
            items.removeIf { it.playlistId == playlistId }
            return if (removed) 1 else 0
        }

        override suspend fun clearPlaylist(playlistId: Long): Int {
            val before = items.size
            items.removeIf { it.playlistId == playlistId }
            return before - items.size
        }

        override suspend fun updatePosition(itemId: Long, position: Int): Int {
            val index = items.indexOfFirst { it.id == itemId }
            if (index == -1) return 0
            items[index] = items[index].copy(position = position)
            return 1
        }

        override suspend fun renamePlaylist(playlistId: Long, name: String, timestampMs: Long): Int {
            val index = playlists.indexOfFirst { it.id == playlistId }
            if (index == -1) return 0
            playlists[index] = playlists[index].copy(name = name, updatedAtMs = timestampMs)
            return 1
        }

        override suspend fun touchPlaylist(playlistId: Long, timestampMs: Long): Int {
            val index = playlists.indexOfFirst { it.id == playlistId }
            if (index == -1) return 0
            playlists[index] = playlists[index].copy(updatedAtMs = timestampMs)
            return 1
        }
    }

    private lateinit var dao: FakePlaylistDao
    private lateinit var repository: PlaylistRepository

    @Before
    fun setUp() {
        dao = FakePlaylistDao()
        repository = PlaylistRepository(
            playlistDao = dao,
            extractorThrottler = ExtractorThrottler(maxConcurrentCalls = 4)
        )
    }

    @Test
    fun createRenameDelete_roundtrip() = runTest {
        val id = repository.createPlaylist("دروس").getOrThrow()
        assertTrue(id > 0L)

        repository.renamePlaylist(id, "دروس التفسير").getOrThrow()
        val playlists = repository.getPlaylistsFlow().first()
        assertEquals("دروس التفسير", playlists.first { it.id == id }.name)

        repository.deletePlaylist(id).getOrThrow()
        assertTrue(repository.getPlaylistsFlow().first().none { it.id == id })
    }

    @Test
    fun create_blankName_returnsFailure() = runTest {
        assertTrue(repository.createPlaylist("   ").isFailure)
    }

    @Test
    fun addRemove_isInReflectsMembership() = runTest {
        val playlistId = repository.createPlaylist("قائمتي").getOrThrow()
        val video = FeedVideoItem(
            videoId = "vid1",
            title = "عنوان",
            channelId = "UC1",
            channelName = "قناة",
            durationMs = 60_000L
        )

        repository.addToPlaylist(playlistId, video).getOrThrow()
        assertTrue(repository.isInPlaylist(playlistId, "vid1"))

        val stored = repository.getItemsFlow(playlistId).first()
        assertEquals(1, stored.size)
        assertEquals("vid1", stored.first().videoId)
        assertEquals(playlistId, stored.first().playlistId)

        repository.removeFromPlaylist(playlistId, "vid1").getOrThrow()
        assertFalse(repository.isInPlaylist(playlistId, "vid1"))
    }

    @Test
    fun addDuplicate_returnsFailure() = runTest {
        val playlistId = repository.createPlaylist("قائمتي").getOrThrow()
        val video = FeedVideoItem("vid1", "عنوان", "UC1", "قناة")
        repository.addToPlaylist(playlistId, video).getOrThrow()
        assertTrue(repository.addToPlaylist(playlistId, video).isFailure)
        assertEquals(1, repository.getItemsFlow(playlistId).first().size)
    }

    @Test
    fun import_dedupesByVideoIdAndAssignsPositions() = runTest {
        val fakeItems = listOf(
            PlaylistItemEntity(playlistId = 0L, videoId = "a", title = "أ", channelId = "c", channelName = "ق"),
            PlaylistItemEntity(playlistId = 0L, videoId = "b", title = "ب", channelId = "c", channelName = "ق"),
            PlaylistItemEntity(playlistId = 0L, videoId = "a", title = "أ مكرر", channelId = "c", channelName = "ق")
        )
        val fakeRepo = PlaylistRepository(
            playlistDao = dao,
            extractorThrottler = ExtractorThrottler(maxConcurrentCalls = 4),
            customPlaylistFetcher = { "مستوردة" to fakeItems }
        )

        val playlistId = fakeRepo.importFromYoutubeUrl("https://www.youtube.com/playlist?list=PL123").getOrThrow()
        val stored = fakeRepo.getItemsFlow(playlistId).first()
        assertEquals(2, stored.size)
        assertEquals(listOf("a", "b"), stored.map { it.videoId })
        assertEquals(listOf(0, 1), stored.map { it.position })
        assertTrue(stored.all { it.playlistId == playlistId })
    }

    @Test
    fun import_blankUrl_returnsFailure() = runTest {
        assertTrue(repository.importFromYoutubeUrl("  ").isFailure)
    }

    @Test
    fun mapPlaylistStreamItem_mapsFields() {
        val item = StreamInfoItem(
            0,
            "https://www.youtube.com/watch?v=px9",
            "حلقة",
            StreamType.VIDEO_STREAM
        ).apply {
            setUploaderName("قناة")
            setUploaderUrl("https://www.youtube.com/channel/UC9")
            setDuration(120L)
        }
        val mapped = mapPlaylistStreamItem(item, 3)
        checkNotNull(mapped)
        assertEquals("px9", mapped.videoId)
        assertEquals("حلقة", mapped.title)
        assertEquals("UC9", mapped.channelId)
        assertEquals(120_000L, mapped.durationMs)
        assertEquals(3, mapped.position)
    }

    @Test
    fun getPlaylistIdsForVideo_returnsContainingPlaylists() = runTest {
        val p1 = repository.createPlaylist("قائمة 1").getOrThrow()
        val p2 = repository.createPlaylist("قائمة 2").getOrThrow()
        val p3 = repository.createPlaylist("قائمة 3").getOrThrow()

        repository.addToPlaylist(
            playlistId = p1,
            videoId = "vid_123",
            title = "عنوان",
            channelId = "ch_1",
            channelName = "قناة"
        ).getOrThrow()
        repository.addToPlaylist(
            playlistId = p3,
            videoId = "vid_123",
            title = "عنوان",
            channelId = "ch_1",
            channelName = "قناة"
        ).getOrThrow()

        val containing = repository.getPlaylistIdsForVideo("vid_123")
        assertEquals(setOf(p1, p3), containing)
        assertFalse(containing.contains(p2))
    }
}

