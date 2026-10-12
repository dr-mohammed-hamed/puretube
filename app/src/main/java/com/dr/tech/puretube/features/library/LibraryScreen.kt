package com.dr.tech.puretube.features.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.library.components.ContinueWatchingCard
import com.dr.tech.puretube.features.library.components.PlaylistRowCard
import com.dr.tech.puretube.features.playlists.sheets.CreatePlaylistDialog
import com.dr.tech.puretube.features.watchlater.components.WatchLaterItemCard
import org.koin.androidx.compose.koinViewModel

/**
 * Library tab: resume history, watch-later shortcut and user playlists.
 * Fully offline-capable (Room flows only). PureTheme tokens, RTL by default.
 */
@Composable
fun LibraryScreen(
    onVideoClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onPlaylistClick: (Long) -> Unit = {},
    onSeeAllWatchLater: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playlistError by viewModel.playlistError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(playlistError) {
        val msg = playlistError
        if (msg != null) {
            try {
                snackbarHostState.showSnackbar(msg)
            } finally {
                viewModel.clearPlaylistError()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureTheme.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المكتبة",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
                if (state.history.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "مسح سجل المشاهدة",
                            tint = PureTheme.colors.textSecondary
                        )
                    }
                }
            }

            if (state.isLoading) {
                PureFeedShimmerList(
                    count = 3,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else if (state.history.isEmpty() && state.watchLater.isEmpty() && state.playlists.isEmpty()) {
                PureEmptyState(
                    icon = Icons.Outlined.VideoLibrary,
                    title = "مكتبتك فارغة",
                    description = "ستظهر هنا مقاطع المتابعة والمحفوظات وقوائم التشغيل التي تنشئها.",
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (state.history.isNotEmpty()) {
                        item(key = "history_header") {
                            SectionHeader(
                                icon = Icons.Outlined.History,
                                title = "مواصلة المشاهدة"
                            )
                        }
                        items(state.history.take(10), key = { "h_${it.videoId}" }) { item ->
                            ContinueWatchingCard(
                                item = item,
                                onClick = { onVideoClick(item.videoId) }
                            )
                        }
                    }

                    if (state.watchLater.isNotEmpty()) {
                        item(key = "watchlater_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(
                                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                    title = "المشاهدة لاحقاً"
                                )
                                TextButton(onClick = onSeeAllWatchLater) {
                                    Text(
                                        text = "عرض الكل",
                                        style = PureTheme.typography.labelMedium,
                                        color = PureTheme.colors.primary
                                    )
                                }
                            }
                        }
                        items(state.watchLater.take(5), key = { "w_${it.videoId}" }) { item ->
                            WatchLaterItemCard(
                                item = item,
                                onClick = { onVideoClick(item.videoId) },
                                onDeleteClick = { viewModel.removeWatchLater(item.videoId) }
                            )
                        }
                    }

                    item(key = "playlists_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SectionHeader(
                                icon = Icons.Outlined.VideoLibrary,
                                title = "قوائمي (${state.playlists.size})"
                            )
                            TextButton(onClick = { showCreateDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PureTheme.colors.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إنشاء قائمة",
                                    style = PureTheme.typography.labelMedium,
                                    color = PureTheme.colors.primary
                                )
                            }
                        }
                    }

                    if (state.playlists.isEmpty()) {
                        item(key = "playlists_empty") {
                            Text(
                                text = "لا توجد قوائم بعد — أنشئ قائمتك الأولى لحفظ وتنظيم المقاطع.",
                                style = PureTheme.typography.bodyMedium,
                                color = PureTheme.colors.textSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        items(state.playlists, key = { "p_${it.id}" }) { playlist ->
                            PlaylistRowCard(
                                name = playlist.name,
                                thumbnailUrl = null,
                                onClick = { onPlaylistClick(playlist.id) }
                            )
                        }
                    }

                    item(key = "bottom_space") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }
}

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PureTheme.colors.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = PureTheme.typography.titleMedium,
            color = PureTheme.colors.textPrimary
        )
    }
}
