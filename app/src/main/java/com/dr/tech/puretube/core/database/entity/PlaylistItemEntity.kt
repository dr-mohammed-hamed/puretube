package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * عنصر فيديو داخل قائمة (يُحذف تلقائياً مع القائمة CASCADE).
 * بلا روابط مؤقتة؛ الأزمنة والمدد Long مللي ثانية.
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["videoId"]),
        Index(value = ["playlistId", "videoId"], unique = true)
    ]
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val playlistId: Long,
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val addedAtMs: Long = System.currentTimeMillis(),
    val position: Int = 0
)
