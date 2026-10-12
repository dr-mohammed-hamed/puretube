package com.dr.tech.puretube.features.channel.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.data.model.ChannelPlaylistItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Playlists tab displaying channel's organized playlist cards.
 * Supports header and sticky tabs directly inside LazyColumn for unified scrolling.
 * Conforms to Constitution Principles I, V, VI (< 300 LOC), VII.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelPlaylistsTab(
    playlists: List<ChannelPlaylistItem>,
    isLoading: Boolean,
    onPlaylistClick: (ChannelPlaylistItem) -> Unit,
    modifier: Modifier = Modifier,
    headerContent: (@Composable () -> Unit)? = null,
    tabsContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (headerContent != null) {
            item(key = "channel_header") {
                headerContent()
            }
        }

        if (tabsContent != null) {
            stickyHeader(key = "channel_tabs") {
                tabsContent()
            }
        }

        when {
            isLoading && playlists.isEmpty() -> {
                item(key = "loading_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PureTheme.colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            playlists.isEmpty() -> {
                item(key = "empty_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        PureEmptyState(
                            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                            title = "لا توجد قوائم تشغيل",
                            description = "لم يقم هذا الناشر بإنشاء أي قوائم تشغيل عامة بعد."
                        )
                    }
                }
            }

            else -> {
                items(
                    items = playlists,
                    key = { it.playlistId }
                ) { playlist ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Surface(
                            onClick = { onPlaylistClick(playlist) },
                            shape = RoundedCornerShape(14.dp),
                            color = PureTheme.colors.surface,
                            border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail with playlist badge
                                Box(
                                    modifier = Modifier
                                        .width(120.dp)
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PureTheme.colors.surfaceElevated)
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(playlist.thumbnailUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = playlist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .background(
                                                color = PureTheme.colors.background.copy(alpha = 0.85f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                                                contentDescription = null,
                                                tint = PureTheme.colors.textPrimary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${playlist.streamCount}",
                                                color = PureTheme.colors.textPrimary,
                                                style = PureTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = playlist.title,
                                        style = PureTheme.typography.titleMedium,
                                        color = PureTheme.colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = "${playlist.streamCount} مقطع فيديو",
                                        style = PureTheme.typography.bodyMedium,
                                        color = PureTheme.colors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
