package com.dr.tech.puretube.features.player.sheets

import androidx.compose.runtime.Composable
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import com.dr.tech.puretube.features.player.CaptionUiState
import com.dr.tech.puretube.features.player.PlayerUiState

/**
 * Modal sheets host for player settings: Playback Speed, Quality,
 * Captions and Sleep Timer.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, PureTheme tokens, < 100 LOC.
 */
@Composable
fun PlayerModalSheets(
    uiState: PlayerUiState,
    captionState: CaptionUiState,
    isSpeedSheetOpen: Boolean,
    isQualitySheetOpen: Boolean,
    isCaptionSheetOpen: Boolean,
    isSleepTimerSheetOpen: Boolean,
    onDismissSpeed: () -> Unit,
    onDismissQuality: () -> Unit,
    onDismissCaption: () -> Unit,
    onDismissSleepTimer: () -> Unit,
    onSpeedSelected: (Float) -> Unit,
    onQualitySelected: (PlaybackQuality) -> Unit,
    onCcToggle: (Boolean) -> Unit,
    onCaptionLanguageSelected: (String?) -> Unit,
    onSleepTimerSelected: (SleepTimerOption) -> Unit
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

    if (isCaptionSheetOpen) {
        CaptionSelectionSheet(
            ccEnabled = captionState.ccEnabled,
            selectedLang = captionState.selectedLang,
            tracks = captionState.tracks,
            onCcToggle = onCcToggle,
            onLanguageSelected = onCaptionLanguageSelected,
            onDismiss = onDismissCaption
        )
    }

    if (isSleepTimerSheetOpen) {
        SleepTimerSheet(
            activeOption = uiState.activeSleepTimer,
            onDismiss = onDismissSleepTimer,
            onOptionSelected = onSleepTimerSelected
        )
    }
}
