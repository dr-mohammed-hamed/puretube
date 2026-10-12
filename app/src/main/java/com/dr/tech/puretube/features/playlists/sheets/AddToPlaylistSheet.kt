package com.dr.tech.puretube.features.playlists.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.playlists.PlaylistImportState

/**
 * Bottom sheet for saving a video into playlists: existing-playlist
 * checklist, inline new-playlist creation, and YouTube-URL import
 * with progress and error states. PureTheme tokens only, RTL by default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    videoTitle: String,
    playlists: List<PlaylistEntity>,
    checkedIds: Set<Long>,
    importState: PlaylistImportState,
    onToggle: (playlistId: Long, checked: Boolean) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onImportUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }
    var importUrl by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PureTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistAdd,
                    contentDescription = null,
                    tint = PureTheme.colors.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "حفظ في قائمة",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
            }

            Text(
                text = videoTitle,
                style = PureTheme.typography.labelSmall,
                color = PureTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (playlists.isEmpty()) {
                Text(
                    text = "لا توجد قوائم بعد — أنشئ قائمتك الأولى بالأسفل",
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(playlists, key = { it.id }) { playlist ->
                        val checked = playlist.id in checkedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggle(playlist.id, !checked) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { onToggle(playlist.id, it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = PureTheme.colors.primary,
                                    uncheckedColor = PureTheme.colors.textSecondary,
                                    checkmarkColor = PureTheme.colors.background
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = playlist.name,
                                style = PureTheme.typography.bodyMedium,
                                color = PureTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (checked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PureTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = PureTheme.colors.borderOutline)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "اسم قائمة جديدة",
                            style = PureTheme.typography.bodyMedium,
                            color = PureTheme.colors.textSecondary
                        )
                    },
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
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            onCreatePlaylist(newName.trim())
                            newName = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "إنشاء قائمة",
                        tint = PureTheme.colors.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    OutlinedTextField(
                        value = importUrl,
                        onValueChange = { importUrl = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                text = "استيراد: الصق رابط قائمة يوتيوب",
                                style = PureTheme.typography.bodyMedium,
                                color = PureTheme.colors.textSecondary
                            )
                        },
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
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (importState.isLoading) {
                    CircularProgressIndicator(
                        color = PureTheme.colors.primary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            if (importUrl.isNotBlank()) {
                                onImportUrl(importUrl.trim())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "استيراد قائمة",
                            tint = PureTheme.colors.primary
                        )
                    }
                }
            }

            if (importState.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = importState.errorMessage ?: "",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "إغلاق",
                    style = PureTheme.typography.labelMedium,
                    color = PureTheme.colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
