package com.dr.tech.puretube.features.channel.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.components.PureErrorState
import com.dr.tech.puretube.core.components.PureVideoCard
import com.dr.tech.puretube.core.components.PureVideoCardShimmer
import com.dr.tech.puretube.core.data.model.ChannelSortOrder
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.designsystem.theme.PureTheme

/**
 * Videos tab content in Channel screen with sorting chips and video list.
 * Conforms to Constitution Principles I, V, VI (< 300 LOC), VII.
 */
@Composable
fun ChannelVideosTab(
    videos: List<FeedVideoItem>,
    sortOrder: ChannelSortOrder,
    isLoading: Boolean,
    bookmarkedVideoIds: Set<String>,
    onSortOrderChanged: (ChannelSortOrder) -> Unit,
    onVideoClick: (String) -> Unit,
    onBookmarkClick: (FeedVideoItem) -> Unit,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        // Sorting Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                ChannelSortOrder.LATEST to "الأحدث",
                ChannelSortOrder.POPULAR to "الأكثر مشاهدة",
                ChannelSortOrder.OLDEST to "الأقدم"
            )

            chips.forEach { (order, label) ->
                val isSelected = sortOrder == order
                FilterChip(
                    selected = isSelected,
                    onClick = { onSortOrderChanged(order) },
                    label = {
                        Text(
                            text = label,
                            style = PureTheme.typography.labelSmall
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = PureTheme.colors.surface,
                        labelColor = PureTheme.colors.textSecondary,
                        selectedContainerColor = PureTheme.colors.primary,
                        selectedLabelColor = PureTheme.colors.background
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isSelected) PureTheme.colors.primary else PureTheme.colors.borderOutline
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Videos Content
        when {
            isLoading && videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(3) {
                        PureVideoCardShimmer()
                    }
                }
            }

            errorMessage != null && videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    PureErrorState(
                        title = "تعذر تحميل مقاطع الفيديو",
                        message = errorMessage,
                        onRetry = onRetry ?: {}
                    )
                }
            }

            videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    PureEmptyState(
                        icon = Icons.Outlined.VideoLibrary,
                        title = "لا توجد مقاطع فيديو عامة",
                        description = "لم يقم هذا الناشر بنشر أي مقاطع مرئية عامة في هذا القسم بعد."
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = videos,
                        key = { _, video -> video.videoId }
                    ) { _, video ->
                        PureVideoCard(
                            video = video,
                            isBookmarked = video.videoId in bookmarkedVideoIds,
                            onClick = { onVideoClick(video.videoId) },
                            onBookmarkClick = { onBookmarkClick(video) }
                        )
                    }
                }
            }
        }
    }
}
