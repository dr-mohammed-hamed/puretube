package com.dr.tech.puretube.features.channel.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.data.model.ChannelTabType
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Tab Row for Channel screen matching official YouTube navigation.
 * Conforms to Constitution Principles V, VI (< 300 LOC), and VII.
 */
@Composable
fun ChannelTabsRow(
    selectedTab: ChannelTabType,
    onTabSelected: (ChannelTabType) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ChannelTabType.VIDEOS to "فيديوهات",
        ChannelTabType.PLAYLISTS to "قوائم التشغيل",
        ChannelTabType.LIVE to "بث مباشر",
        ChannelTabType.ABOUT to "حول القناة"
    )

    val selectedIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = PureTheme.colors.background,
        contentColor = PureTheme.colors.primary,
        indicator = { tabPositions ->
            if (selectedIndex < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = PureTheme.colors.primary,
                    height = 3.dp
                )
            }
        },
        divider = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PureTheme.colors.borderOutline)
            )
        },
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, (tab, title) ->
            val isSelected = index == selectedIndex
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = title,
                        style = if (isSelected) PureTheme.typography.titleMedium else PureTheme.typography.bodyMedium,
                        color = if (isSelected) PureTheme.colors.textPrimary else PureTheme.colors.textSecondary
                    )
                }
            )
        }
    }
}
