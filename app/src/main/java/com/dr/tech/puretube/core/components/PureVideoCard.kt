package com.dr.tech.puretube.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared Video Card component for PureTube (نَقِيّ).
 * Conforms to Single Source of Truth Theme tokens, Zero-Emoji discipline,
 * Coil image loading with crossfade, 16:9 thumbnail, duration badge, and bookmark action.
 */
@Composable
fun PureVideoCard(
    video: FeedVideoItem,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    isBookmarked: Boolean = false,
    onChannelClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = PureTheme.colors.surface,
        border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
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

                if (video.durationMs > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(
                                color = PureTheme.colors.background.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(6.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
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
                            modifier = if (onChannelClick != null) {
                                Modifier.clickable { onChannelClick(video.channelId) }
                            } else {
                                Modifier
                            }
                        )

                        val uploadText = video.uploadDateText?.takeIf { it.isNotBlank() }
                            ?: formatUploadDate(video.uploadDateMs).takeIf { it.isNotBlank() }

                        if (uploadText != null) {
                            Text(
                                text = " • $uploadText",
                                style = PureTheme.typography.bodyMedium,
                                color = PureTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

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

