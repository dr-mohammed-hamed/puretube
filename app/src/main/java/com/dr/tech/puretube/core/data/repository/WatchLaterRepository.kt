package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repository for managing intentional Watch Later bookmarks.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 */
class WatchLaterRepository(
    private val watchLaterDao: WatchLaterDao,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {

    private val toggleMutex = Mutex()

    /**
     * Emits the reactive list of saved Watch Later videos, newest additions first.
     * List observation only — NEVER derive bookmark status by scanning this list;
     * status checks MUST go through [isInWatchLater]/[isInWatchLaterOnce] (Constitution VIII).
     */
    fun getWatchLaterFlow(): Flow<List<WatchLaterEntity>> = watchLaterDao.getAllFlow()

    /**
     * Observes whether a specific video is currently in the Watch Later list.
     * Sole reactive source of bookmark truth (single Room EXISTS query, Constitution VIII).
     */
    fun isInWatchLater(videoId: String): Flow<Boolean> = watchLaterDao.isInWatchLater(videoId)

    /**
     * One-shot bookmark check against the same single Room EXISTS query backing
     * [isInWatchLater]. Authoritative decision source for toggles — callers MUST NOT
     * decide add/remove from in-memory mirrors (e.g. a locally cached id set).
     */
    suspend fun isInWatchLaterOnce(videoId: String): Boolean = withContext(ioDispatcher) {
        watchLaterDao.isInWatchLater(videoId).first()
    }

    /**
     * Atomically toggles bookmark state from Room truth (no in-memory snapshot input).
     * Serialized via [toggleMutex] so concurrent toggles cannot interleave; the
     * add/remove decision is always read from Room truth.
     * Returns [Result] with the new saved state (true = now saved).
     */
    suspend fun toggleWatchLater(entity: WatchLaterEntity): Result<Boolean> =
        withContext(ioDispatcher) {
            toggleMutex.withLock {
                runCatching {
                    if (watchLaterDao.isInWatchLater(entity.videoId).first()) {
                        watchLaterDao.deleteById(entity.videoId)
                        false
                    } else {
                        watchLaterDao.insert(entity)
                        true
                    }
                }
            }
        }

    /**
     * Adds or updates a video in Watch Later.
     */
    suspend fun addToWatchLater(entity: WatchLaterEntity) = withContext(ioDispatcher) {
        watchLaterDao.insert(entity)
    }

    /**
     * Removes a video from Watch Later by its YouTube videoId.
     */
    suspend fun removeFromWatchLater(videoId: String) = withContext(ioDispatcher) {
        watchLaterDao.deleteById(videoId)
    }

    /**
     * Clears all videos from Watch Later.
     */
    suspend fun clearAll() = withContext(ioDispatcher) {
        watchLaterDao.clearAll()
    }

    /**
     * Returns total count of items currently in Watch Later.
     */
    suspend fun getWatchLaterCount(): Int = withContext(ioDispatcher) {
        watchLaterDao.getCount()
    }
}
