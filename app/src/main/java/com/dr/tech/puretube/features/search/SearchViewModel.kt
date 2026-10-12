package com.dr.tech.puretube.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dr.tech.puretube.core.data.model.SearchFilter
import com.dr.tech.puretube.core.data.model.SearchResultItem
import com.dr.tech.puretube.core.data.repository.SearchRepository
import com.dr.tech.puretube.core.database.dao.SearchHistoryDao
import com.dr.tech.puretube.core.database.entity.SearchHistoryEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

data class SearchUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val results: List<SearchResultItem> = emptyList(),
    val hasMore: Boolean = false,
    val hasSearched: Boolean = false
)

/**
 * Search ViewModel with F1 red-team guards: 500ms debounce, in-flight
 * job cancellation, and a monotonic requestId so stale responses never
 * overwrite newer ones. History persisted locally (cap 30 on read).
 */
class SearchViewModel(
    private val searchRepository: SearchRepository,
    private val searchHistoryDao: SearchHistoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val debounceMs: Long = 500L
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val history: StateFlow<List<String>> = searchHistoryDao.getRecentFlow(30)
        .map { list -> list.map { it.query } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var debounceJob: Job? = null
    private var nextPageToken: String? = null
    private val requestIdCounter = AtomicInteger(0)

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        android.util.Log.e("SearchViewModel", "Uncaught search coroutine exception", throwable)
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                errorMessage = "حدث خطأ غير متوقع أثناء البحث"
            )
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
        debounceJob?.cancel()
        if (value.isBlank()) {
            nextPageToken = null
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    results = emptyList(),
                    hasMore = false,
                    hasSearched = false
                )
            }
            return
        }
        debounceJob = viewModelScope.launch(coroutineExceptionHandler) {
            delay(debounceMs)
            executeSearch(requestQuery = value.trim(), requestFilter = _filter.value, isNewSearch = true)
        }
    }

    fun onSearchSubmit(rawQuery: String = _query.value) {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank()) return
        _query.value = trimmed
        debounceJob?.cancel()
        viewModelScope.launch(coroutineExceptionHandler) {
            saveHistory(trimmed)
        }
        viewModelScope.launch(coroutineExceptionHandler) {
            executeSearch(requestQuery = trimmed, requestFilter = _filter.value, isNewSearch = true)
        }
    }

    fun onFilterChange(filter: SearchFilter) {
        if (_filter.value == filter) return
        _filter.value = filter
        nextPageToken = null
        debounceJob?.cancel()
        val current = _query.value.trim()
        if (current.isBlank()) return
        viewModelScope.launch(coroutineExceptionHandler) {
            executeSearch(requestQuery = current, requestFilter = filter, isNewSearch = true)
        }
    }

    fun loadMore() {
        val token = nextPageToken ?: return
        if (_uiState.value.isLoading || _uiState.value.isLoadingMore) return
        val requestId = requestIdCounter.incrementAndGet()
        _uiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        viewModelScope.launch(coroutineExceptionHandler) {
            searchRepository.getNextPage(token).onSuccess { (items, next) ->
                if (requestId != requestIdCounter.get()) return@onSuccess
                nextPageToken = next
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        results = (it.results + items).distinctBy { item ->
                            when (item) {
                                is SearchResultItem.VideoItem -> "v_${item.videoId}"
                                is SearchResultItem.ChannelItem -> "c_${item.channelId}"
                            }
                        },
                        hasMore = next != null
                    )
                }
            }.onFailure {
                if (requestId != requestIdCounter.get()) return@onFailure
                nextPageToken = token
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        errorMessage = "تعذر تحميل المزيد من النتائج، تحقق من الاتصال"
                    )
                }
            }
        }
    }

    fun retry() {
        val current = _query.value.trim()
        if (current.isBlank()) return
        viewModelScope.launch(coroutineExceptionHandler) {
            executeSearch(requestQuery = current, requestFilter = _filter.value, isNewSearch = true)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun removeHistory(query: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                searchHistoryDao.delete(query)
            } catch (_: Exception) {
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            try {
                searchHistoryDao.clearAll()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun executeSearch(
        requestQuery: String,
        requestFilter: SearchFilter,
        isNewSearch: Boolean
    ) {
        if (requestQuery.isBlank()) return
        val requestId = requestIdCounter.incrementAndGet()
        if (isNewSearch) {
            nextPageToken = null
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    errorMessage = null,
                    results = emptyList(),
                    hasMore = false,
                    hasSearched = true
                )
            }
        }
        val result = kotlinx.coroutines.withContext(ioDispatcher) {
            searchRepository.search(requestQuery, requestFilter)
        }
        if (requestId != requestIdCounter.get()) return
        result.onSuccess { (items, next) ->
            nextPageToken = next
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    results = if (isNewSearch) items else (it.results + items).distinctBy { item ->
                        when (item) {
                            is SearchResultItem.VideoItem -> "v_${item.videoId}"
                            is SearchResultItem.ChannelItem -> "c_${item.channelId}"
                        }
                    },
                    hasMore = next != null,
                    errorMessage = null
                )
            }
            if (items.isNotEmpty()) {
                viewModelScope.launch(coroutineExceptionHandler) { saveHistory(requestQuery) }
            }
        }.onFailure {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = "تعذر إتمام البحث، تحقق من الاتصال وحاول مجدداً"
                )
            }
        }
    }

    private suspend fun saveHistory(trimmedQuery: String) {
        try {
            searchHistoryDao.upsert(
                SearchHistoryEntity(
                    query = trimmedQuery,
                    searchedAtMs = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
        }
    }
}
