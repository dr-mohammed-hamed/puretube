package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity representing a video intentionally saved to Watch Later.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 * Ephemeral streaming URLs are strictly omitted (Principle VIII).
 */
@Entity(tableName = "watch_later", indices = [Index(value = ["addedAtMs"])])
data class WatchLaterEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val addedAtMs: Long = System.currentTimeMillis()
)
