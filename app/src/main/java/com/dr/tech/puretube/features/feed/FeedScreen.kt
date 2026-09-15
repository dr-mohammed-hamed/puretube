package com.dr.tech.puretube.features.feed

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.components.PureFeedShimmerList
import com.dr.tech.puretube.core.components.PureVideoCard
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.feed.components.FeedStarterBanner
import org.koin.androidx.compose.koinViewModel

/**
 * Feed Screen representing the Mindful Chronological Feed (Constitution Principle I & IV).
 * Zero recommendations, zero algorithmic feeds, strictly reverse-chronological from subscriptions.
 */
@Composable
fun FeedScreen(
    onVideoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        // Mindful Top App Header
        FeedHeader(
            isRefreshing = state.isRefreshing,
            onRefreshClick = { viewModel.refresh(force = true) }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when {
                state.isEmptySubscriptions -> {
                    FeedStarterBanner(
                        onActivateClick = { viewModel.activateStarterPack() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.isLoading && state.videos.isEmpty() -> {
                    PureFeedShimmerList(
                        count = 3,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                state.errorMessage != null && state.videos.isEmpty() -> {
                    PureErrorState(
                        title = "تعذر تحميل الخلاصات",
                        message = state.errorMessage ?: "حدث خطأ غير متوقع",
                        onRetry = { viewModel.refresh(force = true) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (state.isRefreshing) {
                            item(key = "refreshing_indicator") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PureTheme.colors.primary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        items(
                            items = state.videos,
                            key = { it.videoId }
                        ) { video ->
                            PureVideoCard(
                                video = video,
                                isBookmarked = video.videoId in state.bookmarkedVideoIds,
                                onClick = { onVideoClick(video.videoId) },
                                onBookmarkClick = { viewModel.toggleBookmark(video) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PureTheme.colors.primary,
                modifier = Modifier.size(26.dp)
            )
            Column {
                Text(
                    text = "PureTube | نَقِيّ",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
                Text(
                    text = "خلاصة واعية بدون خوارزميات",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.secondary
                )
            }
        }

        IconButton(
            onClick = onRefreshClick,
            enabled = !isRefreshing
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "تحديث الخلاصة",
                tint = PureTheme.colors.primary
            )
        }
    }
}
