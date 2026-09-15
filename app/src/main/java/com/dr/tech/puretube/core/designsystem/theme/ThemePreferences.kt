package com.dr.tech.puretube.core.designsystem.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persistent preferences helper for PureTube theme selection.
 * Backed by Android SharedPreferences and exposes a reactive StateFlow
 * for real-time Compose theme observation.
 */
class ThemePreferences(
    private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "puretube_theme_prefs"
        const val KEY_SELECTED_THEME_PRESET = "selected_theme_preset"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _currentPreset = MutableStateFlow(getInitialPreset())
    val currentPreset: StateFlow<ThemePreset> = _currentPreset.asStateFlow()

    private fun getInitialPreset(): ThemePreset {
        val savedName = prefs.getString(KEY_SELECTED_THEME_PRESET, null) ?: return ThemePreset.EMERALD_NIGHT
        return try {
            ThemePreset.valueOf(savedName)
        } catch (_: IllegalArgumentException) {
            ThemePreset.EMERALD_NIGHT
        }
    }

    fun setPreset(preset: ThemePreset) {
        prefs.edit().putString(KEY_SELECTED_THEME_PRESET, preset.name).apply()
        _currentPreset.value = preset
    }
}
