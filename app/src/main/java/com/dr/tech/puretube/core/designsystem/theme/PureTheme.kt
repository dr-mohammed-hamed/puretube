package com.dr.tech.puretube.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Central Theme Access Point for PureTube (نَقِيّ).
 * Single Source of Truth: All UI files must access colors through PureTheme.colors.*
 */
object PureTheme {
    val colors: PureColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalPureColors.current

    val typography
        @Composable
        @ReadOnlyComposable
        get() = PureTypography
}

/**
 * Root Theme Wrapper for PureTube.
 * Provides LocalPureColors to the Composition tree, enforces RTL layout direction,
 * and bridges semantic tokens to MaterialTheme.
 */
@Composable
fun PureTheme(
    preset: ThemePreset = ThemePreset.EMERALD_NIGHT,
    content: @Composable () -> Unit
) {
    val pureColors = remember(preset) { preset.toColorScheme() }

    val materialColors = remember(pureColors) {
        darkColorScheme(
            primary = pureColors.primary,
            onPrimary = pureColors.background,
            secondary = pureColors.secondary,
            onSecondary = pureColors.background,
            background = pureColors.background,
            onBackground = pureColors.textPrimary,
            surface = pureColors.surface,
            onSurface = pureColors.textPrimary,
            surfaceVariant = pureColors.surfaceElevated,
            onSurfaceVariant = pureColors.textSecondary,
            outline = pureColors.borderOutline,
            error = pureColors.error
        )
    }

    CompositionLocalProvider(
        LocalPureColors provides pureColors,
        LocalLayoutDirection provides LayoutDirection.Rtl
    ) {
        MaterialTheme(
            colorScheme = materialColors,
            typography = PureTypography,
            content = content
        )
    }
}
