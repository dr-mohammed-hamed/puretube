package com.dr.tech.puretube.features.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.components.PureVideoCard
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.model.SearchResultItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.playlists.PlaylistViewModel
import com.dr.tech.puretube.features.playlists.sheets.AddToPlaylistSheet
import com.dr.tech.puretube.features.search.components.ChannelSearchCard
import com.dr.tech.puretube.features.search.components.SearchBarField
import com.dr.tech.puretube.features.search.components.SearchFilterRow
import com.dr.tech.puretube.features.search.components.SearchHistoryList
import org.koin.androidx.compose.koinViewModel

/**
 * Global search overlay: debounced query, ALL/VIDEOS/CHANNELS filter,
 * local history, paginated results and add-to-playlist actions.
 * PureTheme tokens only, RTL by default.
 */
@Composable
fun SearchScreen(
    initialQuery: String? = null,
    onVideoClick: (String) -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onBackClick: () -> Unit = {},
    miniPlayerContent: @Composable () -> Unit = {},
    viewModel: SearchViewModel = koinViewModel(),
    playlistViewModel: PlaylistViewModel = koinViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val playlists by playlistViewModel.playlists.collectAsStateWithLifecycle()
    val importState by playlistViewModel.importState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var sheetVideo by remember { mutableStateOf<FeedVideoItem?>(null) }
    var sheetCheckedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val listState = rememberLazyListState()

    LaunchedEffect(listState.canScrollForward, state.hasMore, state.isLoadingMore) {
        if (!listState.canScrollForward && state.hasMore && !state.isLoadingMore) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && query.isBlank()) {
            viewModel.onQueryChange(initialQuery)
        }
    }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage
        if (msg != null) {
            try {
                snackbarHostState.showSnackbar(msg)
            } finally {
                viewModel.clearError()
            }
        }
    }

    BackHandler(onBack = onBackClick)

    fun openSheetFor(video: FeedVideoItem) {
        sheetVideo = video
        sheetCheckedIds = emptySet()
    }

    LaunchedEffect(sheetVideo?.videoId) {
        val videoId = sheetVideo?.videoId
        if (!videoId.isNullOrBlank()) {
            sheetCheckedIds = playlistViewModel.membershipFor(videoId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureTheme.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = PureTheme.colors.textPrimary
                    )
                }
                SearchBarField(
                    query = query,
                    onQueryChange = viewModel::onQueryChange,
                    onSearchSubmit = { viewModel.onSearchSubmit() },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            SearchFilterRow(
                selected = filter,
                onFilterChange = viewModel::onFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    state.isLoading && state.results.isEmpty() -> {
                        PureFeedShimmerList(
                            count = 3,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    state.errorMessage != null && state.results.isEmpty() -> {
                        PureErrorState(
                            title = "تعذر إتمام البحث",
                            message = state.errorMessage ?: "حدث خطأ غير متوقع",
                            onRetry = { viewModel.retry() },
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    query.isBlank() && history.isNotEmpty() -> {
                        SearchHistoryList(
                            history = history,
                            onHistoryClick = { viewModel.onSearchSubmit(it) },
                            onHistoryDelete = { viewModel.removeHistory(it) },
                            onClearAll = { viewModel.clearHistory() }
                        )
                    }
                    state.results.isEmpty() && state.hasSearched && !state.isLoading -> {
                        PureEmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "لا توجد نتائج",
                            description = "جرّب كلمات مختلفة أو غيّر الفلتر بين الكل والفيديوهات والقنوات.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    query.isBlank() -> {
                        PureEmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "ابحث في يوتيوب",
                            description = "اكتب كلمة للبحث عن الفيديوهات والقنوات.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(
                                state.results,
                                key = { index, item ->
                                    "$index:" + when (item) {
                                        is SearchResultItem.VideoItem -> "v_${item.videoId}"
                                        is SearchResultItem.ChannelItem -> "c_${item.channelId}"
                                    }
                                }
                            ) { _, item ->
                                when (item) {
                                    is SearchResultItem.VideoItem -> {
                                        val feedItem = FeedVideoItem(
                                            videoId = item.videoId,
                                            title = item.title,
                                            channelId = item.channelId,
                                            channelName = item.channelName,
                                            thumbnailUrl = item.thumbnailUrl,
                                            durationMs = item.durationMs,
                                            viewCount = item.viewCount,
                                            uploadDateText = item.uploadDateText
                                        )
                                        PureVideoCard(
                                            video = feedItem,
                                            isBookmarked = false,
                                            onClick = { onVideoClick(item.videoId) },
                                            onBookmarkClick = { openSheetFor(feedItem) },
                                            onChannelClick = { onChannelClick(item.channelId) }
                                        )
                                    }
                                    is SearchResultItem.ChannelItem -> {
                                        ChannelSearchCard(
                                            item = item,
                                            onClick = { onChannelClick(item.channelId) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            miniPlayerContent()
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    val activeVideo = sheetVideo
    if (activeVideo != null) {
        AddToPlaylistSheet(
            videoTitle = activeVideo.title,
            playlists = playlists,
            checkedIds = sheetCheckedIds,
            importState = importState,
            onToggle = { playlistId, checked ->
                playlistViewModel.toggleInPlaylist(
                    playlistId = playlistId,
                    videoId = activeVideo.videoId,
                    title = activeVideo.title,
                    channelId = activeVideo.channelId,
                    channelName = activeVideo.channelName,
                    thumbnailUrl = activeVideo.thumbnailUrl,
                    durationMs = activeVideo.durationMs,
                    currentlyIn = !checked
                ) { nowIn ->
                    sheetCheckedIds = if (nowIn) {
                        sheetCheckedIds + playlistId
                    } else {
                        sheetCheckedIds - playlistId
                    }
                }
            },
            onCreatePlaylist = { name ->
                playlistViewModel.createPlaylist(name)
            },
            onImportUrl = { url ->
                playlistViewModel.importFromUrl(url)
            },
            onDismiss = { sheetVideo = null }
        )
    }
}
