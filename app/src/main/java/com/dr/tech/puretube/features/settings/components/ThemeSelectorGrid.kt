package com.dr.tech.puretube.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset

/**
 * Grid/List of the 6 approved PureTube visual themes (Single Source of Truth).
 * Conforms to DESIGN_SYSTEM.md and Constitution Principles V & VII.
 */
@Composable
fun ThemeSelectorGrid(
    currentPreset: ThemePreset,
    onPresetSelected: (ThemePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = PureTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "الهوية البصرية ونظام التصميم الموحد",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        }

        ThemePreset.entries.forEach { preset ->
            val isSelected = preset == currentPreset
            ThemePresetCard(
                preset = preset,
                isSelected = isSelected,
                onClick = { onPresetSelected(preset) }
            )
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PureTheme.colors.primary else PureTheme.colors.borderOutline
    val surfaceColor = if (isSelected) PureTheme.colors.surfaceElevated else PureTheme.colors.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Color indicator dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PureTheme.colors.primary else PureTheme.colors.secondary)
                )

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = preset.titleArabic,
                        style = PureTheme.typography.titleMedium,
                        color = if (isSelected) PureTheme.colors.primary else PureTheme.colors.textPrimary
                    )
                    Text(
                        text = preset.descriptionArabic,
                        style = PureTheme.typography.bodySmall,
                        color = PureTheme.colors.textSecondary
                    )
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) PureTheme.colors.primary else PureTheme.colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
