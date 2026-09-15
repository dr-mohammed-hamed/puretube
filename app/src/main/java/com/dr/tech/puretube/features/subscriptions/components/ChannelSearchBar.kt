package com.dr.tech.puretube.features.subscriptions.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Search and entry bar for subscribing to new channels.
 * Accepts channel URLs, channel IDs, or @handles.
 */
@Composable
fun ChannelSearchBar(
    onAddChannel: (String) -> Unit,
    isActionInProgress: Boolean,
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PureTheme.colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                placeholder = {
                    Text(
                        text = "رابط القناة أو المعرف @handle",
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PureTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                maxLines = 1,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PureTheme.colors.primary,
                    unfocusedBorderColor = PureTheme.colors.borderOutline,
                    cursorColor = PureTheme.colors.primary,
                    focusedTextColor = PureTheme.colors.textPrimary,
                    unfocusedTextColor = PureTheme.colors.textPrimary,
                    focusedContainerColor = PureTheme.colors.surfaceElevated,
                    unfocusedContainerColor = PureTheme.colors.surfaceElevated
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (queryText.isNotBlank() && !isActionInProgress) {
                        onAddChannel(queryText.trim())
                        queryText = ""
                        keyboardController?.hide()
                    }
                })
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (queryText.isNotBlank() && !isActionInProgress) {
                        onAddChannel(queryText.trim())
                        queryText = ""
                        keyboardController?.hide()
                    }
                },
                enabled = queryText.isNotBlank() && !isActionInProgress,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureTheme.colors.primary,
                    contentColor = PureTheme.colors.background,
                    disabledContainerColor = PureTheme.colors.surfaceElevated,
                    disabledContentColor = PureTheme.colors.textSecondary
                )
            ) {
                if (isActionInProgress) {
                    CircularProgressIndicator(
                        color = PureTheme.colors.background,
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إضافة قناة",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "متابعة",
                        style = PureTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}
