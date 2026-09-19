package com.dr.tech.puretube.features.channel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.data.model.ChannelSortOrder
import com.dr.tech.puretube.core.data.model.ChannelTabType
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.channel.components.ChannelAboutTab
import com.dr.tech.puretube.features.channel.components.ChannelHeader
import com.dr.tech.puretube.features.channel.components.ChannelPlaylistsTab
import com.dr.tech.puretube.features.channel.components.ChannelTabsRow
import com.dr.tech.puretube.features.channel.components.ChannelVideosTab
import org.koin.androidx.compose.koinViewModel

/**
 * Root Channel Profile Screen matching official YouTube layout.
 * Conforms to Constitution Principles I, V, VI (< 300 LOC), VII, IX.
 */
@Composable
fun ChannelScreen(
    channelId: String,
    onBackClick: () -> Unit,
    onVideoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    miniPlayerContent: @Composable () -> Unit = {},
    viewModel: ChannelViewModel = koinViewModel()
) {
    BackHandler(onBack = onBackClick)

    LaunchedEffect(channelId) {
        viewModel.initChannel(channelId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            if (uiState.videos.isNotEmpty() || uiState.playlists.isNotEmpty() || uiState.liveStreams.isNotEmpty()) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background),
        bottomBar = {
            Box(modifier = Modifier.navigationBarsPadding()) {
                miniPlayerContent()
            }
        },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureTheme.colors.background)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع",
                        tint = PureTheme.colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = uiState.profile?.channelName ?: "القناة",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "تحديث",
                        tint = PureTheme.colors.textPrimary
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureTheme.colors.background)
        ) {
            when {
                uiState.isLoadingProfile && uiState.profile == null -> {
                    CircularProgressIndicator(
                        color = PureTheme.colors.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }

                uiState.profile == null && uiState.errorMessage != null -> {
                    PureErrorState(
                        title = "خطأ في التحميل",
                        message = uiState.errorMessage ?: "تعذر تحميل بيانات القناة",
                        onRetry = { viewModel.refresh() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    uiState.profile?.let { profile ->
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Channel Header (Banner + Identity + Actions)
                            ChannelHeader(
                                profile = profile,
                                isSubscribed = uiState.isSubscribed,
                                isActionInProgress = uiState.isActionInProgress,
                                onSubscribeToggle = { viewModel.toggleSubscribe() },
                                onDescriptionClick = { viewModel.selectTab(ChannelTabType.ABOUT) }
                            )

                            // Tabs Row (Videos | Playlists | Live | About)
                            ChannelTabsRow(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )

                            // Active Tab Content
                            Box(modifier = Modifier.weight(1f)) {
                                when (uiState.selectedTab) {
                                    ChannelTabType.VIDEOS -> {
                                        ChannelVideosTab(
                                            videos = uiState.videos,
                                            sortOrder = uiState.sortOrder,
                                            isLoading = uiState.isLoadingContent,
                                            bookmarkedVideoIds = uiState.bookmarkedVideoIds,
                                            onSortOrderChanged = { viewModel.changeSortOrder(it) },
                                            onVideoClick = onVideoClick,
                                            onBookmarkClick = { viewModel.toggleBookmark(it) },
                                            errorMessage = uiState.errorMessage,
                                            onRetry = { viewModel.refresh() }
                                        )
                                    }
                                    ChannelTabType.PLAYLISTS -> {
                                        ChannelPlaylistsTab(
                                            playlists = uiState.playlists,
                                            isLoading = uiState.isLoadingContent,
                                            onPlaylistClick = { /* Can open playlist in future */ }
                                        )
                                    }
                                    ChannelTabType.LIVE -> {
                                        ChannelVideosTab(
                                            videos = uiState.liveStreams,
                                            sortOrder = ChannelSortOrder.LATEST,
                                            isLoading = uiState.isLoadingContent,
                                            bookmarkedVideoIds = uiState.bookmarkedVideoIds,
                                            onSortOrderChanged = {},
                                            onVideoClick = onVideoClick,
                                            onBookmarkClick = { viewModel.toggleBookmark(it) },
                                            errorMessage = uiState.errorMessage,
                                            onRetry = { viewModel.refresh() }
                                        )
                                    }
                                    ChannelTabType.ABOUT -> {
                                        ChannelAboutTab(profile = profile)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
