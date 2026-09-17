package com.dr.tech.puretube.features.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.model.ChannelPlaylistItem
import com.dr.tech.puretube.core.data.model.ChannelProfileDetails
import com.dr.tech.puretube.core.data.model.ChannelSortOrder
import com.dr.tech.puretube.core.data.model.ChannelTabType
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.repository.ChannelRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.database.entity.WatchLaterEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Channel Profile Screen.
 */
data class ChannelUiState(
    val channelId: String = "",
    val profile: ChannelProfileDetails? = null,
    val isSubscribed: Boolean = false,
    val isNotificationsEnabled: Boolean = true,
    val selectedTab: ChannelTabType = ChannelTabType.VIDEOS,
    val sortOrder: ChannelSortOrder = ChannelSortOrder.LATEST,
    val videos: List<FeedVideoItem> = emptyList(),
    val playlists: List<ChannelPlaylistItem> = emptyList(),
    val liveStreams: List<FeedVideoItem> = emptyList(),
    val bookmarkedVideoIds: Set<String> = emptySet(),
    val isLoadingProfile: Boolean = false,
    val isLoadingContent: Boolean = false,
    val isActionInProgress: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel managing channel metadata, video sorting, tab transitions,
 * and subscription state persistence with Room DB.
 * Conforms to Constitution Principles I, V, VI (< 250 LOC), and IX.
 */
class ChannelViewModel(
    private val channelRepository: ChannelRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val watchLaterRepository: WatchLaterRepository
) : ViewModel() {

    private val bookmarkingIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private var currentChannelKey: String = ""
    private var subObserveJob: Job? = null
    private var watchLaterObserveJob: Job? = null
    private var contentJob: Job? = null
    private var profileJob: Job? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("ChannelViewModel", "Unhandled coroutine error", throwable)
        _uiState.update {
            it.copy(
                isLoadingProfile = false,
                isLoadingContent = false,
                errorMessage = sanitizeErrorMessage(throwable)
            )
        }
    }

    init {
        observeWatchLater()
    }

    fun initChannel(channelIdOrUrl: String) {
        if (channelIdOrUrl.isBlank() || channelIdOrUrl == currentChannelKey) return
        currentChannelKey = channelIdOrUrl
        _uiState.update { it.copy(channelId = channelIdOrUrl, isLoadingProfile = true, errorMessage = null) }

        loadProfile(channelIdOrUrl)
        loadTabContent(channelIdOrUrl, _uiState.value.selectedTab, _uiState.value.sortOrder)
        observeSubscription(channelIdOrUrl)
    }

    fun refresh() {
        val channel = currentChannelKey.ifBlank { _uiState.value.channelId }
        if (channel.isNotBlank()) {
            loadProfile(channel, forceRefresh = true)
            loadTabContent(channel, _uiState.value.selectedTab, _uiState.value.sortOrder, forceRefresh = true)
        }
    }

    fun selectTab(tab: ChannelTabType) {
        if (_uiState.value.selectedTab == tab) return
        _uiState.update { it.copy(selectedTab = tab) }
        loadTabContent(currentChannelKey, tab, _uiState.value.sortOrder)
    }

    fun changeSortOrder(sortOrder: ChannelSortOrder) {
        if (_uiState.value.sortOrder == sortOrder) return
        _uiState.update { it.copy(sortOrder = sortOrder) }
        loadTabContent(currentChannelKey, ChannelTabType.VIDEOS, sortOrder)
    }

    private fun loadProfile(channelKey: String, forceRefresh: Boolean = false) {
        profileJob?.cancel()
        profileJob = viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoadingProfile = true) }
            channelRepository.getChannelProfile(channelKey, forceRefresh)
                .onSuccess { profile ->
                    _uiState.update { it.copy(profile = profile, isLoadingProfile = false) }
                    // If channel ID resolved to actual UC ID, observe that as well
                    if (profile.channelId != channelKey) {
                        observeSubscription(profile.channelId)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoadingProfile = false, errorMessage = sanitizeErrorMessage(error)) }
                }
        }
    }

    private fun loadTabContent(
        channelKey: String,
        tab: ChannelTabType,
        sortOrder: ChannelSortOrder,
        forceRefresh: Boolean = false
    ) {
        contentJob?.cancel()
        contentJob = viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoadingContent = true) }
            when (tab) {
                ChannelTabType.VIDEOS -> {
                    channelRepository.getChannelVideos(channelKey, sortOrder, forceRefresh)
                        .onSuccess { videos -> _uiState.update { it.copy(videos = videos, isLoadingContent = false) } }
                        .onFailure { error -> _uiState.update { it.copy(isLoadingContent = false, errorMessage = sanitizeErrorMessage(error)) } }
                }
                ChannelTabType.PLAYLISTS -> {
                    channelRepository.getChannelPlaylists(channelKey, forceRefresh)
                        .onSuccess { playlists -> _uiState.update { it.copy(playlists = playlists, isLoadingContent = false) } }
                        .onFailure { error -> _uiState.update { it.copy(isLoadingContent = false, errorMessage = sanitizeErrorMessage(error)) } }
                }
                ChannelTabType.LIVE -> {
                    channelRepository.getChannelLiveStreams(channelKey, forceRefresh)
                        .onSuccess { live -> _uiState.update { it.copy(liveStreams = live, isLoadingContent = false) } }
                        .onFailure { error -> _uiState.update { it.copy(isLoadingContent = false, errorMessage = sanitizeErrorMessage(error)) } }
                }
                ChannelTabType.ABOUT -> {
                    _uiState.update { it.copy(isLoadingContent = false) }
                }
            }
        }
    }

    private fun observeSubscription(channelId: String) {
        subObserveJob?.cancel()
        subObserveJob = viewModelScope.launch(exceptionHandler) {
            subscriptionRepository.isSubscribed(channelId).collect { isSub ->
                _uiState.update { it.copy(isSubscribed = isSub) }
            }
        }
    }

    private fun observeWatchLater() {
        watchLaterObserveJob?.cancel()
        watchLaterObserveJob = viewModelScope.launch(exceptionHandler) {
            watchLaterRepository.getWatchLaterFlow().collect { items ->
                _uiState.update { it.copy(bookmarkedVideoIds = items.map { item -> item.videoId }.toSet()) }
            }
        }
    }

    fun toggleSubscribe() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isActionInProgress) return

        viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isActionInProgress = true) }
            try {
                if (_uiState.value.isSubscribed) {
                    subscriptionRepository.unsubscribe(profile.channelId)
                } else {
                    val entity = SubscriptionEntity(
                        channelId = profile.channelId,
                        channelName = profile.channelName,
                        channelHandle = profile.channelHandle,
                        avatarUrl = profile.avatarUrl,
                        subscriberCountText = profile.subscriberCountText,
                        isNotificationsEnabled = true,
                        subscribedAtMs = System.currentTimeMillis()
                    )
                    subscriptionRepository.subscribe(entity)
                }
            } finally {
                _uiState.update { it.copy(isActionInProgress = false) }
            }
        }
    }

    fun toggleBookmark(video: FeedVideoItem) {
        if (!bookmarkingIds.add(video.videoId)) return
        viewModelScope.launch(exceptionHandler) {
            try {
                val entity = WatchLaterEntity(
                    videoId = video.videoId,
                    title = video.title,
                    channelId = video.channelId,
                    channelName = video.channelName,
                    thumbnailUrl = video.thumbnailUrl,
                    durationMs = video.durationMs,
                    addedAtMs = System.currentTimeMillis()
                )
                watchLaterRepository.toggleWatchLater(entity)
            } finally {
                bookmarkingIds.remove(video.videoId)
            }
        }
    }

    /**
     * Sanitizes internal exceptions into dignified, user-friendly Arabic explanations
     * according to Constitution Principles IV and VII. Prevents raw JVM LinkageError /
     * NoSuchMethodError signatures from surfacing in the presentation layer.
     */
    private fun sanitizeErrorMessage(error: Throwable): String {
        android.util.Log.e("ChannelViewModel", "Channel operation failed", error)
        val raw = error.localizedMessage.orEmpty()
        return when {
            error is java.lang.LinkageError || raw.contains("NoSuchMethodError") || raw.contains("URLEncoder") ->
                "تعذر تحميل بيانات القناة بسبب عدم توافق في النظام، يرجى تحديث التطبيق أو إعادة المحاولة."
            error is java.io.IOException || raw.contains("timeout", ignoreCase = true) || raw.contains("network", ignoreCase = true) ->
                "تعذر الاتصال بالإنترنت لجلب بيانات القناة. يرجى التحقق من الاتصال والمحاولة مجدداً."
            raw.isNotBlank() && !raw.contains("java.", ignoreCase = true) && !raw.contains("class L", ignoreCase = true) ->
                raw
            else ->
                "تعذر تحميل بيانات القناة حالياً. يرجى إعادة المحاولة."
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
