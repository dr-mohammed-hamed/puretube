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

enum class SleepTimerOption(val labelArabic: String, val durationMinutes: Int?) {
    OFF("إيقاف المؤقت", null),
    MIN_15("15 دقيقة", 15),
    MIN_30("30 دقيقة", 30),
    MIN_45("45 دقيقة", 45),
    MIN_60("60 دقيقة (ساعة)", 60),
    END_OF_VIDEO("عند نهاية هذا المقطع", 0)
}

/**
 * Modal bottom sheet for configuring sleep timer.
 * Conforms to Constitution Principles I (Mindful Intent), V, VI, VII.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    activeOption: SleepTimerOption,
    onDismiss: () -> Unit,
    onOptionSelected: (SleepTimerOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = remember { SleepTimerOption.entries }

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
                text = "مؤقت النوم وإيقاف التشغيل",
                style = PureTheme.typography.headlineMedium,
                color = PureTheme.colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "يتوقف التشغيل تلقائياً بعد انقضاء الوقت لمساعدتك على النوم دون هدر للطاقة والوقت.",
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            options.forEach { option ->
                val isSelected = (option == activeOption)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOptionSelected(option)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.labelArabic,
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
