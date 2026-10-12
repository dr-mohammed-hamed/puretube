package com.dr.tech.puretube.features.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.player.sheets.SleepTimerOption

/**
 * Overlay rendering custom player controls, timeline slider, video title, and action buttons.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, PureTheme semantic tokens, safe insets, < 250 LOC.
 */
@Composable
fun PlayerControlsOverlay(
    isVisible: Boolean,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isAudioOnly: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    currentQuality: PlaybackQuality,
    playbackSpeed: Float,
    isZoomToFill: Boolean = false,
    activeSleepTimer: SleepTimerOption = SleepTimerOption.OFF,
    videoTitle: String = "",
    channelName: String = "",
    isFullscreen: Boolean = false,
    modifier: Modifier = Modifier,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onScrubbingChange: (Boolean) -> Unit = {},
    onAudioOnlyToggle: () -> Unit = {},
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSleepTimerClick: () -> Unit = {},
    onZoomToFillToggle: () -> Unit = {},
    onFullscreenToggle: () -> Unit = {},
    onPipClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val insetsModifier = if (isFullscreen) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureTheme.colors.background.copy(alpha = 0.65f))
                .then(insetsModifier)
        ) {
            // 1. Top Bar: Navigation, Video Info (in fullscreen), & Player Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع",
                            tint = PureTheme.colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (isFullscreen && videoTitle.isNotBlank()) {
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = videoTitle,
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
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    OverlayIconButton(
                        icon = Icons.Default.Speed,
                        contentDescription = "السرعة",
                        onClick = onSpeedClick
                    )
                    OverlayIconButton(
                        icon = Icons.Default.HighQuality,
                        contentDescription = "الجودة",
                        onClick = onQualityClick
                    )
                    OverlayIconButton(
                        icon = Icons.Default.Bedtime,
                        contentDescription = "مؤقت النوم",
                        tint = if (activeSleepTimer != SleepTimerOption.OFF) PureTheme.colors.secondary else PureTheme.colors.textSecondary,
                        onClick = onSleepTimerClick
                    )
                    OverlayIconButton(
                        icon = if (isFullscreen) Icons.Default.FitScreen else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "ملاءمة الشاشة" else "ملء الشاشة",
                        tint = if (isFullscreen && isZoomToFill) PureTheme.colors.primary else PureTheme.colors.textSecondary,
                        onClick = {
                            if (isFullscreen) onZoomToFillToggle() else onFullscreenToggle()
                        }
                    )
                    OverlayIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        onClick = onMoreOptionsClick
                    )
                }
            }

            // 2. Center: Play/Pause/Replay Controls
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSeekRelative(-10_000L) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "تأخير 10 ثوان",
                        tint = PureTheme.colors.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(PureTheme.colors.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            color = PureTheme.colors.background,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = PureTheme.colors.background,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onSeekRelative(10_000L) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "تقديم 10 ثوان",
                        tint = PureTheme.colors.textPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // 3. Bottom: Scrubber Slider, Time Labels & Fullscreen Toggle
            PlayerTimelineBar(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isFullscreen = isFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = if (isFullscreen) 12.dp else 4.dp,
                        end = if (isFullscreen) 12.dp else 4.dp,
                        bottom = if (isFullscreen) 6.dp else 0.dp
                    ),
                onSeekTo = onSeekTo,
                onScrubbingChange = onScrubbingChange,
                onFullscreenToggle = onFullscreenToggle
            )
        }
    }
}

@Composable
private fun OverlayIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color = PureTheme.colors.textSecondary,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
