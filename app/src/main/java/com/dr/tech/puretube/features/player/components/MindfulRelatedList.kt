package com.dr.tech.puretube.features.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import java.util.Locale

/**
 * List of videos strictly from the same channel or user subscriptions.
 * Enforces Constitution Principle I: Zero algorithmic recommendations or rabbit holes.
 */
@Composable
fun MindfulRelatedList(
    videos: List<FeedVideoItem>,
    modifier: Modifier = Modifier,
    onVideoSelect: (String) -> Unit
) {
    if (videos.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "المزيد من نفس القناة",
            style = PureTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            color = PureTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            videos.forEach { video ->
                RelatedVideoRowItem(
                    video = video,
                    onClick = { onVideoSelect(video.videoId) }
                )
            }
        }
    }
}

@Composable
private fun RelatedVideoRowItem(
    video: FeedVideoItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 120.dp, height = 70.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = video.title,
                style = PureTheme.typography.bodyMedium,
                color = PureTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = video.channelName,
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.textSecondary
            )

            if (video.durationMs > 0L) {
                Text(
                    text = formatDuration(video.durationMs),
                    style = PureTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = PureTheme.colors.textSecondary
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
