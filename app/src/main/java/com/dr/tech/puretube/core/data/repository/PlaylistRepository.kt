package com.dr.tech.puretube.core.data.repository

import androidx.annotation.VisibleForTesting
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.database.dao.PlaylistDao
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * مستودع قوائم التشغيل المحلية + استيراد قوائم يوتيوب عبر رابط.
 * كل الكتابة على IO dispatcher؛ الاستيراد بالجملة ذري عبر insertPlaylistWithItems.
 */
class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val extractorThrottler: ExtractorThrottler,
    private val customPlaylistFetcher: (suspend (url: String) -> Pair<String, List<PlaylistItemEntity>>)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getPlaylistsFlow(): Flow<List<PlaylistEntity>> = playlistDao.getPlaylistsFlow()

    fun getItemsFlow(playlistId: Long): Flow<List<PlaylistItemEntity>> =
        playlistDao.getItemsFlow(playlistId)

    suspend fun createPlaylist(name: String): Result<Long> = withContext(ioDispatcher) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("اسم القائمة فارغ"))
        }
        runCatching {
            playlistDao.insertPlaylist(PlaylistEntity(name = trimmed))
        }
    }

    suspend fun renamePlaylist(playlistId: Long, name: String): Result<Unit> = withContext(ioDispatcher) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("اسم القائمة فارغ"))
        }
        runCatching {
            playlistDao.renamePlaylist(playlistId, trimmed)
            Unit
        }
    }

    suspend fun deletePlaylist(playlistId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            playlistDao.deletePlaylist(playlistId)
            Unit
        }
    }

    suspend fun addToPlaylist(playlistId: Long, video: FeedVideoItem): Result<Long> =
        addToPlaylist(
            playlistId = playlistId,
            videoId = video.videoId,
            title = video.title,
            channelId = video.channelId,
            channelName = video.channelName,
            thumbnailUrl = video.thumbnailUrl,
            durationMs = video.durationMs
        )

    suspend fun addToPlaylist(
        playlistId: Long,
        videoId: String,
        title: String,
        channelId: String,
        channelName: String,
        thumbnailUrl: String? = null,
        durationMs: Long = 0L
    ): Result<Long> = withContext(ioDispatcher) {
        if (videoId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("معرّف الفيديو فارغ"))
        }
        runCatching {
            val rowId = playlistDao.addItemAtEnd(
                PlaylistItemEntity(
                    playlistId = playlistId,
                    videoId = videoId.trim(),
                    title = title.ifBlank { videoId },
                    channelId = channelId,
                    channelName = channelName,
                    thumbnailUrl = thumbnailUrl,
                    durationMs = durationMs,
                    position = 0
                )
            )
            if (rowId == -1L) throw IllegalStateException("الفيديو موجود في القائمة مسبقاً")
            rowId
        }
    }

    suspend fun removeFromPlaylist(playlistId: Long, videoId: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                playlistDao.removeItemAndTouch(playlistId, videoId.trim())
                Unit
            }
        }

    suspend fun isInPlaylist(playlistId: Long, videoId: String): Boolean =
        withContext(ioDispatcher) {
            if (videoId.isBlank()) return@withContext false
            playlistDao.isInPlaylist(playlistId, videoId.trim())
        }

    suspend fun getPlaylistIdsForVideo(videoId: String): Set<Long> =
        withContext(ioDispatcher) {
            if (videoId.isBlank()) return@withContext emptySet()
            runCatching {
                playlistDao.getPlaylistIdsContainingVideo(videoId.trim()).toSet()
            }.getOrDefault(emptySet())
        }

    suspend fun clearPlaylist(playlistId: Long): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            playlistDao.clearPlaylistAndTouch(playlistId)
            Unit
        }
    }

    /**
     * يستورد قائمة يوتيوب عبر رابطها (اسم + عناصر مميزة بـ videoId) ذرياً.
     * يُرجع معرّف القائمة المحلية الجديدة.
     */
    suspend fun importFromYoutubeUrl(url: String): Result<Long> = withContext(ioDispatcher) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("رابط القائمة فارغ"))
        }
        if (customPlaylistFetcher != null) {
            return@withContext runCatching {
                val (name, items) = customPlaylistFetcher.invoke(trimmed)
                playlistDao.insertPlaylistWithItems(
                    PlaylistEntity(name = name.ifBlank { trimmed }),
                    items.distinctBy { it.videoId }
                )
            }
        }
        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val extractor = service.getPlaylistExtractor(trimmed)
            extractor.fetchPage()
            val name = extractor.name?.ifBlank { null } ?: trimmed
            val initialPage = extractor.initialPage
                ?: throw ExtractionException("تعذر استخراج عناصر القائمة")
            val items = (initialPage.items ?: emptyList())
                .mapIndexedNotNull { index, item -> mapPlaylistStreamItem(item, index) }
                .distinctBy { it.videoId }
            playlistDao.insertPlaylistWithItems(PlaylistEntity(name = name), items)
        }
    }
}

@VisibleForTesting
internal fun mapPlaylistStreamItem(item: StreamInfoItem, position: Int): PlaylistItemEntity? {
    val rawUrl = item.url ?: return null
    val videoId = extractSearchVideoId(rawUrl)
    if (videoId.isBlank() || videoId.contains("/")) return null
    val durationMs = if (item.duration > 0L) item.duration * 1000L else 0L
    return PlaylistItemEntity(
        playlistId = 0L,
        videoId = videoId,
        title = item.name ?: videoId,
        channelId = item.uploaderUrl?.substringAfterLast("/")?.substringBefore("?")?.trim().orEmpty(),
        channelName = item.uploaderName ?: "",
        thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
        durationMs = durationMs,
        position = position
    )
}
