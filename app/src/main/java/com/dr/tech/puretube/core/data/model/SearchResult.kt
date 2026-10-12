package com.dr.tech.puretube.core.data.model

import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeSearchQueryHandlerFactory

/**
 * نتيجة بحث عام (فيديو أو قناة).
 * القيم النصية للفلاتر مطابقة لثوابت NewPipe الموثقة:
 * ALL="all" (param 8AEB), VIDEOS="videos" (param EgIQAfABAQ==),
 * CHANNELS="channels" (param EgIQAvABAQ==) — تم التحقق من YoutubeSearchQueryHandlerFactory v0.26.5.
 */
sealed interface SearchResultItem {

    data class VideoItem(
        val videoId: String,
        val title: String,
        val channelId: String,
        val channelName: String,
        val thumbnailUrl: String? = null,
        val durationMs: Long = 0L,
        val viewCount: Long = 0L,
        val uploadDateText: String? = null
    ) : SearchResultItem

    data class ChannelItem(
        val channelId: String,
        val channelName: String,
        val handle: String? = null,
        val avatarUrl: String? = null,
        val subscriberCountText: String? = null
    ) : SearchResultItem
}

/**
 * فلتر البحث العام مع ربط NewPipe content-filter.
 * أي قيمة غير معروفة تسقط إلى ALL.
 */
enum class SearchFilter {
    ALL,
    VIDEOS,
    CHANNELS;

    fun toContentFilter(): List<String> = listOf(
        when (this) {
            ALL -> YoutubeSearchQueryHandlerFactory.ALL
            VIDEOS -> YoutubeSearchQueryHandlerFactory.VIDEOS
            CHANNELS -> YoutubeSearchQueryHandlerFactory.CHANNELS
        }
    )

    companion object {
        fun fromString(value: String?): SearchFilter = when (value?.trim()?.lowercase()) {
            "videos", "video" -> VIDEOS
            "channels", "channel" -> CHANNELS
            else -> ALL
        }
    }
}
