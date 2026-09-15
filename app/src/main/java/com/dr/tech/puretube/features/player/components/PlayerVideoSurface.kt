package com.dr.tech.puretube.features.player.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import kotlinx.coroutines.delay

/**
 * Surface wrapping Media3 PlayerView with gesture detection and animated seek ripples.
 * Conforms to Constitution Principles V, VI, and VII (Zero Emojis, Cairo typography, PureTheme).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerVideoSurface(
    player: Player,
    isAudioOnly: Boolean,
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Long) -> Unit
) {
    var seekIndicatorText by remember { mutableStateOf<String?>(null) }
    var isForwardSeek by remember { mutableStateOf(true) }
    val currentOnSingleTap by rememberUpdatedState(onSingleTap)
    val currentOnDoubleTapSeek by rememberUpdatedState(onDoubleTapSeek)

    LaunchedEffect(seekIndicatorText) {
        if (seekIndicatorText != null) {
            delay(650)
            seekIndicatorText = null
        }
    }

    Box(
        modifier = modifier
            .background(PureTheme.colors.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { currentOnSingleTap() },
                    onDoubleTap = { offset ->
                        val isRightSide = offset.x > (size.width / 2)
                        // In RTL Arabic layout, right side = forward seek (+10s), left side = rewind (-10s)
                        if (isRightSide) {
                            isForwardSeek = true
                            seekIndicatorText = "+10"
                            currentOnDoubleTapSeek(10_000L)
                        } else {
                            isForwardSeek = false
                            seekIndicatorText = "-10"
                            currentOnDoubleTapSeek(-10_000L)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val isRtl = androidx.compose.ui.platform.LocalLayoutDirection.current == androidx.compose.ui.unit.LayoutDirection.Rtl
        val indicatorAlignment = if (isRtl) {
            if (isForwardSeek) Alignment.CenterStart else Alignment.CenterEnd
        } else {
            if (isForwardSeek) Alignment.CenterEnd else Alignment.CenterStart
        }

        if (isAudioOnly) {
            // Audio-only mode surface placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(PureTheme.colors.surfaceElevated, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = "وضع الاستماع الصوتي",
                        tint = PureTheme.colors.primary,
                        modifier = Modifier.size(44.dp)
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "وضع الاستماع الصوتي يعمل بنقاء",
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary
                )
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        this.player = player
                    }
                },
                update = { view ->
                    if (view.player != player) {
                        view.player = player
                    }
                },
                onRelease = { view ->
                    view.player = null
                }
            )
        }

        // Animated double-tap seek feedback indicator
        AnimatedVisibility(
            visible = seekIndicatorText != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(indicatorAlignment)
        ) {
            seekIndicatorText?.let { text ->
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(PureTheme.colors.background.copy(alpha = 0.75f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isForwardSeek) Icons.Default.FastForward else Icons.Default.FastRewind,
                            contentDescription = null,
                            tint = PureTheme.colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = text,
                            color = PureTheme.colors.textPrimary,
                            style = PureTheme.typography.labelSmall.copy(fontSize = 13.sp)
                        )
                    }
                }
            }
        }
    }
}
