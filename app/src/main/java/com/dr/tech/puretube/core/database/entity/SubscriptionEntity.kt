package com.dr.tech.puretube.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Local Room entity representing an intentionally subscribed YouTube channel.
 * Conforms to Constitution Principles I, III, and VIII (Single Source of Truth, local sovereignty).
 */
@Entity(tableName = "subscriptions", indices = [Index(value = ["subscribedAtMs"])])
data class SubscriptionEntity(
    @PrimaryKey
    val channelId: String,
    val channelName: String,
    val channelHandle: String? = null,
    val avatarUrl: String? = null,
    val subscriberCountText: String? = null,
    val isNotificationsEnabled: Boolean = true,
    val subscribedAtMs: Long = System.currentTimeMillis()
)
