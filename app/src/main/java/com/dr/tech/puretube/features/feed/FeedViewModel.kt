package com.dr.tech.puretube.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.repository.FeedRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Mindful Feed screen.
 * Conforms to Constitution Principles I, IV, and VIII.
 */
data class FeedUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val videos: List<FeedVideoItem> = emptyList(),
    val bookmarkedVideoIds: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val isEmptySubscriptions: Boolean = false
)

/**
 * ViewModel managing feed aggregation, caching, pull-to-refresh,
 * and Watch Later bookmark synchronization.
 */
class FeedViewModel(
    private val feedRepository: FeedRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val watchLaterRepository: WatchLaterRepository,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState(isLoading = true))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private var isFetchInProgress = false

    init {
        observeWatchLaterBookmarks()
        observeSubscriptions()
        loadFeed(forceRefresh = false)
    }

    private fun observeWatchLaterBookmarks() {
        viewModelScope.launch {
            watchLaterRepository.getWatchLaterFlow().collect { bookmarks ->
                val idSet = bookmarks.map { it.videoId }.toSet()
                _uiState.update { it.copy(bookmarkedVideoIds = idSet) }
            }
        }
    }

    private fun observeSubscriptions() {
        viewModelScope.launch {
            subscriptionRepository.getSubscriptionsFlow().collect { subscriptions ->
                val isEmpty = subscriptions.isEmpty()
                val wasEmpty = _uiState.value.isEmptySubscriptions
                _uiState.update { it.copy(isEmptySubscriptions = isEmpty) }

                if (isEmpty) {
                    _uiState.update { it.copy(videos = emptyList(), isLoading = false, isRefreshing = false) }
                } else if (wasEmpty) {
                    // When subscriptions transition from empty to non-empty, auto-refresh feed
                    loadFeed(forceRefresh = true)
                }
            }
        }
    }

    /**
     * Loads the feed. Reuses 15-minute cache unless forceRefresh is true.
     */
    fun refresh(force: Boolean = true) {
        loadFeed(forceRefresh = force)
    }

    private fun loadFeed(forceRefresh: Boolean) {
        if (isFetchInProgress) return
        isFetchInProgress = true

        val isInitial = _uiState.value.videos.isEmpty()
        _uiState.update {
            it.copy(
                isLoading = isInitial,
                isRefreshing = !isInitial && forceRefresh,
                errorMessage = null
            )
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                val result = feedRepository.getFeed(forceRefresh = forceRefresh)

                result.fold(
                    onSuccess = { videoList ->
                        _uiState.update {
                            it.copy(
                                videos = videoList,
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = null
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                errorMessage = error.localizedMessage ?: "تعذر تحميل خلاصات القنوات. يرجى التحقق من الاتصال."
                            )
                        }
                    }
                )
            } finally {
                isFetchInProgress = false
            }
        }
    }

    /**
     * Toggles bookmark status for a video in Watch Later.
     * Applies optimistic state update to prevent rapid-tap race conditions (Subagent B Red-Teaming).
     */
    fun toggleBookmark(video: FeedVideoItem) {
        viewModelScope.launch {
            val currentlyBookmarked = _uiState.value.bookmarkedVideoIds.contains(video.videoId)
            val updatedBookmarks = if (currentlyBookmarked) {
                _uiState.value.bookmarkedVideoIds - video.videoId
            } else {
                _uiState.value.bookmarkedVideoIds + video.videoId
            }
            _uiState.update { it.copy(bookmarkedVideoIds = updatedBookmarks) }

            if (currentlyBookmarked) {
                watchLaterRepository.removeFromWatchLater(video.videoId)
            } else {
                watchLaterRepository.addToWatchLater(
                    WatchLaterEntity(
                        videoId = video.videoId,
                        title = video.title,
                        channelId = video.channelId,
                        channelName = video.channelName,
                        thumbnailUrl = video.thumbnailUrl,
                        durationMs = video.durationMs,
                        addedAtMs = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Activates the Curated Starter Pack when subscriptions are empty.
     */
    fun activateStarterPack() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            subscriptionRepository.reactivateStarterPack()
            loadFeed(forceRefresh = true)
        }
    }
}
