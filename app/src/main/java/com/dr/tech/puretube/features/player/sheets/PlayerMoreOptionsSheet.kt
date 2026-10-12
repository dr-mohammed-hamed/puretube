package com.dr.tech.puretube.features.player.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Modal bottom sheet for additional player options: PiP, Audio-only, Share, FitScreen.
 * Conforms to Constitution Principles V, VI, VII:
 * < 150 LOC, zero emojis, PureTheme tokens, Cairo typography.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerMoreOptionsSheet(
    isAudioOnly: Boolean,
    isZoomToFill: Boolean,
    onDismiss: () -> Unit,
    onPipClick: () -> Unit,
    onAudioOnlyToggle: () -> Unit,
    onZoomToFillToggle: () -> Unit,
    onShareClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "خيارات إضافية",
                style = PureTheme.typography.headlineMedium,
                color = PureTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            MoreOptionItem(
                icon = Icons.Default.PictureInPictureAlt,
                title = "صورة داخل صورة (PiP)",
                subtitle = "متابعة المشاهدة بنافذة مصغرة عائمة",
                onClick = {
                    onDismiss()
                    onPipClick()
                }
            )

            MoreOptionItem(
                icon = Icons.Default.Headphones,
                title = if (isAudioOnly) "العودة للوضع المرئي" else "تشغيل صوتي فقط",
                subtitle = if (isAudioOnly) "عرض الفيديو والمشاهد" else "توفير البيانات والبطارية",
                isActive = isAudioOnly,
                onClick = {
                    onDismiss()
                    onAudioOnlyToggle()
                }
            )

            MoreOptionItem(
                icon = Icons.Default.FitScreen,
                title = if (isZoomToFill) "الأبعاد الأصلية (16:9)" else "ملاءمة الشاشة (ملء الشاشة)",
                subtitle = if (isZoomToFill) "إلغاء التكبير والعودة للأبعاد الطبيعية" else "تكبير الفيديو لملء كامل الشاشة",
                isActive = isZoomToFill,
                onClick = {
                    onDismiss()
                    onZoomToFillToggle()
                }
            )

            MoreOptionItem(
                icon = Icons.Default.Share,
                title = "مشاركة الفيديو",
                subtitle = "مشاركة رابط الفيديو عبر التطبيقات",
                onClick = {
                    onDismiss()
                    onShareClick()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoreOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) PureTheme.colors.primary else PureTheme.colors.textPrimary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = PureTheme.typography.bodyMedium,
                color = if (isActive) PureTheme.colors.primary else PureTheme.colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textSecondary
                )
            }
        }
    }
}
