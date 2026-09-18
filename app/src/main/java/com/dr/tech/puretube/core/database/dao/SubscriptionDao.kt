package com.dr.tech.puretube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Authoritative Data Access Object for channel subscriptions (Constitution Principle VIII).
 * All queries run reactively via Flow or asynchronously via suspend functions.
 */
@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAtMs DESC")
    fun getAllFlow(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY subscribedAtMs DESC")
    suspend fun getAll(): List<SubscriptionEntity>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM subscriptions 
            WHERE channelId = :channelId 
               OR channelHandle = :channelId 
               OR channelHandle = ('@' || :channelId) 
               OR ('@' || channelHandle) = :channelId
               OR (:handle IS NOT NULL AND (
                   channelId = :handle 
                   OR channelHandle = :handle 
                   OR channelHandle = ('@' || :handle) 
                   OR ('@' || channelHandle) = :handle
               ))
        )
    """)
    fun isSubscribed(channelId: String, handle: String? = null): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SubscriptionEntity>)

    @Query("""
        DELETE FROM subscriptions 
        WHERE channelId = :channelId 
           OR channelHandle = :channelId 
           OR channelHandle = ('@' || :channelId) 
           OR ('@' || channelHandle) = :channelId
           OR (:handle IS NOT NULL AND (
               channelId = :handle 
               OR channelHandle = :handle 
               OR channelHandle = ('@' || :handle) 
               OR ('@' || channelHandle) = :handle
           ))
    """)
    suspend fun deleteById(channelId: String, handle: String? = null): Int

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun getCount(): Int
}
