package com.dr.tech.puretube.core.data.model

import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity

/**
 * عرض قائمة تشغيل مع بيانات وصفية للشاشات (عدد العناصر + مصغرة).
 * بلا أي روابط مؤقتة — المبدأ VIII.
 */
data class Playlist(
    val id: Long,
    val name: String,
    val itemCount: Int = 0,
    val thumbnailUrl: String? = null,
    val createdAtMs: Long = 0L
)

/** يحوّل كيان القائمة إلى عرض مع حقن العدد والمصغرة من الاستعلامات. */
fun PlaylistEntity.toPlaylist(
    itemCount: Int = 0,
    thumbnailUrl: String? = null
): Playlist = Playlist(
    id = id,
    name = name,
    itemCount = itemCount,
    thumbnailUrl = thumbnailUrl,
    createdAtMs = createdAtMs
)

/** يحوّل عنصر القائمة إلى نموذج فيديو قابل للعرض/التشغيل. */
fun PlaylistItemEntity.toFeedVideoItem(): FeedVideoItem = FeedVideoItem(
    videoId = videoId,
    title = title,
    channelId = channelId,
    channelName = channelName,
    thumbnailUrl = thumbnailUrl,
    durationMs = durationMs
)
