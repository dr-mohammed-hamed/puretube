package com.dr.tech.puretube.features.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dr.tech.puretube.core.components.PureBottomNavBar
import com.dr.tech.puretube.core.designsystem.theme.PureTheme
import com.dr.tech.puretube.features.feed.FeedScreen
import com.dr.tech.puretube.features.settings.SettingsScreen
import com.dr.tech.puretube.features.subscriptions.SubscriptionsScreen
import com.dr.tech.puretube.features.watchlater.WatchLaterScreen

/**
 * Permanent navigation shell of PureTube (Constitution Principles V & VII).
 * Replaces the temporary test harness and manages stateful transitions between top-level tabs.
 */
@Composable
fun MainNavigationShell(
    modifier: Modifier = Modifier,
    onVideoClick: (String) -> Unit = {}
) {
    var currentTab by rememberSaveable { mutableStateOf(PureNavTab.FEED) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PureTheme.colors.background),
        bottomBar = {
            PureBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PureTheme.colors.background)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "PureTubeTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    PureNavTab.FEED -> FeedScreen(
                        onVideoClick = onVideoClick
                    )
                    PureNavTab.SUBSCRIPTIONS -> SubscriptionsScreen()
                    PureNavTab.WATCH_LATER -> WatchLaterScreen(
                        onVideoClick = onVideoClick
                    )
                    PureNavTab.SETTINGS -> SettingsScreen()
                }
            }
        }
    }
}
