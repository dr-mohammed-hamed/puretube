package com.dr.tech.puretube.core.data.model

/**
 * Represents an extracted video stream track (DASH or progressive).
 * Follows Constitution Principle VIII: Ephemeral streams held strictly in memory.
 */
data class VideoStreamFormat(
    val url: String,
    val resolution: String,
    val format: String,
    val width: Int,
    val height: Int,
    val isVideoOnly: Boolean = true
)

/**
 * Represents an extracted audio stream track.
 */
data class AudioStreamFormat(
    val url: String,
    val bitrate: Int,
    val format: String,
    val averageBitrate: Int = bitrate
)

/**
 * Supported user-selectable playback resolutions.
 */
enum class PlaybackQuality(val label: String, val targetHeight: Int) {
    AUTO("تلقائي", 0),
    HD_1080("1080p", 1080),
    HD_720("720p", 720),
    SD_480("480p", 480),
    SD_360("360p", 360)
}

/**
 * Full resolved metadata and media stream URLs for player screen consumption.
 * Duration is always Long milliseconds (Constitution Principle IV).
 */
data class ExtractedVideoDetails(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val channelAvatarUrl: String? = null,
    val thumbnailUrl: String? = null,
    val subscriberCount: Long? = null,
    val durationMs: Long = 0L,
    val viewCount: Long = 0L,
    val uploadDateText: String? = null,
    val description: String? = null,
    val videoStreams: List<VideoStreamFormat> = emptyList(),
    val audioStreams: List<AudioStreamFormat> = emptyList()
)
