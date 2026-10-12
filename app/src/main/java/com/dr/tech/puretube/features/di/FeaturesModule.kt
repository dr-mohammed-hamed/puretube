package com.dr.tech.puretube.features.di

import com.dr.tech.puretube.core.designsystem.theme.ThemePreferences
import com.dr.tech.puretube.features.feed.FeedViewModel
import com.dr.tech.puretube.features.library.LibraryViewModel
import com.dr.tech.puretube.features.playlists.PlaylistViewModel
import com.dr.tech.puretube.features.search.SearchViewModel
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
            watchLaterRepository = get(),
            playbackPrefs = get(),
            playlistRepository = get()
        )
    }

    viewModel {
        com.dr.tech.puretube.features.channel.ChannelViewModel(
            channelRepository = get(),
            subscriptionRepository = get(),
            watchLaterRepository = get()
        )
    }

    viewModel {
        SearchViewModel(
            searchRepository = get(),
            searchHistoryDao = get()
        )
    }

    viewModel {
        LibraryViewModel(
            historyRepository = get(),
            watchLaterRepository = get(),
            playlistRepository = get()
        )
    }

    viewModel {
        PlaylistViewModel(
            playlistRepository = get()
        )
    }
}
