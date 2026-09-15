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
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Modal bottom sheet for selecting playback speed.
 * Conforms to Constitution Principles V, VI, VII.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSheet(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val speeds = remember { listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f) }

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
                text = "سرعة التشغيل",
                style = PureTheme.typography.headlineMedium,
                color = PureTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            speeds.forEach { speed ->
                val isSelected = (speed == currentSpeed)
                val label = if (speed == 1.0f) "1.0x (العادية)" else "${speed}x"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSpeedSelected(speed)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
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
