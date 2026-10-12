package com.dr.tech.puretube.features.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.util.ArabicTimeFormatter
import com.dr.tech.puretube.features.player.util.TimestampParser
import java.util.Locale

/**
 * Metadata section beneath the player displaying video title, channel row, quick actions,
 * interactive chapters/timestamps, and collapsible description.
 * Conforms to Constitution Principles I, V, VI (< 220 LOC), VII:
 * Zero emojis, PureTheme semantic colors, Cairo typography.
 */
@Composable
fun VideoMetadataSection(
    details: ExtractedVideoDetails,
    isSubscribed: Boolean,
    isSavedToWatchLater: Boolean,
    isAudioOnly: Boolean = false,
    modifier: Modifier = Modifier,
    onSubscribeToggle: () -> Unit,
    onWatchLaterToggle: () -> Unit,
    onAudioOnlyToggle: () -> Unit = {},
    onSleepTimerClick: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onChannelClick: () -> Unit = {}
) {
    val chapters = remember(details.description, details.durationMs) {
        TimestampParser.extractChapters(details.description, details.durationMs)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 1. Video Title
        Text(
            text = details.title,
            style = PureTheme.typography.headlineMedium.copy(fontSize = 18.sp),
            color = PureTheme.colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. View Count & Upload Date
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (details.viewCount > 0) {
                Text(
                    text = "${formatViews(details.viewCount)} مشاهدة",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textSecondary
                )
            }
            val formattedDate = remember(details.uploadDateText) {
                ArabicTimeFormatter.formatRelativeTime(0L, details.uploadDateText).takeIf { it.isNotBlank() }
            }
            if (formattedDate != null) {
                if (details.viewCount > 0) {
                    Text(
                        text = "•",
                        style = PureTheme.typography.labelSmall,
                        color = PureTheme.colors.textSecondary
                    )
                }
                Text(
                    text = formattedDate,
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Channel Row & Subscribe Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureTheme.colors.surface, RoundedCornerShape(12.dp))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onChannelClick)
            ) {
                if (!details.channelAvatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = details.channelAvatarUrl,
                        contentDescription = details.channelName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(PureTheme.colors.surfaceElevated, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = details.channelName.take(1),
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = details.channelName,
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.textPrimary
                    )
                    details.subscriberCount?.let { count ->
                        Text(
                            text = "${formatViews(count)} مشترك",
                            style = PureTheme.typography.labelSmall,
                            color = PureTheme.colors.textSecondary
                        )
                    }
                }
            }

            // Subscription Button
            if (isSubscribed) {
                OutlinedButton(
                    onClick = onSubscribeToggle,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PureTheme.colors.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "مشترك", style = PureTheme.typography.labelSmall)
                }
            } else {
                Button(
                    onClick = onSubscribeToggle,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureTheme.colors.primary,
                        contentColor = PureTheme.colors.background
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "اشتراك", style = PureTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Quick Action Row
        VideoActionButtonsRow(
            videoId = details.videoId,
            title = details.title,
            isSavedToWatchLater = isSavedToWatchLater,
            isAudioOnly = isAudioOnly,
            onWatchLaterToggle = onWatchLaterToggle,
            onAudioOnlyToggle = onAudioOnlyToggle,
            onSleepTimerClick = onSleepTimerClick
        )

        // 5. Chapters Row
        VideoChaptersRow(chapters = chapters, onSeekTo = onSeekTo)

        // 6. Collapsible Description Card
        VideoDescriptionCard(description = details.description)
    }
}

private fun formatViews(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
