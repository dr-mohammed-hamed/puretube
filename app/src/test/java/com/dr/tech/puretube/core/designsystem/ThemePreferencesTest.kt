package com.dr.tech.puretube.core.designsystem

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.core.designsystem.theme.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit test for ThemePreferences persistence and StateFlow emissions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ThemePreferencesTest {

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear preferences before each test
        context.getSharedPreferences("puretube_theme_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        themePreferences = ThemePreferences(context)
    }

    @Test
    fun defaultPreset_isEmeraldNight() {
        assertEquals(ThemePreset.EMERALD_NIGHT, themePreferences.currentPreset.value)
    }

    @Test
    fun setPreset_updatesStateFlowAndPersists() {
        themePreferences.setPreset(ThemePreset.ROYAL_INDIGO)
        assertEquals(ThemePreset.ROYAL_INDIGO, themePreferences.currentPreset.value)

        // Create new instance from same context to verify persistence
        val newInstance = ThemePreferences(context)
        assertEquals(ThemePreset.ROYAL_INDIGO, newInstance.currentPreset.value)
    }

    @Test
    fun setPreset_allPresetsCanBePersisted() {
        ThemePreset.entries.forEach { preset ->
            themePreferences.setPreset(preset)
            assertEquals(preset, themePreferences.currentPreset.value)
        }
    }
}
