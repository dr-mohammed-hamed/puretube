package com.dr.tech.puretube.features.watchlater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Mindful Watch Later screen.
 */
data class WatchLaterUiState(
    val isLoading: Boolean = true,
    val items: List<WatchLaterEntity> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel managing bookmarks saved for intentional postponed viewing.
 */
class WatchLaterViewModel(
    private val watchLaterRepository: WatchLaterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WatchLaterUiState())
    val uiState: StateFlow<WatchLaterUiState> = _uiState.asStateFlow()

    init {
        observeWatchLaterItems()
    }

    private fun observeWatchLaterItems() {
        viewModelScope.launch {
            watchLaterRepository.getWatchLaterFlow().collect { list ->
                _uiState.update {
                    it.copy(
                        items = list,
                        isLoading = false
                    )
                }
            }
        }
    }

    /**
     * Removes an item from Watch Later by videoId.
     */
    fun removeItem(videoId: String) {
        viewModelScope.launch {
            watchLaterRepository.removeFromWatchLater(videoId)
        }
    }

    /**
     * Clears all saved Watch Later bookmarks.
     */
    fun clearAll() {
        viewModelScope.launch {
            watchLaterRepository.clearAll()
        }
    }
}
