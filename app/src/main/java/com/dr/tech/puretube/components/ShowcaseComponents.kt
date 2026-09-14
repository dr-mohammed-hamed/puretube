package com.dr.tech.puretube.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset
import com.dr.tech.puretube.core.designsystem.theme.toColorScheme

/**
 * =========================================================================================
 * [تنبيه هام للوكلاء والمطورين في الجلسات القادمة / NOTICE FOR FUTURE AGENTS]
 * =========================================================================
 * المكونات في هذا الملف مؤقتة وخاصة بالمرحلة الأولى (Phase 1 Smoke-Test Harness).
 * الهدف منها فقط: اختبار تجميع Compose، وفحص ثيمات الألوان، والتحقق من الخطوط العربية.
 *
 * في المرحلة الثالثة (Phase 3: UI & Navigation):
 * - يتم استبدال هذه المكونات بالهيكل الدائم MainNavigationShell (الخلاصة، الاشتراكات، المشاهدة لاحقاً).
 * - يُنقل اختيار الثيمات إلى شاشة الإعدادات الدائمة (SettingsScreen).
 * =========================================================================================
 */
@Deprecated(
    message = "Temporary showcase component for Milestone 1 infrastructure verification. To be replaced in Phase 3.",
    level = DeprecationLevel.WARNING
)
@Composable
fun TemporaryHarnessBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(PureTheme.colors.surfaceElevated)
            .border(1.dp, PureTheme.colors.secondary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            tint = PureTheme.colors.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "شاشة فحص مؤقتة (المرحلة 1) — تُستبدل بالتبويبات في المرحلة 3",
            style = PureTheme.typography.labelSmall,
            color = PureTheme.colors.secondary
        )
    }
}

@Composable
fun HeaderSection() {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureTheme.colors.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PureTheme.colors.background,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "PureTube | نَقِيّ",
                    style = PureTheme.typography.headlineMedium,
                    color = PureTheme.colors.textPrimary
                )
                Text(
                    text = "مختبر الهوية البصرية والمعمارية",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.secondary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "بيئة مشاهدة نقية تحمي العقل من مصائد الإدمان، مبنية على هندسة برمجية متينة.",
            style = PureTheme.typography.bodyMedium,
            color = PureTheme.colors.textSecondary
        )
    }
}

@Composable
fun ThemeSelectorSection(
    currentPreset: ThemePreset,
    onPresetSelected: (ThemePreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(PureTheme.colors.surface)
            .border(1.dp, PureTheme.colors.borderOutline, RoundedCornerShape(18.dp))
            .padding(18.dp)
    ) {
        Text(
            text = "اختر الهوية اللونية للتطبيق (Live Palettes)",
            style = PureTheme.typography.titleMedium,
            color = PureTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "انقر لتجربة الطابع البصري المناسب للراحة النفسية ومكافحة التشتت:",
            style = PureTheme.typography.bodyMedium,
            color = PureTheme.colors.textSecondary
        )
        Spacer(modifier = Modifier.height(14.dp))

        // First row of presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeCard(
                preset = ThemePreset.EMERALD_NIGHT,
                isSelected = currentPreset == ThemePreset.EMERALD_NIGHT,
                onClick = { onPresetSelected(ThemePreset.EMERALD_NIGHT) },
                modifier = Modifier.weight(1f)
            )
            ThemeCard(
                preset = ThemePreset.ROYAL_INDIGO,
                isSelected = currentPreset == ThemePreset.ROYAL_INDIGO,
                onClick = { onPresetSelected(ThemePreset.ROYAL_INDIGO) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Second row of presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeCard(
                preset = ThemePreset.NORDIC_SAGE,
                isSelected = currentPreset == ThemePreset.NORDIC_SAGE,
                onClick = { onPresetSelected(ThemePreset.NORDIC_SAGE) },
                modifier = Modifier.weight(1f)
            )
            ThemeCard(
                preset = ThemePreset.WARM_ESPRESSO,
                isSelected = currentPreset == ThemePreset.WARM_ESPRESSO,
                onClick = { onPresetSelected(ThemePreset.WARM_ESPRESSO) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Third row of presets (OLED & Sapphire)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeCard(
                preset = ThemePreset.OLED_PURE_BLACK,
                isSelected = currentPreset == ThemePreset.OLED_PURE_BLACK,
                onClick = { onPresetSelected(ThemePreset.OLED_PURE_BLACK) },
                modifier = Modifier.weight(1f)
            )
            ThemeCard(
                preset = ThemePreset.SAPPHIRE_OCEAN,
                isSelected = currentPreset == ThemePreset.SAPPHIRE_OCEAN,
                onClick = { onPresetSelected(ThemePreset.SAPPHIRE_OCEAN) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val ThemeCardShape = RoundedCornerShape(12.dp)

@Composable
private fun ThemeCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = preset.toColorScheme()
    val borderColor = if (isSelected) scheme.primary else PureTheme.colors.borderOutline
    val bgColor = if (isSelected) scheme.surfaceElevated else PureTheme.colors.surface

    Box(
        modifier = modifier
            .clip(ThemeCardShape)
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, ThemeCardShape)
            .clickable(enabled = !isSelected, onClick = onClick)
            .padding(10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Swatches
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(scheme.primary))
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(scheme.secondary))
                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(scheme.background).border(0.5.dp, PureTheme.colors.borderOutline, CircleShape))
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = preset.titleArabic,
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
            Text(
                text = preset.descriptionArabic,
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.textSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PureTheme.colors.surface)
            .border(1.dp, PureTheme.colors.borderOutline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
            .background(PureTheme.colors.surfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PureTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = PureTheme.typography.bodyMedium,
                color = PureTheme.colors.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PureTheme.colors.success,
            modifier = Modifier.size(20.dp)
        )
    }
}
