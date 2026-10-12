package com.dr.tech.puretube.features.player.sheets

import androidx.compose.runtime.Composable
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.features.player.PlayerUiState

/**
 * Modal sheets host for player settings: Playback Speed, Quality, Sleep Timer, and More Options.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, PureTheme tokens, < 100 LOC.
 */
@Composable
fun PlayerModalSheets(
    uiState: PlayerUiState,
    isSpeedSheetOpen: Boolean,
    isQualitySheetOpen: Boolean,
    isSleepTimerSheetOpen: Boolean,
    isMoreOptionsSheetOpen: Boolean = false,
    onDismissSpeed: () -> Unit,
    onDismissQuality: () -> Unit,
    onDismissSleepTimer: () -> Unit,
    onDismissMoreOptions: () -> Unit = {},
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (PlaybackQuality) -> Unit,
    onSleepTimerSelected: (SleepTimerOption) -> Unit,
    onPipClick: () -> Unit = {},
    onAudioOnlyToggle: () -> Unit = {},
    onZoomToFillToggle: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    if (isSpeedSheetOpen) {
        PlaybackSpeedSheet(
            currentSpeed = uiState.playbackSpeed,
            onDismiss = onDismissSpeed,
            onSpeedSelected = onSpeedSelected
        )
    }

    if (isQualitySheetOpen) {
        QualitySelectionSheet(
            currentQuality = uiState.currentQuality,
            onDismiss = onDismissQuality,
            onQualitySelected = onQualitySelected
        )
    }

    if (isSleepTimerSheetOpen) {
        SleepTimerSheet(
            activeOption = uiState.activeSleepTimer,
            onDismiss = onDismissSleepTimer,
            onOptionSelected = onSleepTimerSelected
        )
    }

    if (isMoreOptionsSheetOpen) {
        PlayerMoreOptionsSheet(
            isAudioOnly = uiState.isAudioOnly,
            isZoomToFill = uiState.isZoomToFill,
            onDismiss = onDismissMoreOptions,
            onPipClick = onPipClick,
            onAudioOnlyToggle = onAudioOnlyToggle,
            onZoomToFillToggle = onZoomToFillToggle,
            onShareClick = onShareClick
        )
    }
}
