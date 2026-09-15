package com.dr.tech.puretube.features.player.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Modal bottom sheet for selecting video streaming resolution/quality.
 * Conforms to Constitution Principles V, VI, VII.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    currentQuality: PlaybackQuality,
    onDismiss: () -> Unit,
    onQualitySelected: (PlaybackQuality) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val qualities = remember {
        listOf(
            PlaybackQuality.AUTO,
            PlaybackQuality.HD_1080,
            PlaybackQuality.HD_720,
            PlaybackQuality.SD_480,
            PlaybackQuality.SD_360
        )
    }

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
                text = "جودة الفيديو",
                style = PureTheme.typography.headlineMedium,
                color = PureTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            qualities.forEach { quality ->
                val isSelected = (quality == currentQuality)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onQualitySelected(quality)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = quality.label,
                        style = PureTheme.typography.bodyMedium,
                        color = if (isSelected) PureTheme.colors.primary else PureTheme.colors.textPrimary
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = PureTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
