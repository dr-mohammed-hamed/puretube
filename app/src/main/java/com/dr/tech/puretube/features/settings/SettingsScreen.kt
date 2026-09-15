package com.dr.tech.puretube.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.settings.components.DatabaseStatsCard
import com.dr.tech.puretube.features.settings.components.ThemeSelectorGrid
import org.koin.androidx.compose.koinViewModel

/**
 * Settings and customization screen.
 * Hosts permanent theme selection, offline database stats, and system information.
 * Conforms to Constitution Principles I, III, V, and VII.
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        SettingsHeader()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                DatabaseStatsCard(
                    subscriptionCount = state.subscriptionCount,
                    watchLaterCount = state.watchLaterCount,
                    historyCount = state.historyCount
                )
            }

            item {
                ThemeSelectorGrid(
                    currentPreset = state.currentPreset,
                    onPresetSelected = { viewModel.selectThemePreset(it) }
                )
            }

            item {
                AboutPureTubeCard(appVersion = state.appVersion)
            }
        }
    }
}

@Composable
private fun SettingsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = PureTheme.colors.primary,
            modifier = Modifier.size(26.dp)
        )
        Column {
            Text(
                text = "الإعدادات والمظهر",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
            Text(
                text = "التحكم الكامل والخصوصية التامة",
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.textSecondary
            )
        }
    }
}

@Composable
private fun AboutPureTubeCard(appVersion: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, PureTheme.colors.borderOutline, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = PureTheme.colors.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = PureTheme.colors.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "عن مشروع PureTube (نَقِيّ)",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
            }

            Text(
                text = "بيئة يوتيوب وقائية، آمنة ومحصنة للمتعافين والشباب والأطفال، خالية تماماً من الفتن ومصائد الإدمان والخوارزميات الاستدراجية. بياناتك بالكامل ملك لك ومحفوظة محلياً على جهازك.",
                style = PureTheme.typography.bodySmall,
                color = PureTheme.colors.textSecondary
            )

            Text(
                text = "الإصدار $appVersion | الإصدار الوقائي المعتمد",
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.secondary
            )
        }
    }
}
