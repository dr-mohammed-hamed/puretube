package com.dr.tech.puretube.features.player

import com.dr.tech.puretube.core.data.repository.HistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Periodic playback-position sync loop extracted from PlayerViewModel.
 * Constitution V/VI/IX: viewModelScope only (scope injected by caller),
 * debounce handled by caller, zero double-bang, Long ms everywhere, no Context,
 * no Compose. Failures are best-effort but observable via [onSyncError].
 */
class PlayerPlaybackSyncHelper(
    private val historyRepository: HistoryRepository,
    private val onSyncError: (Throwable) -> Unit = { e ->
        android.util.Log.w("PlayerPlaybackSyncHelper", "history sync failed", e)
    }
) {

    private var historySyncJob: Job? = null

    val isSyncing: Boolean
        get() = historySyncJob?.isActive == true

    fun start(
        scope: CoroutineScope,
        videoIdProvider: () -> String?,
        positionProvider: () -> Long,
        durationProvider: () -> Long,
        isPlayingProvider: () -> Boolean,
        intervalMs: Long = 5_000L
    ) {
        require(intervalMs >= 1_000L) { "intervalMs must be >= 1000, was $intervalMs" }
        historySyncJob?.cancel()
        historySyncJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                try {
                    val vid = videoIdProvider()?.takeIf { it.isNotBlank() } ?: continue
                    val pos = positionProvider()
                    val dur = durationProvider()
                    if (isPlayingProvider() && pos > 0L) {
                        historyRepository.updatePlaybackPosition(vid, pos)
                        if (dur > 0L && pos >= dur * 95L / 100L) {
                            historyRepository.markCompleted(vid)
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    onSyncError(e)
                }
            }
        }
    }

    fun persistAsync(scope: CoroutineScope, videoId: String?, positionMs: Long) {
        val vid = videoId?.takeIf { it.isNotBlank() } ?: return
        if (positionMs <= 0L) return
        scope.launch(NonCancellable) {
            try {
                historyRepository.updatePlaybackPosition(vid, positionMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onSyncError(e)
            }
        }
    }

    fun stop() {
        historySyncJob?.cancel()
        historySyncJob = null
    }
}
