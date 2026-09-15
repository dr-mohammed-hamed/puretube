package com.dr.tech.puretube

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.features.navigation.MainNavigationShell
import org.koin.android.ext.android.inject

/**
 * Main Activity hosting the permanent PureTube navigation shell (Constitution Principle V).
 * Replaces the temporary milestone smoke-test harness with the production MainNavigationShell.
 */
class MainActivity : ComponentActivity() {

    private val themePreferences: ThemePreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentPreset by themePreferences.currentPreset.collectAsStateWithLifecycle()

            PureTheme(preset = currentPreset) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureTheme.colors.background
                ) {
                    MainNavigationShell()
                }
            }
        }
    }
}
