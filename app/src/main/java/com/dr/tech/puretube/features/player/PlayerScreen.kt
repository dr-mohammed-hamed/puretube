package com.dr.tech.puretube.features.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.R
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.player.components.MindfulRelatedList
import com.dr.tech.puretube.features.player.components.PlayerVideoSurface
import com.dr.tech.puretube.features.player.components.PlayerViewport
import com.dr.tech.puretube.features.player.components.VideoMetadataSection
import com.dr.tech.puretube.features.player.sheets.PlayerModalSheets
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * Root Player Screen composable with responsive portrait/landscape architecture.
 * Conforms to Constitution Principles I, IV, V, VI, VII:
 * Hard LOC ceiling < 300 LOC, zero emojis, PureTheme colors, PiP & immersive landscape.
 */
@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun PlayerScreen(
    videoId: String,
    modifier: Modifier = Modifier,
    isInPipMode: Boolean = false,
    viewModel: PlayerViewModel = koinViewModel(),
    onEnterPip: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onChannelClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var areControlsVisible by remember { mutableStateOf(true) }
    var isScrubbing by remember { mutableStateOf(false) }
    var isSpeedSheetOpen by remember { mutableStateOf(false) }
    var isQualitySheetOpen by remember { mutableStateOf(false) }
    var isSleepTimerSheetOpen by remember { mutableStateOf(false) }
    var isMoreOptionsSheetOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val toggleErrorText = stringResource(R.string.player_toggle_error)
    val extractErrorText = stringResource(R.string.player_error_extract)
    val genericErrorText = stringResource(R.string.player_error_generic)

    val resolvedErrorText = when (val msg = uiState.errorMessage) {
        null -> genericErrorText
        "تعذر استخراج بيانات الفيديو" -> extractErrorText
        else -> msg.ifBlank { genericErrorText }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val activity = context as? Activity

    val onToggleFullscreen: () -> Unit = {
        activity?.requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    LaunchedEffect(videoId) { viewModel.loadVideo(videoId) }

    LaunchedEffect(uiState.toggleErrorMessage) {
        val msg = uiState.toggleErrorMessage
        if (msg != null) {
            try {
                snackbarHostState.showSnackbar(msg.ifBlank { toggleErrorText })
            } finally {
                viewModel.clearToggleError()
            }
        }
    }

    // Auto-hide controls after 3.5s when video is playing and user is not scrubbing
    LaunchedEffect(areControlsVisible, uiState.isPlaying, isScrubbing) {
        if (areControlsVisible && uiState.isPlaying && !isScrubbing) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Immersive sticky fullscreen in landscape
    DisposableEffect(isLandscape) {
        val window = activity?.window
        if (window != null && isLandscape) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { insetsController.show(WindowInsetsCompat.Type.systemBars()) }
        } else {
            onDispose { }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPlaybackTracking()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val onBackAction: (Boolean) -> Unit = { isFullscreen ->
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            viewModel.persistCurrentPlaybackPosition()
            onBackClick()
        }
    }

    BackHandler(enabled = !isInPipMode) {
        onBackAction(isLandscape)
    }

    // 1. Picture-in-Picture Mode
    if (isInPipMode) {
        PlayerVideoSurface(
            player = viewModel.playerManager.player,
            isAudioOnly = uiState.isAudioOnly,
            isZoomToFill = uiState.isZoomToFill,
            modifier = Modifier.fillMaxSize(),
            onSingleTap = { areControlsVisible = !areControlsVisible },
            onDoubleTapSeek = { offsetMs -> viewModel.seekRelative(offsetMs) }
        )
        return
    }

    // 2. Landscape True Fullscreen Mode
    if (isLandscape) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(PureTheme.colors.background)
        ) {
            PlayerViewport(
                player = viewModel.playerManager.player,
                uiState = uiState,
                areControlsVisible = areControlsVisible,
                isScrubbing = isScrubbing,
                isFullscreen = true,
                modifier = Modifier.fillMaxSize(),
                onSingleTap = { areControlsVisible = !areControlsVisible },
                onDoubleTapSeek = { offsetMs -> viewModel.seekRelative(offsetMs) },
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onSeekTo = { posMs -> viewModel.seekTo(posMs) },
                onSeekRelative = { offsetMs -> viewModel.seekRelative(offsetMs) },
                onScrubbingChange = { isScrubbing = it },
                onAudioOnlyToggle = { viewModel.setAudioOnly(!uiState.isAudioOnly) },
                onSpeedClick = { isSpeedSheetOpen = true },
                onQualityClick = { isQualitySheetOpen = true },
                onSleepTimerClick = { isSleepTimerSheetOpen = true },
                onZoomToFillToggle = { viewModel.toggleZoomToFill() },
                onFullscreenToggle = onToggleFullscreen,
                onPipClick = onEnterPip,
                onMoreOptionsClick = { isMoreOptionsSheetOpen = true },
                onBackClick = { onBackAction(true) }
            )

            if (uiState.errorMessage != null) {
                PureErrorState(
                    title = stringResource(R.string.player_error_title),
                    message = resolvedErrorText,
                    onRetry = { viewModel.loadVideo(videoId, force = true) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    } else {
        // 3. Portrait Normal Mode
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .background(PureTheme.colors.background),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(PureTheme.colors.background)
            ) {
                PlayerViewport(
                    player = viewModel.playerManager.player,
                    uiState = uiState,
                    areControlsVisible = areControlsVisible,
                    isScrubbing = isScrubbing,
                    isFullscreen = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f),
                    onSingleTap = { areControlsVisible = !areControlsVisible },
                    onDoubleTapSeek = { offsetMs -> viewModel.seekRelative(offsetMs) },
                    onPlayPauseClick = { viewModel.togglePlayPause() },
                    onSeekTo = { posMs -> viewModel.seekTo(posMs) },
                    onSeekRelative = { offsetMs -> viewModel.seekRelative(offsetMs) },
                    onScrubbingChange = { isScrubbing = it },
                    onAudioOnlyToggle = { viewModel.setAudioOnly(!uiState.isAudioOnly) },
                    onSpeedClick = { isSpeedSheetOpen = true },
                    onQualityClick = { isQualitySheetOpen = true },
                    onSleepTimerClick = { isSleepTimerSheetOpen = true },
                    onZoomToFillToggle = { viewModel.toggleZoomToFill() },
                    onFullscreenToggle = onToggleFullscreen,
                    onPipClick = onEnterPip,
                    onMoreOptionsClick = { isMoreOptionsSheetOpen = true },
                    onBackClick = { onBackAction(false) }
                )

                // Body: Metadata, Loading/Error, and Related List
                val details = uiState.videoDetails
                when {
                    uiState.errorMessage != null -> {
                        PureErrorState(
                            title = stringResource(R.string.player_error_title),
                            message = resolvedErrorText,
                            onRetry = { viewModel.loadVideo(videoId, force = true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    uiState.isLoading -> {
                        PureFeedShimmerList(count = 2, modifier = Modifier.weight(1f))
                    }
                    details != null -> {
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
                                    isAudioOnly = uiState.isAudioOnly,
                                    onSubscribeToggle = { viewModel.toggleSubscription() },
                                    onWatchLaterToggle = { viewModel.toggleWatchLater() },
                                    onAudioOnlyToggle = { viewModel.setAudioOnly(!uiState.isAudioOnly) },
                                    onSleepTimerClick = { isSleepTimerSheetOpen = true },
                                    onSeekTo = { posMs -> viewModel.seekTo(posMs) },
                                    onChannelClick = { onChannelClick(details.channelId) }
                                )
                            }
                            item {
                                MindfulRelatedList(
                                    videos = uiState.relatedVideos,
                                    onVideoSelect = { nextVideoId -> viewModel.loadVideo(nextVideoId) }
                                )
                            }
                        }
                    }
                    else -> { }
                }
            }
        }
    }

    val onShareVideo: () -> Unit = {
        val shareTitle = uiState.videoDetails?.title.orEmpty()
        val shareId = uiState.videoDetails?.videoId ?: videoId
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, shareTitle)
            putExtra(android.content.Intent.EXTRA_TEXT, "https://youtu.be/$shareId")
        }
        try {
            context.startActivity(android.content.Intent.createChooser(shareIntent, "مشاركة الفيديو"))
        } catch (e: Exception) {
            android.util.Log.w("PlayerScreen", "Share failed", e)
        }
    }

    // Modal Sheets Host
    PlayerModalSheets(
        uiState = uiState,
        isSpeedSheetOpen = isSpeedSheetOpen,
        isQualitySheetOpen = isQualitySheetOpen,
        isSleepTimerSheetOpen = isSleepTimerSheetOpen,
        isMoreOptionsSheetOpen = isMoreOptionsSheetOpen,
        onDismissSpeed = { isSpeedSheetOpen = false },
        onDismissQuality = { isQualitySheetOpen = false },
        onDismissSleepTimer = { isSleepTimerSheetOpen = false },
        onDismissMoreOptions = { isMoreOptionsSheetOpen = false },
        onSpeedSelected = { viewModel.setPlaybackSpeed(it) },
        onQualitySelected = { viewModel.setQuality(it) },
        onSleepTimerSelected = { viewModel.setSleepTimer(it) },
        onPipClick = onEnterPip,
        onAudioOnlyToggle = { viewModel.setAudioOnly(!uiState.isAudioOnly) },
        onZoomToFillToggle = { viewModel.toggleZoomToFill() },
        onShareClick = onShareVideo
    )
}
