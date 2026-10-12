package com.dr.tech.puretube.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.util.ArabicTimeFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared Video Card component for PureTube (نَقِيّ).
 * Conforms to Single Source of Truth Theme tokens, YouTube Edge-to-Edge aesthetics,
 * Zero-Emoji discipline, Cairo typography, Coil image loading with crossfade,
 * 16:9 full-width thumbnail, duration badge, and bookmark action.
 */
@Composable
fun PureVideoCard(
    video: FeedVideoItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean = false,
    watchedProgress: Float = 0f,
    onChannelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // 1. Thumbnail: 100% full width, 16:9 aspect ratio, edge-to-edge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(PureTheme.colors.surfaceElevated)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(video.thumbnailUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Duration badge: pinned at Alignment.BottomEnd with 4.dp rounded corners
            if (video.durationMs > 0L) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            bottom = if (watchedProgress > 0f) 8.dp else 6.dp,
                            end = 6.dp
                        )
                        .background(
                            color = PureTheme.colors.background.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = PureTheme.colors.textPrimary,
                        style = PureTheme.typography.labelSmall
                    )
                }
            }

            // Watched progress bar: 3dp high at Alignment.BottomStart
            if (watchedProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(PureTheme.colors.surfaceElevated.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(watchedProgress.coerceIn(0f, 1f))
                            .background(PureTheme.colors.primary)
                    )
                }
            }
        }

        // 2. Info Row below thumbnail
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Leading: Circular channel avatar (38dp diameter, CircleShape, background surfaceElevated)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(PureTheme.colors.surfaceElevated)
                    .then(
                        if (onChannelClick != null) {
                            Modifier.clickable { onChannelClick(video.channelId) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!video.channelAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(video.channelAvatarUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = video.channelName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = video.channelName.take(1),
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.primary
                    )
                }
            }

            // Middle: Video title and subtitle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            ) {
                Text(
                    text = video.title,
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = video.channelName,
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .then(
                                if (onChannelClick != null) {
                                    Modifier.clickable { onChannelClick(video.channelId) }
                                } else {
                                    Modifier
                                }
                            )
                    )

                    val viewsText = remember(video.viewCount) {
                        if (video.viewCount > 0L) formatViews(video.viewCount) else ""
                    }
                    if (viewsText.isNotBlank()) {
                        Text(
                            text = " • $viewsText",
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val dateText = remember(video.uploadDateMs, video.uploadDateText) {
                        ArabicTimeFormatter.formatRelativeTime(video.uploadDateMs, video.uploadDateText)
                            .takeIf { it.isNotBlank() }
                    }

                    if (dateText != null) {
                        Text(
                            text = " • $dateText",
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Trailing: Bookmark button
            IconButton(
                onClick = onBookmarkClick
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = if (isBookmarked) "إزالة من المشاهدة لاحقاً" else "حفظ في المشاهدة لاحقاً",
                    tint = if (isBookmarked) PureTheme.colors.secondary else PureTheme.colors.textSecondary
                )
            }
        }
    }
}

/**
 * Formats view count cleanly (e.g., 1.2M, 500K, or raw count) with Arabic suffix.
 */
fun formatViews(count: Long): String {
    if (count <= 0L) return ""
    val formatted = when {
        count >= 1_000_000_000L -> String.format(Locale.US, "%.1fB", count / 1_000_000_000.0)
        count >= 1_000_000L -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000L -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }.replace(".0M", "M").replace(".0K", "K").replace(".0B", "B")
    return "$formatted مشاهدة"
}

/**
 * Formats duration in milliseconds to mm:ss or hh:mm:ss.
 */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

private val uploadDateFormatThreadLocal = object : ThreadLocal<SimpleDateFormat>() {
    override fun initialValue(): SimpleDateFormat {
        return SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    }
}

/**
 * Formats timestamp in milliseconds to readable date string.
 */
fun formatUploadDate(ms: Long): String {
    if (ms <= 0L) return ""
    val sdf = uploadDateFormatThreadLocal.get() ?: SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    return sdf.format(Date(ms))
}
