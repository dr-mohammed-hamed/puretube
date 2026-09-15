package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.WatchLaterDao
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing intentional Watch Later bookmarks.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 */
class WatchLaterRepository(
    private val watchLaterDao: WatchLaterDao
) {

    /**
     * Emits the reactive list of saved Watch Later videos, newest additions first.
     */
    fun getWatchLaterFlow(): Flow<List<WatchLaterEntity>> = watchLaterDao.getAllFlow()

    /**
     * Observes whether a specific video is currently in the Watch Later list.
     */
    fun isInWatchLater(videoId: String): Flow<Boolean> = watchLaterDao.isInWatchLater(videoId)

    /**
     * Adds or updates a video in Watch Later.
     */
    suspend fun addToWatchLater(entity: WatchLaterEntity) = withContext(Dispatchers.IO) {
        watchLaterDao.insert(entity)
    }

    /**
     * Removes a video from Watch Later by its YouTube videoId.
     */
    suspend fun removeFromWatchLater(videoId: String) = withContext(Dispatchers.IO) {
        watchLaterDao.deleteById(videoId)
    }

    /**
     * Returns total count of items currently in Watch Later.
     */
    suspend fun getWatchLaterCount(): Int = withContext(Dispatchers.IO) {
        watchLaterDao.getCount()
    }
}
