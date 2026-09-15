package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.HistoryDao
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing video playback history and resume positions.
 * Conforms to Constitution Principles III, IV, and VIII:
 * Stored strictly on device, Long millisecond accuracy, zero ephemeral stream tokens persisted.
 */
class HistoryRepository(
    private val historyDao: HistoryDao,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Emits the reactive stream of recently played videos up to the given limit.
     */
    fun getHistoryFlow(limit: Int = 100): Flow<List<HistoryEntity>> = historyDao.getRecentFlow(limit)

    /**
     * Retrieves the full history entity for a video.
     * Single authoritative row read for resume truth (Constitution VIII) — all
     * per-video state derivations (e.g. [getPlaybackPosition]) funnel through here
     * instead of issuing parallel queries for the same row.
     */
    suspend fun getHistoryItem(videoId: String): HistoryEntity? = withContext(ioDispatcher) {
        historyDao.getById(videoId)
    }

    /**
     * Retrieves the last playback position in milliseconds for exact resume.
     * Derived from the single authoritative row read ([getHistoryItem]).
     * Returns null or 0L if no prior record exists.
     */
    suspend fun getPlaybackPosition(videoId: String): Long? = withContext(ioDispatcher) {
        getHistoryItem(videoId)?.lastPlaybackPositionMs
    }

    /**
     * Records or updates playback start for a video.
     */
    suspend fun recordPlayback(entity: HistoryEntity) = withContext(ioDispatcher) {
        historyDao.upsert(entity)
    }

    /**
     * Updates playback progress timestamp in milliseconds.
     */
    suspend fun updatePlaybackPosition(
        videoId: String,
        positionMs: Long,
        timestampMs: Long = System.currentTimeMillis()
    ) = withContext(ioDispatcher) {
        historyDao.updatePlaybackPosition(videoId, positionMs, timestampMs)
    }

    /**
     * Marks video playback as completed and resets playback position.
     */
    suspend fun markCompleted(videoId: String) = withContext(ioDispatcher) {
        historyDao.markCompleted(videoId)
    }

    /**
     * Deletes a single history entry by video ID.
     */
    suspend fun deleteFromHistory(videoId: String) = withContext(ioDispatcher) {
        historyDao.deleteById(videoId)
    }

    /**
     * Clears all playback history.
     */
    suspend fun clearAllHistory() = withContext(ioDispatcher) {
        historyDao.clearAll()
    }

    /**
     * Returns total count of items in history.
     */
    suspend fun getHistoryCount(): Int = withContext(ioDispatcher) {
        historyDao.getCount()
    }
}
