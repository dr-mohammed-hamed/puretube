package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity representing playback history and resume position.
 * Timestamps, durations, and seek offsets are stored strictly in Long milliseconds
 * to eliminate fractional drift (Constitution Principle IV).
 * Zero ephemeral streams or DASH tokens are stored (Constitution Principle VIII).
 */
@Entity(tableName = "playback_history", indices = [Index(value = ["lastPlayedAtMs"])])
data class HistoryEntity(
    @PrimaryKey
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val lastPlaybackPositionMs: Long = 0L,
    val lastPlayedAtMs: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
