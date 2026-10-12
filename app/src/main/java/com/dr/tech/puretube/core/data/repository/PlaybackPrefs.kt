package com.dr.tech.puretube.core.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * تفضيلات التشغيل العامة (سرعة/جودة/لغة ترجمة/CC) — تُحفظ في SharedPreferences
 * وتُبث عبر StateFlows. يماثل بنية ThemePreferences.
 */
class PlaybackPrefs(
    context: Context
) {
    companion object {
        private const val PREFS_NAME = "puretube_playback_prefs"
        const val KEY_SPEED = "playback_speed"
        const val KEY_QUALITY = "playback_quality"
        const val KEY_CAPTION_LANG = "caption_lang"
        const val KEY_CC_ENABLED = "cc_enabled"

        const val DEFAULT_SPEED = 1f
        const val MIN_SPEED = 0.5f
        const val MAX_SPEED = 2.0f
    }

    private val prefs: SharedPreferences by lazy {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _speed = MutableStateFlow(getInitialSpeed())
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _quality = MutableStateFlow(getInitialQuality())
    val quality: StateFlow<PlaybackQuality> = _quality.asStateFlow()

    private val _captionLang = MutableStateFlow(getInitialCaptionLang())
    val captionLang: StateFlow<String?> = _captionLang.asStateFlow()

    private val _ccEnabled = MutableStateFlow(getInitialCcEnabled())
    val ccEnabled: StateFlow<Boolean> = _ccEnabled.asStateFlow()

    private fun getInitialSpeed(): Float {
        return prefs.getFloat(KEY_SPEED, DEFAULT_SPEED).coerceIn(MIN_SPEED, MAX_SPEED)
    }

    private fun getInitialQuality(): PlaybackQuality {
        val savedName = prefs.getString(KEY_QUALITY, null) ?: return PlaybackQuality.AUTO
        return try {
            PlaybackQuality.valueOf(savedName)
        } catch (_: IllegalArgumentException) {
            PlaybackQuality.AUTO
        }
    }

    private fun getInitialCaptionLang(): String? {
        return prefs.getString(KEY_CAPTION_LANG, null)?.ifBlank { null }
    }

    private fun getInitialCcEnabled(): Boolean {
        return prefs.getBoolean(KEY_CC_ENABLED, false)
    }

    fun setSpeed(value: Float) {
        val coerced = value.coerceIn(MIN_SPEED, MAX_SPEED)
        prefs.edit().putFloat(KEY_SPEED, coerced).apply()
        _speed.value = coerced
    }

    fun setQuality(quality: PlaybackQuality) {
        prefs.edit().putString(KEY_QUALITY, quality.name).apply()
        _quality.value = quality
    }

    fun setCaptionLang(languageCode: String?) {
        val normalized = languageCode?.trim()?.ifBlank { null }
        val editor = prefs.edit()
        if (normalized == null) {
            editor.remove(KEY_CAPTION_LANG)
        } else {
            editor.putString(KEY_CAPTION_LANG, normalized)
        }
        editor.apply()
        _captionLang.value = normalized
    }

    fun setCcEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CC_ENABLED, enabled).apply()
        _ccEnabled.value = enabled
    }
}
