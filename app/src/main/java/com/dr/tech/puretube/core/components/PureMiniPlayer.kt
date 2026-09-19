package com.dr.tech.puretube.core.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.player.PurePlayerManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Persistent MiniPlayer docked above the bottom navigation bar.
 * Follows YouTube/NewPipe standard: allows seamless continuous playback while browsing all screens.
 * Conforms to Constitution Principles I, V, VI, VII (Zero Emojis, PureTheme tokens, Cairo typography).
 */
@Composable
fun PureMiniPlayer(
    isVisible: Boolean,
    title: String,
    channelName: String,
    thumbnailUrl: String?,
    isPlaying: Boolean,
    progress: Float,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PureTheme.colors.surfaceElevated)
                .border(
                    width = 1.dp,
                    color = PureTheme.colors.borderOutline,
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .clickable(onClick = onClick)
        ) {
            // 1. Slim Emerald Progress Bar along the top edge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(PureTheme.colors.borderOutline)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(
                            current = progress.coerceIn(0f, 1f),
                            range = 0f..1f
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(PureTheme.colors.primary)
                )
            }

            // 2. Mini Content Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(PureTheme.colors.surface)
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Channel
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = title.ifBlank { "تشغيل جاري" },
                        style = PureTheme.typography.bodyMedium,
                        color = PureTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (channelName.isNotBlank()) {
                        Text(
                            text = channelName,
                            style = PureTheme.typography.labelSmall,
                            color = PureTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Controls
                IconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                        tint = PureTheme.colors.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "إغلاق المشغل",
                        tint = PureTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * State container for [PureMiniPlayer] to encapsulate high-frequency (500ms) playback
 * updates and prevent recomposition leaks into the root Activity or active screens.
 * Conforms to Constitution Principles V, VI, VII.
 */
@Composable
fun PureMiniPlayerContainer(
    playerManager: PurePlayerManager,
    onOpenPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDetails by playerManager.currentVideoDetails.collectAsStateWithLifecycle()
    val isPlaying by playerManager.isPlaying.collectAsStateWithLifecycle()
    val positionMs by playerManager.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by playerManager.durationMs.collectAsStateWithLifecycle()
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    PureMiniPlayer(
        isVisible = currentDetails != null,
        title = currentDetails?.title ?: "",
        channelName = currentDetails?.channelName ?: "",
        thumbnailUrl = currentDetails?.thumbnailUrl ?: currentDetails?.channelAvatarUrl,
        isPlaying = isPlaying,
        progress = progress,
        onPlayPauseClick = {
            if (isPlaying) playerManager.pause() else playerManager.play()
        },
        onCloseClick = {
            playerManager.stop()
        },
        onClick = {
            currentDetails?.let { details ->
                onOpenPlayer(details.videoId)
            }
        },
        modifier = modifier
    )
}
