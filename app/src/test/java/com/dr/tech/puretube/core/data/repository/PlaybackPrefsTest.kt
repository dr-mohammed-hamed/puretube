package com.dr.tech.puretube.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dr.tech.puretube.core.data.model.PlaybackQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test for PlaybackPrefs defaults, persistence, and value coercion.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlaybackPrefsTest {

    private lateinit var context: Context
    private lateinit var prefs: PlaybackPrefs

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("puretube_playback_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        prefs = PlaybackPrefs(context)
    }

    @Test
    fun defaults_matchSpec() {
        assertEquals(1f, prefs.speed.value)
        assertEquals(PlaybackQuality.AUTO, prefs.quality.value)
        assertNull(prefs.captionLang.value)
        assertFalse(prefs.ccEnabled.value)
    }

    @Test
    fun setters_persistAndEmit() {
        prefs.setSpeed(1.5f)
        prefs.setQuality(PlaybackQuality.SD_240)
        prefs.setCaptionLang("ar")
        prefs.setCcEnabled(true)

        assertEquals(1.5f, prefs.speed.value)
        assertEquals(PlaybackQuality.SD_240, prefs.quality.value)
        assertEquals("ar", prefs.captionLang.value)
        assertTrue(prefs.ccEnabled.value)

        val reloaded = PlaybackPrefs(context)
        assertEquals(1.5f, reloaded.speed.value)
        assertEquals(PlaybackQuality.SD_240, reloaded.quality.value)
        assertEquals("ar", reloaded.captionLang.value)
        assertTrue(reloaded.ccEnabled.value)
    }

    @Test
    fun setSpeed_coercesToRange() {
        prefs.setSpeed(5f)
        assertEquals(2.0f, prefs.speed.value)

        prefs.setSpeed(0.1f)
        assertEquals(0.5f, prefs.speed.value)

        val reloaded = PlaybackPrefs(context)
        assertEquals(0.5f, reloaded.speed.value)
    }

    @Test
    fun unknownPersistedQuality_fallsBackToAuto() {
        context.getSharedPreferences("puretube_playback_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("playback_quality", "ULTRA_8K")
            .commit()
        val reloaded = PlaybackPrefs(context)
        assertEquals(PlaybackQuality.AUTO, reloaded.quality.value)
    }

    @Test
    fun setCaptionLang_nullClearsValue() {
        prefs.setCaptionLang("en")
        assertEquals("en", prefs.captionLang.value)
        prefs.setCaptionLang(null)
        assertNull(prefs.captionLang.value)

        prefs.setCaptionLang("   ")
        assertNull(prefs.captionLang.value)
    }

    @Test
    fun allQualities_canBePersisted() {
        PlaybackQuality.entries.forEach { quality ->
            prefs.setQuality(quality)
            assertEquals(quality, prefs.quality.value)
        }
        val reloaded = PlaybackPrefs(context)
        assertEquals(PlaybackQuality.SD_144, reloaded.quality.value)
    }
}
