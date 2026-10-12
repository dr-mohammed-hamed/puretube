package com.dr.tech.puretube.features.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.repository.PlaylistRepository
import com.dr.tech.puretube.core.database.entity.PlaylistEntity
import com.dr.tech.puretube.core.database.entity.PlaylistItemEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class PlaylistImportState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successPlaylistId: Long? = null
)

/**
 * Playlist detail ViewModel: selected-playlist items flow plus
 * CRUD, membership helpers and YouTube-URL import state.
 */
class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    private val coroutineExceptionHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("PlaylistViewModel", "Uncaught playlist coroutine exception", throwable)
        _actionError.value = "حدث خطأ غير متوقع أثناء معالجة القائمة"
    }

    val playlists: StateFlow<List<PlaylistEntity>> =
        playlistRepository.getPlaylistsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<PlaylistItemEntity>> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList()) else playlistRepository.getItemsFlow(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    private val _importState = MutableStateFlow(PlaylistImportState())
    val importState: StateFlow<PlaylistImportState> = _importState.asStateFlow()

    private val togglingPlaylist = ConcurrentHashMap.newKeySet<Pair<Long, String>>()
    private val creatingPlaylist = AtomicBoolean(false)

    fun selectPlaylist(playlistId: Long?) {
        _selectedPlaylistId.value = playlistId
    }

    fun createPlaylist(name: String, onCreated: (Long) -> Unit = {}) {
        if (!creatingPlaylist.compareAndSet(false, true)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                playlistRepository.createPlaylist(name).onSuccess { id ->
                    _actionError.value = null
                    onCreated(id)
                }.onFailure {
                    _actionError.value = "تعذر إنشاء القائمة"
                }
            } finally {
                creatingPlaylist.set(false)
            }
        }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            playlistRepository.renamePlaylist(playlistId, name).onSuccess {
                _actionError.value = null
            }.onFailure {
                _actionError.value = "تعذر إعادة التسمية"
            }
        }
    }

    fun deletePlaylist(playlistId: Long, onDeleted: () -> Unit = {}) {
        viewModelScope.launch(coroutineExceptionHandler) {
            playlistRepository.deletePlaylist(playlistId).onSuccess {
                _actionError.value = null
                onDeleted()
            }.onFailure {
                _actionError.value = "تعذر حذف القائمة"
            }
        }
    }

    fun clearPlaylist(playlistId: Long) {
        viewModelScope.launch(coroutineExceptionHandler) {
            playlistRepository.clearPlaylist(playlistId).onFailure {
                _actionError.value = "تعذر إفراغ القائمة"
            }
        }
    }

    fun removeFromPlaylist(playlistId: Long, videoId: String) {
        viewModelScope.launch(coroutineExceptionHandler) {
            playlistRepository.removeFromPlaylist(playlistId, videoId).onFailure {
                _actionError.value = "تعذر حذف العنصر"
            }
        }
    }

    fun toggleInPlaylist(
        playlistId: Long,
        videoId: String,
        title: String,
        channelId: String,
        channelName: String,
        thumbnailUrl: String? = null,
        durationMs: Long = 0L,
        currentlyIn: Boolean,
        onDone: (Boolean) -> Unit = {}
    ) {
        val toggleKey = playlistId to videoId
        if (!togglingPlaylist.add(toggleKey)) return
        viewModelScope.launch(coroutineExceptionHandler) {
            try {
                if (currentlyIn) {
                    playlistRepository.removeFromPlaylist(playlistId, videoId).onSuccess {
                        onDone(false)
                    }.onFailure {
                        _actionError.value = "تعذر حذف العنصر"
                    }
                } else {
                    playlistRepository.addToPlaylist(
                        playlistId = playlistId,
                        videoId = videoId,
                        title = title,
                        channelId = channelId,
                        channelName = channelName,
                        thumbnailUrl = thumbnailUrl,
                        durationMs = durationMs
                    ).onSuccess {
                        onDone(true)
                    }.onFailure {
                        _actionError.value = "تعذر الإضافة إلى القائمة"
                    }
                }
            } finally {
                togglingPlaylist.remove(toggleKey)
            }
        }
    }

    suspend fun membershipFor(videoId: String): Set<Long> {
        if (videoId.isBlank()) return emptySet()
        return playlistRepository.getPlaylistIdsForVideo(videoId)
    }

    fun importFromUrl(url: String, onImported: (Long) -> Unit = {}) {
        if (_importState.value.isLoading) return
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _importState.update { it.copy(errorMessage = "رابط القائمة فارغ") }
            return
        }
        _importState.update { it.copy(isLoading = true, errorMessage = null, successPlaylistId = null) }
        viewModelScope.launch(coroutineExceptionHandler) {
            playlistRepository.importFromYoutubeUrl(trimmed).onSuccess { id ->
                _importState.update {
                    it.copy(isLoading = false, errorMessage = null, successPlaylistId = id)
                }
                onImported(id)
            }.onFailure {
                _importState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "تعذر استيراد القائمة، تحقق من صحة الرابط والاتصال"
                    )
                }
            }
        }
    }

    fun consumeImportSuccess() {
        _importState.update { it.copy(successPlaylistId = null) }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    fun clearImportError() {
        _importState.update { it.copy(errorMessage = null) }
    }
}
