package com.dr.tech.puretube.features.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.components.PureVideoCard
import com.dr.tech.puretube.core.data.model.toFeedVideoItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.playlists.components.ClearPlaylistDialog
import com.dr.tech.puretube.features.playlists.components.DeletePlaylistDialog
import com.dr.tech.puretube.features.playlists.components.RenamePlaylistDialog
import org.koin.androidx.compose.koinViewModel

/**
 * Playlist detail: rename/delete/clear header, items list, play-all
 * and YouTube-URL import. PureTheme tokens only, RTL by default.
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onVideoClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    miniPlayerContent: @Composable () -> Unit = {},
    viewModel: PlaylistViewModel = koinViewModel()
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlist = playlists.firstOrNull { it.id == playlistId }

    LaunchedEffect(playlistId) {
        viewModel.selectPlaylist(playlistId)
    }

    LaunchedEffect(actionError) {
        val msg = actionError
        if (msg != null) {
            try {
                snackbarHostState.showSnackbar(msg)
            } finally {
                viewModel.clearActionError()
            }
        }
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var importUrl by remember { mutableStateOf("") }

    BackHandler(onBack = onBackClick)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureTheme.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = PureTheme.colors.textPrimary
                    )
                }
                Text(
                    text = playlist?.name ?: "تفاصيل القائمة",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showRenameDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "إعادة تسمية",
                        tint = PureTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        contentDescription = "إفراغ القائمة",
                        tint = PureTheme.colors.textSecondary
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "حذف القائمة",
                        tint = PureTheme.colors.error
                    )
                }
            }

            if (playlist == null) {
                PureFeedShimmerList(
                    count = 2,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "actions") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (items.isNotEmpty()) {
                                TextButton(onClick = { onVideoClick(items.first().videoId) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = PureTheme.colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "تشغيل الكل (${items.size})",
                                        style = PureTheme.typography.labelMedium,
                                        color = PureTheme.colors.primary
                                    )
                                }
                            }
                        }
                    }

                    item(key = "import") {
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
                                            text = "استيراد: رابط قائمة يوتيوب",
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
                                            viewModel.importFromUrl(importUrl.trim())
                                            importUrl = ""
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
                    }

                    if (items.isEmpty()) {
                        item(key = "empty") {
                            PureEmptyState(
                                icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                title = "القائمة فارغة",
                                description = "أضف مقاطع من المشغل أو البحث، أو استورد قائمة يوتيوب عبر الرابط أعلاه."
                            )
                        }
                    } else {
                        items(items, key = { it.id }) { entity ->
                            PureVideoCard(
                                video = entity.toFeedVideoItem(),
                                isBookmarked = true,
                                onClick = { onVideoClick(entity.videoId) },
                                onBookmarkClick = {
                                    viewModel.removeFromPlaylist(playlistId, entity.videoId)
                                },
                                onChannelClick = onChannelClick
                            )
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            miniPlayerContent()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showRenameDialog && playlist != null) {
        RenamePlaylistDialog(
            initialName = playlist.name,
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlistId, newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }

    if (showDeleteDialog) {
        DeletePlaylistDialog(
            onConfirm = {
                viewModel.deletePlaylist(playlistId) { onBackClick() }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showClearDialog) {
        ClearPlaylistDialog(
            onConfirm = {
                viewModel.clearPlaylist(playlistId)
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}
