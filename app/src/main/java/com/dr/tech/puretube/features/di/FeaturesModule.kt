package com.dr.tech.puretube.features.di

import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.features.feed.FeedViewModel
import com.dr.tech.puretube.features.settings.SettingsViewModel
import com.dr.tech.puretube.features.subscriptions.SubscriptionsViewModel
import com.dr.tech.puretube.features.watchlater.WatchLaterViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module providing all feature ViewModels and ThemePreferences.
 */
val featuresModule = module {
    single { ThemePreferences(androidContext()) }

    viewModel {
        FeedViewModel(
            feedRepository = get(),
            subscriptionRepository = get(),
            watchLaterRepository = get()
        )
    }

    viewModel {
        SubscriptionsViewModel(
            subscriptionRepository = get()
        )
    }

    viewModel {
        WatchLaterViewModel(
            watchLaterRepository = get()
        )
    }

    viewModel {
        SettingsViewModel(
            themePreferences = get(),
            subscriptionRepository = get(),
            watchLaterRepository = get(),
            historyRepository = get()
        )
    }

    viewModel {
        com.dr.tech.puretube.features.player.PlayerViewModel(
            playerManager = get(),
            videoDetailsRepository = get(),
            historyRepository = get(),
            subscriptionRepository = get(),
            watchLaterRepository = get()
        )
    }
}
