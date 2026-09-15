package com.dr.tech.puretube.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Settings & Customization screen.
 */
data class SettingsUiState(
    val currentPreset: ThemePreset = ThemePreset.EMERALD_NIGHT,
    val subscriptionCount: Int = 0,
    val watchLaterCount: Int = 0,
    val historyCount: Int = 0,
    val appVersion: String = "1.0.0"
)

/**
 * ViewModel managing theme selection persistence and local database statistics.
 * Conforms to Constitution Principles V, VII, and VIII.
 */
class SettingsViewModel(
    private val themePreferences: ThemePreferences,
    private val subscriptionRepository: SubscriptionRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val historyRepository: HistoryRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeThemePreset()
        refreshDatabaseStats()
    }

    private fun observeThemePreset() {
        viewModelScope.launch {
            themePreferences.currentPreset.collect { preset ->
                _uiState.update { it.copy(currentPreset = preset) }
            }
        }
    }

    /**
     * Refreshes offline-first Room database metrics.
     */
    fun refreshDatabaseStats() {
        viewModelScope.launch(ioDispatcher) {
            val subs = subscriptionRepository.getSubscriptionCount()
            val wl = watchLaterRepository.getWatchLaterCount()
            val hist = historyRepository.getHistoryCount()

            _uiState.update {
                it.copy(
                    subscriptionCount = subs,
                    watchLaterCount = wl,
                    historyCount = hist
                )
            }
        }
    }

    /**
     * Updates and permanently persists the active theme preset.
     */
    fun selectThemePreset(preset: ThemePreset) {
        themePreferences.setPreset(preset)
    }
}
