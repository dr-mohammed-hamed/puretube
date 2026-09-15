package com.dr.tech.puretube.features.watchlater

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dr.tech.puretube.core.components.PureEmptyState
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.watchlater.components.WatchLaterItemCard
import org.koin.androidx.compose.koinViewModel

/**
 * Watch Later Screen for intentional postponed viewing (Constitution Principle I & IV).
 */
@Composable
fun WatchLaterScreen(
    onVideoClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WatchLaterViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearAllDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        // Top Header
        WatchLaterHeader(
            hasItems = state.items.isNotEmpty(),
            onClearAllClick = { showClearAllDialog = true }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    CircularProgressIndicator(
                        color = PureTheme.colors.primary,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }

                state.items.isEmpty() -> {
                    PureEmptyState(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = "لا توجد مقاطع محفوظة",
                        description = "احفظ المقاطع التي ترغب في مشاهدتها بوعي في وقت لاحق من خلال النقر على أيقونة الحفظ.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.items,
                            key = { it.videoId }
                        ) { item ->
                            WatchLaterItemCard(
                                item = item,
                                onClick = { onVideoClick(item.videoId) },
                                onDeleteClick = { viewModel.removeItem(item.videoId) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearAllDialog) {
        ClearAllConfirmationDialog(
            onConfirm = {
                viewModel.clearAll()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

@Composable
private fun WatchLaterHeader(
    hasItems: Boolean,
    onClearAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "المشاهدة لاحقاً",
            style = PureTheme.typography.titleMedium,
            color = PureTheme.colors.textPrimary
        )

        if (hasItems) {
            IconButton(onClick = onClearAllClick) {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = "مسح كافة المحفوظات",
                    tint = PureTheme.colors.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClearAllConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureTheme.colors.surface,
            border = BorderStroke(1.dp, PureTheme.colors.borderOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "مسح قائمة المشاهدة لاحقاً",
                    style = PureTheme.typography.titleMedium,
                    color = PureTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هل أنت متأكد من رغبتك في حذف جميع المقاطع المحفوظة من قائمة المشاهدة لاحقاً؟",
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, PureTheme.colors.borderOutline)
                    ) {
                        Text(
                            text = "تراجع",
                            style = PureTheme.typography.labelMedium,
                            color = PureTheme.colors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PureTheme.colors.error,
                            contentColor = PureTheme.colors.background
                        )
                    ) {
                        Text(
                            text = "مسح الكل",
                            style = PureTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
