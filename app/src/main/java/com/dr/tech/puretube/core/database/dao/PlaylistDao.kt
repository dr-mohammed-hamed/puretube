package com.dr.tech.puretube.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

/** وصول تفاعلي لقوائم التشغيل وعناصرها (ترتيب عبر position). */
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAtMs DESC")
    fun getPlaylistsFlow(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    fun getItemsFlow(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int

    @Query("SELECT COUNT(*) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun getItemCount(playlistId: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId)")
    suspend fun isInPlaylist(playlistId: Long, videoId: String): Boolean

    @Query("SELECT playlistId FROM playlist_items WHERE videoId = :videoId")
    suspend fun getPlaylistIdsContainingVideo(videoId: String): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(entity: PlaylistEntity): Long

    /** إدخال يتجاهل المكرر (فهرس فريد playlistId+videoId). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItem(entity: PlaylistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(entities: List<PlaylistItemEntity>): List<Long>

    /** إنشاء قائمة مع عناصرها ذرياً (distinctBy videoId ضد التكرار). */
    @Transaction
    suspend fun insertPlaylistWithItems(
        playlist: PlaylistEntity,
        items: List<PlaylistItemEntity>
    ): Long {
        val playlistId = insertPlaylist(playlist)
        val distinct = items.distinctBy { it.videoId }
            .mapIndexed { index, item -> item.copy(id = 0L, playlistId = playlistId, position = index) }
        if (distinct.isNotEmpty()) insertItems(distinct)
        touchPlaylist(playlistId)
        return playlistId
    }

    /** إضافة عنصر واحد (يتجاهل المكرر، يُرجع -1 عند التكرار). */
    @Transaction
    suspend fun addItem(item: PlaylistItemEntity): Long {
        val rowId = insertItem(item)
        if (rowId != -1L) touchPlaylist(item.playlistId)
        return rowId
    }

    /** إضافة عنصر في النهاية ذرياً (getItemCount + insert في معاملة واحدة). */
    @Transaction
    suspend fun addItemAtEnd(item: PlaylistItemEntity): Long {
        val position = getItemCount(item.playlistId)
        val rowId = insertItem(item.copy(position = position))
        if (rowId != -1L) touchPlaylist(item.playlistId)
        return rowId
    }

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND videoId = :videoId")
    suspend fun removeItem(playlistId: Long, videoId: String): Int

    /** حذف عنصر مع تحديث updatedAtMs ذرياً. */
    @Transaction
    suspend fun removeItemAndTouch(playlistId: Long, videoId: String): Int {
        val removed = removeItem(playlistId, videoId)
        if (removed > 0) touchPlaylist(playlistId)
        return removed
    }

    /** حذف القائمة؛ العناصر تُحذف عبر CASCADE. */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long): Int

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long): Int

    /** إفراغ القائمة مع تحديث updatedAtMs ذرياً. */
    @Transaction
    suspend fun clearPlaylistAndTouch(playlistId: Long): Int {
        val removed = clearPlaylist(playlistId)
        if (removed > 0) touchPlaylist(playlistId)
        return removed
    }

    @Query("UPDATE playlist_items SET position = :position WHERE id = :itemId")
    suspend fun updatePosition(itemId: Long, position: Int): Int

    /** إعادة ترتيب دفعة واحدة ذرياً. */
    @Transaction
    suspend fun updatePositions(orderedItemIds: List<Long>) {
        orderedItemIds.forEachIndexed { index, itemId -> updatePosition(itemId, index) }
    }

    @Query("UPDATE playlists SET name = :name, updatedAtMs = :timestampMs WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String, timestampMs: Long = System.currentTimeMillis()): Int

    @Query("UPDATE playlists SET updatedAtMs = :timestampMs WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, timestampMs: Long = System.currentTimeMillis()): Int
}
