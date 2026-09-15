package com.dr.tech.puretube.features.subscriptions

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.dr.tech.puretube.features.subscriptions.components.ChannelListItem
import com.dr.tech.puretube.features.subscriptions.components.ChannelSearchBar
import com.dr.tech.puretube.features.subscriptions.sheets.ImportExportSheet
import com.dr.tech.puretube.features.subscriptions.sheets.UnsubscribeConfirmDialog
import org.koin.androidx.compose.koinViewModel

/**
 * Subscriptions Management and Portability Hub Screen.
 * Conforms to Constitution Principles I, III, and IV.
 */
@Composable
fun SubscriptionsScreen(
    onChannelClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SubscriptionsViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showImportExportSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background)
    ) {
        // Top Header
        SubscriptionsHeader(
            onOpenImportExport = { showImportExportSheet = true }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search / Add Bar
            ChannelSearchBar(
                onAddChannel = { viewModel.addChannel(it) },
                isActionInProgress = state.isActionInProgress
            )

            Spacer(modifier = Modifier.height(10.dp))

            // User Feedback Banner (Success or Error)
            FeedbackBanner(
                errorMessage = state.errorMessage,
                userNoticeMessage = state.userNoticeMessage,
                onDismiss = { viewModel.clearMessages() }
            )

            // Starter Pack Quick Row
            StarterPackQuickAction(
                isActivating = state.isActivatingPack,
                onActivate = { viewModel.activateStarterPack() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Subscriptions List or Empty State
            when {
                state.isLoading && state.subscriptions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = PureTheme.colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                state.subscriptions.isEmpty() -> {
                    PureEmptyState(
                        icon = Icons.Outlined.Subscriptions,
                        title = "لا توجد اشتراكات مسجلة",
                        description = "أضف قنواتك المفضلة يدويًا أعلاه أو قم بتفعيل حزمة البداية النقية.",
                        actionLabel = "تفعيل حزمة البداية",
                        onActionClick = { viewModel.activateStarterPack() },
                        modifier = Modifier.padding(top = 20.dp)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = state.subscriptions,
                            key = { it.channelId }
                        ) { channel ->
                            ChannelListItem(
                                channel = channel,
                                onUnsubscribeClick = { viewModel.requestUnsubscribe(channel) },
                                onClick = { onChannelClick(channel.channelId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Unsubscribe Confirmation Dialog
    state.channelPendingUnsubscribe?.let { channel ->
        UnsubscribeConfirmDialog(
            channel = channel,
            onConfirm = { viewModel.confirmUnsubscribe() },
            onDismiss = { viewModel.dismissUnsubscribeDialog() }
        )
    }

    // Import / Export Bottom Sheet
    if (showImportExportSheet) {
        ImportExportSheet(
            onDismiss = { showImportExportSheet = false },
            onImportNewPipeJson = { viewModel.importNewPipeJson(it) },
            onImportTakeoutCsv = { viewModel.importTakeoutCsv(it) },
            onExportNewPipeJson = { viewModel.exportNewPipeJson() },
            isImporting = state.isImporting,
            onError = { viewModel.showErrorMessage(it) }
        )
    }
}

@Composable
private fun SubscriptionsHeader(
    onOpenImportExport: () -> Unit
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
            text = "الاشتراكات",
            style = PureTheme.typography.titleMedium,
            color = PureTheme.colors.textPrimary
        )

        IconButton(onClick = onOpenImportExport) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "استيراد وتصدير",
                tint = PureTheme.colors.primary
            )
        }
    }
}

@Composable
private fun FeedbackBanner(
    errorMessage: String?,
    userNoticeMessage: String?,
    onDismiss: () -> Unit
) {
    val message = errorMessage ?: userNoticeMessage ?: return
    val isError = errorMessage != null

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) PureTheme.colors.error.copy(alpha = 0.15f) else PureTheme.colors.primary.copy(alpha = 0.15f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = message,
                style = PureTheme.typography.bodyMedium,
                color = if (isError) PureTheme.colors.error else PureTheme.colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text(
                    text = "إغلاق",
                    style = PureTheme.typography.labelSmall,
                    color = PureTheme.colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun StarterPackQuickAction(
    isActivating: Boolean,
    onActivate: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = PureTheme.colors.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PureTheme.colors.secondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "حزمة القنوات المختارة (10 قنوات)",
                    style = PureTheme.typography.bodyMedium,
                    color = PureTheme.colors.textPrimary
                )
            }

            TextButton(
                onClick = onActivate,
                enabled = !isActivating
            ) {
                if (isActivating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PureTheme.colors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "تفعيل / استعادة",
                        style = PureTheme.typography.labelMedium,
                        color = PureTheme.colors.primary
                    )
                }
            }
        }
    }
}
