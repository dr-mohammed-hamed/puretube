package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * قائمة تشغيل محلية (حاوية فقط، بلا روابط مؤقتة).
 * الأزمنة كلها Long مللي ثانية.
 */
@Entity(tableName = "playlists", indices = [Index(value = ["createdAtMs"])])
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
)
