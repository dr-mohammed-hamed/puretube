package com.dr.tech.puretube.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic Color Scheme for PureTube (نَقِيّ).
 * All UI components MUST exclusively reference these semantic tokens via PureTheme.colors.*
 * Never hardcode raw Color() instances in UI composables.
 */
@Immutable
data class PureColorScheme(
    val primary: Color,
    val primaryVariant: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val borderOutline: Color,
    val error: Color = PureRawColors.ErrorRed,
    val success: Color = PureRawColors.SuccessGreen,
    val isDark: Boolean = true
)

/**
 * CompositionLocal providing the current PureColorScheme throughout the Compose tree.
 */
val LocalPureColors = staticCompositionLocalOf<PureColorScheme> {
    error("No PureColorScheme provided! Ensure your UI is wrapped in PureTheme { ... }")
}
