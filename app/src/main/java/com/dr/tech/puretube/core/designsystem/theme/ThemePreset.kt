package com.dr.tech.puretube.core.designsystem.theme

/**
 * Predefined Theme Presets for PureTube (نَقِيّ).
 * Allows changing the entire look and feel of the application dynamically
 * from user settings without modifying individual UI files.
 */
enum class ThemePreset(
    val titleArabic: String,
    val descriptionArabic: String
) {
    EMERALD_NIGHT(
        titleArabic = "الزمرد المشع",
        descriptionArabic = "أسود فحمي ملكي مع زمرد فيروزي وعنبر دافئ"
    ),
    ROYAL_INDIGO(
        titleArabic = "نور ووقار",
        descriptionArabic = "كحلي ملكي عميق مع أرجواني ساطع وذهب دافئ"
    ),
    NORDIC_SAGE(
        titleArabic = "سكينة الطبيعة",
        descriptionArabic = "أخضر غابي داكن مع نعناع مهدئ ورمل ناعم"
    ),
    WARM_ESPRESSO(
        titleArabic = "دفء الأصالة",
        descriptionArabic = "بني إسبريسو داكن مع توهج عنبري مريح للقراءة ليلاً"
    ),
    OLED_PURE_BLACK(
        titleArabic = "سواد الأوليد",
        descriptionArabic = "سواد نقي 100% لأقصى توفير للطاقة على شاشات AMOLED"
    ),
    SAPPHIRE_OCEAN(
        titleArabic = "أزرق الياقوت",
        descriptionArabic = "أزرق محيطي هادئ يبعث على التركيز والصفاء"
    )
}

/**
 * Default Emerald Night Color Scheme (Vibrant Purity).
 */
val EmeraldNightColorScheme = PureColorScheme(
    primary = PureRawColors.RadiantEmerald,
    primaryVariant = PureRawColors.DeepEmerald,
    secondary = PureRawColors.WarmAmber,
    background = PureRawColors.ObsidianCharcoal,
    surface = PureRawColors.DarkSlate,
    surfaceElevated = PureRawColors.ElevatedSlate,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.SubtleBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * Royal Indigo & Champagne Gold Scheme ("نور ووقار").
 */
val RoyalIndigoColorScheme = PureColorScheme(
    primary = PureRawColors.RoyalIndigo,
    primaryVariant = PureRawColors.DeepIndigo,
    secondary = PureRawColors.ChampagneGold,
    background = PureRawColors.MidnightNavy,
    surface = PureRawColors.NavySurface,
    surfaceElevated = PureRawColors.NavyElevated,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.NavyBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * Nordic Sage & Pine Scheme ("سكينة الطبيعة").
 */
val NordicSageColorScheme = PureColorScheme(
    primary = PureRawColors.SageMint,
    primaryVariant = PureRawColors.DeepSage,
    secondary = PureRawColors.SoftSand,
    background = PureRawColors.ForestDark,
    surface = PureRawColors.ForestSurface,
    surfaceElevated = PureRawColors.ForestElevated,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.ForestBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * Warm Espresso & Amber Scheme ("دفء الأصالة").
 */
val WarmEspressoColorScheme = PureColorScheme(
    primary = PureRawColors.AmberGlow,
    primaryVariant = PureRawColors.DeepAmber,
    secondary = PureRawColors.WarmTerracotta,
    background = PureRawColors.EspressoDark,
    surface = PureRawColors.EspressoSurface,
    surfaceElevated = PureRawColors.EspressoElevated,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.EspressoBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * OLED Pure Black Color Scheme.
 */
val OledBlackColorScheme = PureColorScheme(
    primary = PureRawColors.RadiantEmerald,
    primaryVariant = PureRawColors.DeepEmerald,
    secondary = PureRawColors.WarmAmber,
    background = PureRawColors.OledBlack,
    surface = PureRawColors.OledSurface,
    surfaceElevated = PureRawColors.OledElevated,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.OledBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * Sapphire Ocean Color Scheme.
 */
val SapphireOceanColorScheme = PureColorScheme(
    primary = PureRawColors.SapphireBlue,
    primaryVariant = PureRawColors.DeepSapphire,
    secondary = PureRawColors.WarmAmber,
    background = PureRawColors.OceanBackground,
    surface = PureRawColors.OceanSurface,
    surfaceElevated = PureRawColors.OceanElevated,
    textPrimary = PureRawColors.PureOffWhite,
    textSecondary = PureRawColors.MutedSlate,
    borderOutline = PureRawColors.OceanBorder,
    error = PureRawColors.ErrorRed,
    success = PureRawColors.SuccessGreen,
    isDark = true
)

/**
 * Resolve a ThemePreset to its corresponding PureColorScheme.
 */
fun ThemePreset.toColorScheme(): PureColorScheme = when (this) {
    ThemePreset.EMERALD_NIGHT -> EmeraldNightColorScheme
    ThemePreset.ROYAL_INDIGO -> RoyalIndigoColorScheme
    ThemePreset.NORDIC_SAGE -> NordicSageColorScheme
    ThemePreset.WARM_ESPRESSO -> WarmEspressoColorScheme
    ThemePreset.OLED_PURE_BLACK -> OledBlackColorScheme
    ThemePreset.SAPPHIRE_OCEAN -> SapphireOceanColorScheme
}
