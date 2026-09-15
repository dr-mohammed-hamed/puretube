package com.dr.tech.puretube.core.data.model

/**
 * Domain model representing a video item in the aggregated subscriptions feed.
 * Conforms to Constitution Principles I, IV, and VIII:
 * Intentional feed only, Long millisecond precision, zero ephemeral DASH streaming URLs persisted.
 */
data class FeedVideoItem(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L,
    val uploadDateMs: Long = 0L,
    val uploadDateText: String? = null
)
