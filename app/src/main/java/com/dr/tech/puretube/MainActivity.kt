package com.dr.tech.puretube

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.features.navigation.MainNavigationShell
import com.dr.tech.puretube.features.player.PlayerScreen
import com.dr.tech.puretube.player.PurePlayerManager
import org.koin.android.ext.android.inject

/**
 * Main Activity hosting PureTube navigation and Media3 Picture-in-Picture lifecycle.
 * Conforms to Constitution Principles V, VI, VII, VIII, IX.
 */
class MainActivity : ComponentActivity() {

    private val themePreferences: ThemePreferences by inject()
    private val playerManager: PurePlayerManager by inject()

    private val isInPipModeState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentPreset by themePreferences.currentPreset.collectAsStateWithLifecycle()
            var activeVideoId by rememberSaveable { mutableStateOf<String?>(null) }

            PureTheme(preset = currentPreset) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureTheme.colors.background
                ) {
                    val currentActiveId = activeVideoId
                    if (currentActiveId != null) {
                        PlayerScreen(
                            videoId = currentActiveId,
                            isInPipMode = isInPipModeState.value,
                            onEnterPip = { enterPipMode() },
                            onBackClick = {
                                if (!playerManager.isAudioOnly.value) {
                                    playerManager.pause()
                                }
                                activeVideoId = null
                            }
                        )
                    } else {
                        MainNavigationShell(
                            onVideoClick = { videoId ->
                                activeVideoId = videoId
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (playerManager.isPlaying.value && !playerManager.isAudioOnly.value) {
            enterPipMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipModeState.value = isInPictureInPictureMode
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val builder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
            // Auto-enter intentionally off: enter PiP only via explicit user action
            // or onUserLeaveHint to avoid sticky double-enter behavior.
            enterPictureInPictureMode(builder.build())
        }
    }
}
