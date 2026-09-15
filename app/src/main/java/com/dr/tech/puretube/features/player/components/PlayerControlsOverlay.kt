package com.dr.tech.puretube.features.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import java.util.Locale

/**
 * Overlay rendering custom player controls, timeline slider, and action buttons.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, PureTheme semantic tokens, < 300 LOC.
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
    modifier: Modifier = Modifier,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onAudioOnlyToggle: () -> Unit,
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onPipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var draggingPositionMs by remember { mutableStateOf<Float?>(null) }
    val durationFloat = remember(durationMs) { durationMs.toFloat().coerceAtLeast(1f) }
    val sliderPosition = draggingPositionMs ?: currentPositionMs.toFloat().coerceIn(0f, durationFloat)
    val displayPositionMs = (draggingPositionMs?.toLong() ?: currentPositionMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PureTheme.colors.background.copy(alpha = 0.65f))
                .padding(8.dp)
        ) {
            // 1. Top Bar: Navigation & Player Options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = PureTheme.colors.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio-Only Toggle
                    IconButton(onClick = onAudioOnlyToggle) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "صوتي فقط",
                            tint = if (isAudioOnly) PureTheme.colors.primary else PureTheme.colors.textSecondary
                        )
                    }

                    // Quality Selection
                    IconButton(onClick = onQualityClick) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = "الجودة",
                            tint = PureTheme.colors.textSecondary
                        )
                    }

                    // Playback Speed Selection
                    IconButton(onClick = onSpeedClick) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "السرعة",
                            tint = PureTheme.colors.textSecondary
                        )
                    }

                    // Picture-in-Picture
                    IconButton(onClick = onPipClick) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "صورة داخل صورة",
                            tint = PureTheme.colors.textSecondary
                        )
                    }
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

            // 3. Bottom: Scrubber Slider and Time Labels
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
            ) {
                Slider(
                    value = sliderPosition,
                    onValueChange = { draggingPositionMs = it },
                    onValueChangeFinished = {
                        draggingPositionMs?.let { onSeekTo(it.toLong()) }
                        draggingPositionMs = null
                    },
                    valueRange = 0f..durationFloat,
                    colors = SliderDefaults.colors(
                        thumbColor = PureTheme.colors.primary,
                        activeTrackColor = PureTheme.colors.primary,
                        inactiveTrackColor = PureTheme.colors.borderOutline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(displayPositionMs),
                        style = PureTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = PureTheme.colors.textPrimary
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = PureTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = PureTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
