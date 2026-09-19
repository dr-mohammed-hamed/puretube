package com.dr.tech.puretube.features.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.media3.common.Player
import com.dr.tech.puretube.features.player.PlayerUiState

/**
 * Encapsulated viewport composable stacking [PlayerVideoSurface],
 * [PureHairlineProgressBar], and [PlayerControlsOverlay].
 * Conforms to Constitution Principles V, VI, VII:
 * < 150 LOC, zero emojis, PureTheme semantic tokens, zero business logic.
 */
@Composable
fun PlayerViewport(
    player: Player,
    uiState: PlayerUiState,
    areControlsVisible: Boolean,
    isScrubbing: Boolean,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier,
    onSingleTap: () -> Unit,
    onDoubleTapSeek: (Long) -> Unit,
    onPlayPauseClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSeekRelative: (Long) -> Unit,
    onScrubbingChange: (Boolean) -> Unit,
    onAudioOnlyToggle: () -> Unit,
    onSpeedClick: () -> Unit,
    onQualityClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onZoomToFillToggle: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onPipClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Box(modifier = modifier) {
        PlayerVideoSurface(
            player = player,
            isAudioOnly = uiState.isAudioOnly,
            isZoomToFill = uiState.isZoomToFill,
            modifier = Modifier.fillMaxSize(),
            onSingleTap = onSingleTap,
            onDoubleTapSeek = onDoubleTapSeek
        )

        PureHairlineProgressBar(
            currentPositionMs = uiState.currentPositionMs,
            durationMs = uiState.durationMs,
            isVisible = !areControlsVisible && !uiState.isLoading && uiState.errorMessage == null,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        PlayerControlsOverlay(
            isVisible = areControlsVisible,
            isPlaying = uiState.isPlaying,
            isBuffering = uiState.isBuffering,
            isAudioOnly = uiState.isAudioOnly,
            currentPositionMs = uiState.currentPositionMs,
            durationMs = uiState.durationMs,
            currentQuality = uiState.currentQuality,
            playbackSpeed = uiState.playbackSpeed,
            isZoomToFill = uiState.isZoomToFill,
            activeSleepTimer = uiState.activeSleepTimer,
            videoTitle = uiState.videoDetails?.title.orEmpty(),
            channelName = uiState.videoDetails?.channelName.orEmpty(),
            isFullscreen = isFullscreen,
            onPlayPauseClick = onPlayPauseClick,
            onSeekTo = onSeekTo,
            onSeekRelative = onSeekRelative,
            onScrubbingChange = onScrubbingChange,
            onAudioOnlyToggle = onAudioOnlyToggle,
            onSpeedClick = onSpeedClick,
            onQualityClick = onQualityClick,
            onSleepTimerClick = onSleepTimerClick,
            onZoomToFillToggle = onZoomToFillToggle,
            onFullscreenToggle = onFullscreenToggle,
            onPipClick = onPipClick,
            onBackClick = onBackClick
        )
    }
}
