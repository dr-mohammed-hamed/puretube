package com.dr.tech.puretube.features.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.player.components.MindfulRelatedList
import com.dr.tech.puretube.features.player.components.PlayerControlsOverlay
import com.dr.tech.puretube.features.player.components.PlayerVideoSurface
import com.dr.tech.puretube.features.player.components.VideoMetadataSection
import com.dr.tech.puretube.features.player.sheets.PlaybackSpeedSheet
import com.dr.tech.puretube.features.player.sheets.QualitySelectionSheet
import org.koin.androidx.compose.koinViewModel

/**
 * Root Player Screen composable.
 * Conforms to Constitution Principles I, IV, V, VI, VII:
 * - Hard LOC ceiling < 300 LOC
 * - Zero emojis, PureTheme semantic colors
 * - Seamless PiP mode (omits overlays when in PiP)
 */
@Composable
fun PlayerScreen(
    videoId: String,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    viewModel: PlayerViewModel = koinViewModel(),
    onEnterPip: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var areControlsVisible by remember { mutableStateOf(true) }
    var isSpeedSheetOpen by remember { mutableStateOf(false) }
    var isQualitySheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPlaybackTracking() }
    }

    BackHandler(enabled = !isInPipMode) {
        viewModel.persistCurrentPlaybackPosition()
        onBackClick()
    }

    // 1. Picture-in-Picture Mode: Render ONLY raw player surface
    if (isInPipMode) {
        PlayerVideoSurface(
            player = viewModel.playerManager.player,
            isAudioOnly = uiState.isAudioOnly,
            modifier = Modifier.fillMaxSize(),
            onSingleTap = {},
            onDoubleTapSeek = {}
        )
        return
    }

    // 2. Fullscreen / Normal Mode
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureTheme.colors.background)
        ) {
            // Player Surface with Controls Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                PlayerVideoSurface(
                    player = viewModel.playerManager.player,
                    isAudioOnly = uiState.isAudioOnly,
                    modifier = Modifier.fillMaxSize(),
                    onSingleTap = { areControlsVisible = !areControlsVisible },
                    onDoubleTapSeek = { offsetMs -> viewModel.seekRelative(offsetMs) }
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
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekTo = { posMs -> viewModel.seekTo(posMs) },
                    onSeekRelative = { offsetMs -> viewModel.seekRelative(offsetMs) },
                    onAudioOnlyToggle = { viewModel.setAudioOnly(!uiState.isAudioOnly) },
                    onSpeedClick = { isSpeedSheetOpen = true },
                    onQualityClick = { isQualitySheetOpen = true },
                    onPipClick = onEnterPip,
                    onBackClick = {
                        viewModel.persistCurrentPlaybackPosition()
                        onBackClick()
                    }
                )
            }

            // Body: Metadata, Loading/Error, and Same-Channel Related List
            when {
                uiState.errorMessage != null -> {
                    PureErrorState(
                        title = "تعذر تشغيل الفيديو",
                        message = uiState.errorMessage ?: "حدث خطأ غير متوقع",
                        onRetry = { viewModel.loadVideo(videoId, force = true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                uiState.isLoading -> {
                    PureFeedShimmerList(count = 2, modifier = Modifier.weight(1f))
                }
                uiState.videoDetails != null -> {
                    uiState.videoDetails?.let { details ->
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                VideoMetadataSection(
                                    details = details,
                                    isSubscribed = uiState.isSubscribed,
                                    isSavedToWatchLater = uiState.isSavedToWatchLater,
                                    onSubscribeToggle = { viewModel.toggleSubscription() },
                                    onWatchLaterToggle = { viewModel.toggleWatchLater() }
                                )
                            }

                            item {
                                MindfulRelatedList(
                                    videos = uiState.relatedVideos,
                                    onVideoSelect = { nextVideoId ->
                                        viewModel.loadVideo(nextVideoId)
                                    }
                                )
                            }
                        }
                    }
                }
                else -> { }
            }
        }
    }

    // Modal Sheets
    if (isSpeedSheetOpen) {
        PlaybackSpeedSheet(
            currentSpeed = uiState.playbackSpeed,
            onDismiss = { isSpeedSheetOpen = false },
            onSpeedSelected = { viewModel.setPlaybackSpeed(it) }
        )
    }

    if (isQualitySheetOpen) {
        QualitySelectionSheet(
            currentQuality = uiState.currentQuality,
            onDismiss = { isQualitySheetOpen = false },
            onQualitySelected = { viewModel.setQuality(it) }
        )
    }
}
