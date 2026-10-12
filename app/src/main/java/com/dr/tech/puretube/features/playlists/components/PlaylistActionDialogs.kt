package com.dr.tech.puretube.features.playlists.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Dialog for renaming an existing playlist.
 */
@Composable
fun RenamePlaylistDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "إعادة تسمية القائمة",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = PureTheme.typography.bodyMedium.copy(
                    color = PureTheme.colors.textPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureTheme.colors.primary,
                    unfocusedBorderColor = PureTheme.colors.borderOutline,
                    focusedContainerColor = PureTheme.colors.surface,
                    unfocusedContainerColor = PureTheme.colors.surface,
                    cursorColor = PureTheme.colors.primary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newName.isNotBlank()) {
                        onConfirm(newName.trim())
                    }
                }
            ) {
                Text(
                    text = "حفظ",
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
        }
    )
}

/**
 * Dialog for confirming playlist deletion.
 */
@Composable
fun DeletePlaylistDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "حذف القائمة",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        },
        text = {
            Text(
                text = "هل أنت متأكد من حذف هذه القائمة مع جميع عناصرها؟",
                style = PureTheme.typography.bodyMedium,
                color = PureTheme.colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "حذف",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.error
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
        }
    )
}

/**
 * Dialog for confirming clearing all items from a playlist.
 */
@Composable
fun ClearPlaylistDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PureTheme.colors.surface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "إفراغ القائمة",
                style = PureTheme.typography.titleMedium,
                color = PureTheme.colors.textPrimary
            )
        },
        text = {
            Text(
                text = "هل تريد حذف جميع العناصر مع الاحتفاظ بالقائمة؟",
                style = PureTheme.typography.bodyMedium,
                color = PureTheme.colors.textSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "إفراغ",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.error
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
        }
    )
}
