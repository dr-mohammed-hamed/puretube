package com.dr.tech.puretube.features.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.PlaylistRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.HistoryEntity
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

data class LibraryUiState(
    val isLoading: Boolean = true,
    val history: List<HistoryEntity> = emptyList(),
    val watchLater: List<WatchLaterEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList()
)

/**
 * Library ViewModel combining resume history, watch-later and playlists
 * into a single observable state. No extractor usage (offline-capable).
 */
class LibraryViewModel(
    private val historyRepository: HistoryRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("LibraryViewModel", "Uncaught library coroutine exception", throwable)
        _playlistError.value = "حدث خطأ غير متوقع أثناء معالجة المكتبة"
    }

    val uiState: StateFlow<LibraryUiState> = combine(
        historyRepository.getHistoryFlow(100),
        watchLaterRepository.getWatchLaterFlow(),
        playlistRepository.getPlaylistsFlow()
    ) { history, watchLater, playlists ->
        LibraryUiState(
            isLoading = false,
            history = history,
            watchLater = watchLater,
            playlists = playlists
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private val _playlistError = MutableStateFlow<String?>(null)
    val playlistError: StateFlow<String?> = _playlistError
    private val creatingPlaylist = AtomicBoolean(false)

    fun clearHistory() {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                historyRepository.clearAllHistory()
            } catch (e: Exception) {
                android.util.Log.w("LibraryViewModel", "Failed to clear history: ${e.message}")
            }
        }
    }

    fun deleteHistoryItem(videoId: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                historyRepository.deleteFromHistory(videoId)
            } catch (e: Exception) {
                android.util.Log.w("LibraryViewModel", "Failed to delete history item: ${e.message}")
            }
        }
    }

    fun removeWatchLater(videoId: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                watchLaterRepository.removeFromWatchLater(videoId)
            } catch (e: Exception) {
                android.util.Log.w("LibraryViewModel", "Failed to remove watch later item: ${e.message}")
            }
        }
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        if (!creatingPlaylist.compareAndSet(false, true)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                playlistRepository.createPlaylist(name).onSuccess { id ->
                    _playlistError.value = null
                    onCreated(id)
                }.onFailure {
                    _playlistError.value = "تعذر إنشاء القائمة"
                }
            } finally {
                creatingPlaylist.set(false)
            }
        }
    }

    fun clearPlaylistError() {
        _playlistError.value = null
    }
}
