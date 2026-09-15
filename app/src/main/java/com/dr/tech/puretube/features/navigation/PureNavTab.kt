package com.dr.tech.puretube.features.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation tabs for the PureTube bottom navigation shell.
 * Exclusively uses dignified Arabic titles and Material vector icons. Zero emojis.
 */
enum class PureNavTab(
    val titleArabic: String,
    val iconSelected: ImageVector,
    val iconUnselected: ImageVector
) {
    FEED(
        titleArabic = "الرئيسية",
        iconSelected = Icons.Filled.DynamicFeed,
        iconUnselected = Icons.Outlined.DynamicFeed
    ),
    SUBSCRIPTIONS(
        titleArabic = "الاشتراكات",
        iconSelected = Icons.Filled.Subscriptions,
        iconUnselected = Icons.Outlined.Subscriptions
    ),
    WATCH_LATER(
        titleArabic = "المشاهدة لاحقاً",
        iconSelected = Icons.Filled.Bookmark,
        iconUnselected = Icons.Outlined.BookmarkBorder
    ),
    SETTINGS(
        titleArabic = "الإعدادات",
        iconSelected = Icons.Filled.Settings,
        iconUnselected = Icons.Outlined.Settings
    )
}
