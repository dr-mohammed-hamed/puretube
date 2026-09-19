package com.dr.tech.puretube.features.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.player.util.VideoChapter

/**
 * Horizontal scrollable row displaying video chapters and interactive timestamp pills.
 * Clicking a chapter seeks the player to that timestamp.
 * Conforms to Constitution Principles V, VI (< 100 LOC), VII.
 */
@Composable
fun VideoChaptersRow(
    chapters: List<VideoChapter>,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (chapters.isEmpty()) return

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "فصول ومحاور المقطع",
        style = PureTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = PureTheme.colors.textPrimary
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chapters.forEach { chapter ->
            Surface(
                onClick = { onSeekTo(chapter.timestampMs) },
                shape = RoundedCornerShape(8.dp),
                color = PureTheme.colors.surfaceElevated,
                border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = chapter.rawText,
                        style = PureTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = PureTheme.colors.primary
                    )
                    Text(
                        text = chapter.title,
                        style = PureTheme.typography.labelSmall,
                        color = PureTheme.colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
