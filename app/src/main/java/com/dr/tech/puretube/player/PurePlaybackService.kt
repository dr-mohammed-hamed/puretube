package com.dr.tech.puretube.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.dr.tech.puretube.MainActivity
import org.koin.android.ext.android.inject

/**
 * Foreground MediaSessionService for audio-only and background playback.
 * Conforms to Constitution Principles II, IV, and V:
 * Enables background audio playback with lock-screen notification and headset controls.
 */
class PurePlaybackService : MediaSessionService() {

    private val playerManager: PurePlayerManager by inject()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, playerManager.player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.playbackState == androidx.media3.common.Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            // Player lifecycle is managed by the singleton PurePlayerManager; only release the session
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
