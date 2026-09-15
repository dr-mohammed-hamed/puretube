package com.dr.tech.puretube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Authoritative Data Access Object for Watch Later bookmarks (Constitution Principle VIII).
 */
@Dao
interface WatchLaterDao {

    @Query("SELECT * FROM watch_later ORDER BY addedAtMs DESC")
    fun getAllFlow(): Flow<List<WatchLaterEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watch_later WHERE videoId = :videoId)")
    fun isInWatchLater(videoId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchLaterEntity)

    @Query("DELETE FROM watch_later WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String): Int

    @Query("DELETE FROM watch_later")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM watch_later")
    suspend fun getCount(): Int
}
