package com.dr.tech.puretube.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Authoritative Data Access Object for playback history and resume timestamps (Constitution Principle VIII).
 * All positions and timestamps are Long milliseconds.
 */
@Dao
interface HistoryDao {

    @Query("SELECT * FROM playback_history ORDER BY lastPlayedAtMs DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 100): Flow<List<HistoryEntity>>

    @Query("SELECT lastPlaybackPositionMs FROM playback_history WHERE videoId = :videoId")
    suspend fun getPlaybackPosition(videoId: String): Long?

    @Query("SELECT * FROM playback_history WHERE videoId = :videoId")
    suspend fun getById(videoId: String): HistoryEntity?

    @Upsert
    suspend fun upsert(entity: HistoryEntity)

    @Query("UPDATE playback_history SET lastPlaybackPositionMs = :positionMs, lastPlayedAtMs = :timestampMs, isCompleted = 0 WHERE videoId = :videoId")
    suspend fun updatePlaybackPosition(videoId: String, positionMs: Long, timestampMs: Long = System.currentTimeMillis()): Int

    @Query("UPDATE playback_history SET isCompleted = 1, lastPlaybackPositionMs = 0 WHERE videoId = :videoId")
    suspend fun markCompleted(videoId: String): Int

    @Query("DELETE FROM playback_history WHERE videoId = :videoId")
    suspend fun deleteById(videoId: String): Int

    @Query("DELETE FROM playback_history")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getCount(): Int
}
