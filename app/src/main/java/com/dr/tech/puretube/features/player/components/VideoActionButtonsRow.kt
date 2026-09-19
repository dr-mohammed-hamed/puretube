package com.dr.tech.puretube.features.player.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Quick action buttons row beneath video metadata (Bookmark, Share, Audio-only, Sleep Timer).
 * Conforms to Constitution Principles I, V, VI (< 150 LOC), VII (Zero Emojis, PureTheme tokens).
 */
@Composable
fun VideoActionButtonsRow(
    videoId: String,
    title: String,
    isSavedToWatchLater: Boolean,
    isAudioOnly: Boolean,
    onWatchLaterToggle: () -> Unit,
    onAudioOnlyToggle: () -> Unit,
    onSleepTimerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Bookmark Action
        Surface(
            onClick = onWatchLaterToggle,
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, if (isSavedToWatchLater) PureTheme.colors.secondary else PureTheme.colors.borderOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isSavedToWatchLater) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "المشاهدة لاحقاً",
                    tint = if (isSavedToWatchLater) PureTheme.colors.secondary else PureTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isSavedToWatchLater) "محفوظ" else "حفظ لاحقاً",
                    style = PureTheme.typography.labelSmall,
                    color = if (isSavedToWatchLater) PureTheme.colors.secondary else PureTheme.colors.textPrimary
                )
            }
        }

        // 2. Share Action
        Surface(
            onClick = {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, "https://youtu.be/$videoId")
                }
                try {
                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفيديو"))
                } catch (e: Exception) {
                    android.util.Log.w("VideoActionButtonsRow", "Could not start share intent", e)
                }
            },
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "مشاركة",
                    tint = PureTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "مشاركة",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textPrimary
                )
            }
        }

        // 3. Audio-Only Action
        Surface(
            onClick = onAudioOnlyToggle,
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, if (isAudioOnly) PureTheme.colors.primary else PureTheme.colors.borderOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "صوتي فقط",
                    tint = if (isAudioOnly) PureTheme.colors.primary else PureTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (isAudioOnly) "وضع صوتي" else "استماع فقط",
                    style = PureTheme.typography.labelSmall,
                    color = if (isAudioOnly) PureTheme.colors.primary else PureTheme.colors.textPrimary
                )
            }
        }

        // 4. Sleep Timer Action
        Surface(
            onClick = onSleepTimerClick,
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bedtime,
                    contentDescription = "مؤقت النوم",
                    tint = PureTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "مؤقت النوم",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textPrimary
                )
            }
        }
    }
}
