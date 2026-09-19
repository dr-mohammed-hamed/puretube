package com.dr.tech.puretube

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureMiniPlayerContainer
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.features.channel.ChannelScreen
import com.dr.tech.puretube.features.navigation.MainNavigationShell
import com.dr.tech.puretube.features.player.components.PlayerVideoSurface
import com.dr.tech.puretube.features.player.PlayerScreen
import com.dr.tech.puretube.player.PurePlayerManager
import org.koin.android.ext.android.inject

/**
 * Top-level destinations supported in PureTube backstack navigation.
 */
sealed interface ScreenDestination {
    data object Shell : ScreenDestination
    data class Player(val videoId: String) : ScreenDestination
    data class Channel(val channelId: String) : ScreenDestination

    companion object {
        fun serialize(destination: ScreenDestination): String = when (destination) {
            is Shell -> "shell"
            is Player -> "player:${destination.videoId}"
            is Channel -> "channel:${destination.channelId}"
        }
        fun deserialize(value: String): ScreenDestination = when {
            value.startsWith("player:") -> Player(value.removePrefix("player:"))
            value.startsWith("channel:") -> Channel(value.removePrefix("channel:"))
            else -> Shell
        }
    }
}

/**
 * Main Activity hosting PureTube navigation, persistent MiniPlayer, and Media3 PiP.
 * Conforms to Constitution Principles V, VI, VII, VIII, IX.
 */
class MainActivity : ComponentActivity() {

    private val themePreferences: ThemePreferences by inject()
    private val playerManager: PurePlayerManager by inject()

    private val isInPipModeState = mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* User response handled safely; foreground service can operate with or without heads-up display */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestNotificationPermission()

        setContent {
            val currentPreset by themePreferences.currentPreset.collectAsStateWithLifecycle()

            val backstack = rememberSaveable(
                saver = listSaver(
                    save = { stateList -> stateList.map { ScreenDestination.serialize(it) } },
                    restore = { rawList ->
                        mutableStateListOf<ScreenDestination>().apply {
                            addAll(rawList.map { ScreenDestination.deserialize(it) })
                        }
                    }
                )
            ) { mutableStateListOf<ScreenDestination>(ScreenDestination.Shell) }

            val navigateTo: (ScreenDestination) -> Unit = { destination ->
                if (backstack.lastOrNull() != destination) {
                    backstack.add(destination)
                }
            }

            val miniPlayer: @Composable () -> Unit = {
                PureMiniPlayerContainer(
                    playerManager = playerManager,
                    onOpenPlayer = { videoId ->
                        navigateTo(ScreenDestination.Player(videoId))
                    }
                )
            }

            PureTheme(preset = currentPreset) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureTheme.colors.background
                ) {
                    if (isInPipModeState.value) {
                        PlayerVideoSurface(
                            player = playerManager.player,
                            isAudioOnly = playerManager.isAudioOnly.value,
                            modifier = Modifier.fillMaxSize(),
                            onSingleTap = {},
                            onDoubleTapSeek = {}
                        )
                    } else {
                        when (val current = backstack.lastOrNull() ?: ScreenDestination.Shell) {
                            is ScreenDestination.Player -> {
                                PlayerScreen(
                                    videoId = current.videoId,
                                    isInPipMode = false,
                                    onEnterPip = { enterPipMode() },
                                    onBackClick = {
                                        if (backstack.size > 1) {
                                            backstack.removeAt(backstack.lastIndex)
                                        }
                                    },
                                    onChannelClick = { channelId ->
                                        navigateTo(ScreenDestination.Channel(channelId))
                                    }
                                )
                            }
                            is ScreenDestination.Channel -> {
                                ChannelScreen(
                                    channelId = current.channelId,
                                    onBackClick = {
                                        if (backstack.size > 1) {
                                            backstack.removeAt(backstack.lastIndex)
                                        }
                                    },
                                    onVideoClick = { videoId ->
                                        navigateTo(ScreenDestination.Player(videoId))
                                    },
                                    miniPlayerContent = miniPlayer
                                )
                            }
                            is ScreenDestination.Shell -> {
                                MainNavigationShell(
                                    onVideoClick = { videoId ->
                                        navigateTo(ScreenDestination.Player(videoId))
                                    },
                                    onChannelClick = { channelId ->
                                        navigateTo(ScreenDestination.Channel(channelId))
                                    },
                                    miniPlayerContent = miniPlayer
                                )
                            }
                        }
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

    /**
     * Manifest declares configChanges (orientation|keyboard|fontScale|uiMode|layoutDirection|locale)
     * to preserve ExoPlayer continuity. Compose recomposes automatically; non-Compose
     * resources (PlayerView, notifications) keep current instances intentionally.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val hasPip = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
            if (!hasPip) {
                // Seamless fallback: continue audio in background via PurePlaybackService
                return
            }
            try {
                val builder = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                // Auto-enter intentionally off: enter PiP only via explicit user action
                // or onUserLeaveHint to avoid sticky double-enter behavior.
                enterPictureInPictureMode(builder.build())
            } catch (e: Exception) {
                android.util.Log.w("MainActivity", "Safe fallback: PiP could not be entered", e)
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
