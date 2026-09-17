package com.dr.tech.puretube.features.channel.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dr.tech.puretube.core.data.model.ChannelProfileDetails
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import java.util.Locale

/**
 * Channel Header component matching official YouTube aesthetics.
 * Displays banner, avatar, title, handle, subscriber count, description snippet,
 * and subscription/notification/share action buttons.
 * Conforms to Constitution Principles I, V, VI (< 300 LOC), VII (Zero Emojis).
 */
@Composable
fun ChannelHeader(
    profile: ChannelProfileDetails,
    isSubscribed: Boolean,
    isActionInProgress: Boolean,
    onSubscribeToggle: () -> Unit,
    onDescriptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureTheme.colors.background)
    ) {
        // 1. Channel Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 5f)
                .background(PureTheme.colors.surfaceElevated)
        ) {
            if (!profile.bannerUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(profile.bannerUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PureTheme.colors.surfaceElevated)
                )
            }
        }

        // 2. Avatar & Channel Identity
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PureTheme.colors.surfaceElevated)
                        .border(2.dp, PureTheme.colors.borderOutline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(profile.avatarUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = profile.channelName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = profile.channelName.take(1),
                            style = PureTheme.typography.headlineMedium,
                            color = PureTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Channel Name & Verification
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = profile.channelName,
                            style = PureTheme.typography.titleLarge.copy(fontSize = 20.sp),
                            color = PureTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "قناة موثقة",
                                tint = PureTheme.colors.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Handle & Subscriber Count
                    val stats = buildList {
                        profile.channelHandle?.takeIf { it.isNotBlank() }?.let { add(it) }
                        if (profile.subscriberCount > 0L) {
                            add("${formatNumber(profile.subscriberCount)} مشترك")
                        } else profile.subscriberCountText?.let { add(it) }
                    }.joinToString(" • ")

                    if (stats.isNotBlank()) {
                        Text(
                            text = stats,
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Description Snippet (clickable)
            if (!profile.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = profile.description,
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDescriptionClick)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Action Buttons Row: Subscribe | Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isSubscribed) {
                    OutlinedButton(
                        onClick = onSubscribeToggle,
                        enabled = !isActionInProgress,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PureTheme.colors.textPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PureTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "مشترك",
                            style = PureTheme.typography.titleMedium,
                            color = PureTheme.colors.textPrimary
                        )
                    }
                } else {
                    Button(
                        onClick = onSubscribeToggle,
                        enabled = !isActionInProgress,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureTheme.colors.primary,
                            contentColor = PureTheme.colors.background
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "اشتراك",
                            style = PureTheme.typography.titleMedium,
                            color = PureTheme.colors.background
                        )
                    }
                }

                // Share Button
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            val url = "https://www.youtube.com/channel/${profile.channelId}"
                            putExtra(Intent.EXTRA_TEXT, "${profile.channelName}: $url")
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة القناة"))
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PureTheme.colors.surfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "مشاركة القناة",
                        tint = PureTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatNumber(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
