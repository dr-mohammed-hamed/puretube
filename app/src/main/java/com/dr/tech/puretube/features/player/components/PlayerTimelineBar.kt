package com.dr.tech.puretube.features.player.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import java.util.Locale

/**
 * YouTube-style bottom timeline scrubber bar with circular dot thumb,
 * duration labels, and fullscreen toggle.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, PureTheme semantic tokens, < 150 LOC, LTR media orientation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTimelineBar(
    currentPositionMs: Long,
    durationMs: Long,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    onSeekTo: (Long) -> Unit,
    onScrubbingChange: (Boolean) -> Unit = {},
    onFullscreenToggle: () -> Unit
) {
    var draggingPositionMs by remember { mutableStateOf<Float?>(null) }
    val isScrubbing = draggingPositionMs != null
    val durationFloat = remember(durationMs) { durationMs.toFloat().coerceAtLeast(1f) }
    val sliderPosition = draggingPositionMs ?: currentPositionMs.toFloat().coerceIn(0f, durationFloat)
    val displayPositionMs = (draggingPositionMs?.toLong() ?: currentPositionMs).coerceIn(0L, durationMs.coerceAtLeast(0L))

    val animatedThumbSize by animateDpAsState(
        targetValue = if (isScrubbing) 14.dp else 11.dp,
        label = "thumbSize"
    )

    // Force LTR layout direction so the timeline and timestamps follow universal media player layout
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            // 1. Time Display & Fullscreen Toggle Row (Above Scrubber)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Timestamps (Current / Total)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(displayPositionMs),
                        style = PureTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = PureTheme.colors.textPrimary
                    )
                    Text(
                        text = "/",
                        style = PureTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = PureTheme.colors.textSecondary
                    )
                    Text(
                        text = formatTime(durationMs),
                        style = PureTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = PureTheme.colors.textSecondary
                    )
                }

                // Right: Fullscreen Action
                IconButton(
                    onClick = onFullscreenToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = if (isFullscreen) "إنهاء ملء الشاشة" else "ملء الشاشة",
                        tint = PureTheme.colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. Slim Scrubber with Circular Dot Thumb (YouTube Style)
            Slider(
                value = sliderPosition,
                onValueChange = {
                    if (draggingPositionMs == null) {
                        onScrubbingChange(true)
                    }
                    draggingPositionMs = it
                },
                onValueChangeFinished = {
                    val targetPos = draggingPositionMs
                    draggingPositionMs = null
                    onScrubbingChange(false)
                    targetPos?.let { onSeekTo(it.toLong()) }
                },
                valueRange = 0f..durationFloat,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(animatedThumbSize)
                            .shadow(if (isScrubbing) 4.dp else 1.dp, CircleShape)
                            .background(PureTheme.colors.primary, CircleShape)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(if (isScrubbing) 4.5.dp else 3.dp),
                        colors = SliderDefaults.colors(
                            activeTrackColor = PureTheme.colors.primary,
                            inactiveTrackColor = PureTheme.colors.borderOutline.copy(alpha = 0.8f)
                        ),
                        drawStopIndicator = null,
                        thumbTrackGapSize = 0.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

/**
 * Ultra-thin persistent hairline progress bar (YouTube style).
 * Rendered along the player's bottom edge when controls overlay is hidden.
 */
@Composable
fun PureHairlineProgressBar(
    currentPositionMs: Long,
    durationMs: Long,
    isVisible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            val durationFloat = durationMs.toFloat().coerceAtLeast(1f)
            val progressRatio = (currentPositionMs.toFloat() / durationFloat).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(PureTheme.colors.borderOutline.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressRatio)
                        .background(PureTheme.colors.primary)
                )
            }
        }
    }
}

fun formatTime(millis: Long): String {
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
