package com.dr.tech.puretube.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Raw Color Palette for PureTube (نَقِيّ).
 * Single Source of Truth for raw hex colors according to DESIGN_SYSTEM.md.
 * UI components must NEVER reference these raw colors directly;
 * always use PureTheme.colors.* semantic tokens.
 */
object PureRawColors {
    // Vibrant Purity - Emerald Theme (Default)
    val RadiantEmerald = Color(0xFF00D09C)
    val DeepEmerald = Color(0xFF059669)
    val WarmAmber = Color(0xFFF59E0B)
    val ObsidianCharcoal = Color(0xFF0B0F14)
    val DarkSlate = Color(0xFF161E27)
    val ElevatedSlate = Color(0xFF1F2937)
    val PureOffWhite = Color(0xFFF3F4F6)
    val MutedSlate = Color(0xFF9CA3AF)
    val SubtleBorder = Color(0xFF263342)

    // OLED Pure Black Theme Tokens
    val OledBlack = Color(0xFF000000)
    val OledSurface = Color(0xFF0D0D0D)
    val OledElevated = Color(0xFF171717)
    val OledBorder = Color(0xFF262626)

    // Sapphire Ocean Theme Tokens
    val SapphireBlue = Color(0xFF0EA5E9)
    val DeepSapphire = Color(0xFF0284C7)
    val OceanBackground = Color(0xFF0A0E17)
    val OceanSurface = Color(0xFF131B2B)
    val OceanElevated = Color(0xFF1C273C)
    val OceanBorder = Color(0xFF23324C)

    // Royal Indigo & Champagne Gold Tokens ("نور ووقار")
    val RoyalIndigo = Color(0xFF6366F1)
    val DeepIndigo = Color(0xFF4F46E5)
    val ChampagneGold = Color(0xFFF3C969)
    val MidnightNavy = Color(0xFF090C15)
    val NavySurface = Color(0xFF121826)
    val NavyElevated = Color(0xFF1B2236)
    val NavyBorder = Color(0xFF232E4A)

    // Nordic Pine & Sage Mint Tokens ("سكينة الطبيعة")
    val SageMint = Color(0xFF2DD4BF)
    val DeepSage = Color(0xFF0D9488)
    val SoftSand = Color(0xFFFDE68A)
    val ForestDark = Color(0xFF09130E)
    val ForestSurface = Color(0xFF111F18)
    val ForestElevated = Color(0xFF1B2E24)
    val ForestBorder = Color(0xFF243D30)

    // Warm Espresso & Amber Glow Tokens ("دفء الأصالة")
    val AmberGlow = Color(0xFFF59E0B)
    val DeepAmber = Color(0xFFD97706)
    val WarmTerracotta = Color(0xFFFB923C)
    val EspressoDark = Color(0xFF120E0D)
    val EspressoSurface = Color(0xFF1C1614)
    val EspressoElevated = Color(0xFF29201D)
    val EspressoBorder = Color(0xFF3B2F2A)

    // Utility & Alert Colors
    val ErrorRed = Color(0xFFEF4444)
    val SuccessGreen = Color(0xFF10B981)
}
