package com.dr.tech.puretube.features.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Displays offline-first Room database metrics (Constitution Principles III & VIII).
 */
@Composable
fun DatabaseStatsCard(
    subscriptionCount: Int,
    watchLaterCount: Int,
    historyCount: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, PureTheme.colors.borderOutline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = PureTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = PureTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "سيادة البيانات على الجهاز (Room SQLite)",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = "القنوات المتابعة",
                    value = "$subscriptionCount",
                    icon = Icons.Default.Subscriptions
                )
                StatItem(
                    label = "المحفوظات",
                    value = "$watchLaterCount",
                    icon = Icons.Default.Bookmark
                )
                StatItem(
                    label = "سجل المشاهدة",
                    value = "$historyCount",
                    icon = Icons.Default.History
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureTheme.colors.secondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            style = PureTheme.typography.titleMedium,
            color = PureTheme.colors.textPrimary
        )
        Text(
            text = label,
            style = PureTheme.typography.labelSmall,
            color = PureTheme.colors.textSecondary
        )
    }
}
