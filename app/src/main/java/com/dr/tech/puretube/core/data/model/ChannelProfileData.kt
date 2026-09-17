package com.dr.tech.puretube.core.data.model

/**
 * Domain model representing comprehensive channel profile details.
 * Conforms to Constitution Principles I, V, and VII (Cairo typography, clean domain contract).
 */
data class ChannelProfileDetails(
    val channelId: String,
    val channelName: String,
    val channelHandle: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val subscriberCount: Long = 0L,
    val subscriberCountText: String? = null,
    val videoCount: Long = 0L,
    val description: String? = null,
    val joinedDateText: String? = null,
    val viewCount: Long = 0L,
    val isVerified: Boolean = false
)

/**
 * Domain model representing a channel's playlist item.
 */
data class ChannelPlaylistItem(
    val playlistId: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val streamCount: Long = 0L,
    val uploaderName: String? = null
)

/**
 * Tab types supported on the Channel Screen, matching official YouTube.
 */
enum class ChannelTabType {
    VIDEOS,
    PLAYLISTS,
    LIVE,
    ABOUT
}

/**
 * Sorting orders supported on the Videos tab.
 */
enum class ChannelSortOrder {
    LATEST,
    POPULAR,
    OLDEST
}
