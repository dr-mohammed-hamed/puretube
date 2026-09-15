package com.dr.tech.puretube.player.di

import com.dr.tech.puretube.player.PureMediaSourceFactory
import com.dr.tech.puretube.player.PurePlayerManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin module declaring Media3 ExoPlayer and playback dependencies.
 */
val playerModule = module {
    single { PureMediaSourceFactory(context = androidContext()) }

    single {
        PurePlayerManager(
            context = androidContext(),
            mediaSourceFactory = get(),
            videoDetailsRepository = get(),
            videoFrameHook = get(),
            audioFilterHook = get()
        )
    }
}
