package com.dr.tech.puretube

import android.app.Application
import com.dr.tech.puretube.core.aibridge.AudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpAudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpVideoFrameHook
import com.dr.tech.puretube.core.aibridge.VideoFrameHook
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.PureDownloader
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.schabi.newpipe.extractor.NewPipe

/**
 * Koin module providing core singleton dependencies.
 */
val coreModule = module {
    // Network throttler enforcing Semaphore(4) concurrency limit
    single { ExtractorThrottler(maxConcurrentCalls = 4) }

    // Modular AI hooks (NoOp implementations in V1)
    single<VideoFrameHook> { NoOpVideoFrameHook() }
    single<AudioFilterHook> { NoOpAudioFilterHook() }
}

/**
 * PureTube Application entry point.
 */
class PureTubeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize NewPipeExtractor with custom OkHttp-backed Downloader and disk cache
        NewPipe.init(PureDownloader(cacheDir = cacheDir))

        // 2. Initialize Koin Dependency Injection
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PureTubeApp)
            modules(coreModule)
        }
    }
}
