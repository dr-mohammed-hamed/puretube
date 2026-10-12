package com.dr.tech.puretube.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dr.tech.puretube.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

/** سجل البحث المحلي (الأحدث أولاً، بحد يُمرر من المستدعي). */
@Dao
interface SearchHistoryDao {

    @Query("SELECT * FROM search_history ORDER BY searchedAtMs DESC LIMIT :limit")
    fun getRecentFlow(limit: Int = 30): Flow<List<SearchHistoryEntity>>

    @Upsert
    suspend fun upsert(entity: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun delete(query: String): Int

    @Query("DELETE FROM search_history")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getCount(): Int
}
