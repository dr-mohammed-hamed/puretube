package com.dr.tech.puretube.features.playlists.sheets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Name-input dialog for creating a playlist. Blank names are rejected locally.
 * PureTheme tokens only, RTL by default.
 */
@Composable
fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var showBlankError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "إنشاء قائمة جديدة",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showBlankError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            text = "اسم القائمة",
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.textSecondary
                        )
                    },
                    singleLine = true,
                    isError = showBlankError,
                    textStyle = PureTheme.typography.bodyMedium.copy(
                        color = PureTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureTheme.colors.primary,
                        unfocusedBorderColor = PureTheme.colors.borderOutline,
                        focusedContainerColor = PureTheme.colors.surface,
                        unfocusedContainerColor = PureTheme.colors.surface,
                        cursorColor = PureTheme.colors.primary,
                        errorBorderColor = PureTheme.colors.error
                    )
                )
                if (showBlankError) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اسم القائمة لا يمكن أن يكون فارغاً",
                        style = PureTheme.typography.labelSmall,
                        color = PureTheme.colors.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isBlank()) {
                        showBlankError = true
                    } else {
                        onConfirm(name.trim())
                    }
                }
            ) {
                Text(
                    text = "إنشاء",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "تراجع",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.textSecondary
                )
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
