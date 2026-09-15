package com.dr.tech.puretube

import android.app.Application
import com.dr.tech.puretube.core.aibridge.AudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpAudioFilterHook
import com.dr.tech.puretube.core.aibridge.NoOpVideoFrameHook
import com.dr.tech.puretube.core.aibridge.VideoFrameHook
import com.dr.tech.puretube.core.data.di.repositoryModule
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.database.di.databaseModule
import com.dr.tech.puretube.features.di.featuresModule
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.PureDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
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
            modules(coreModule, databaseModule, repositoryModule, featuresModule)
        }

        // 3. Auto-seed Curated Starter Pack if subscriptions are empty (Constitution Principle I & PM Decision 1)
        val subscriptionRepository = get<SubscriptionRepository>()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                subscriptionRepository.checkAndSeedStarterPackIfEmpty()
            } catch (e: Exception) {
                // Defensive degradation: failure to seed must never crash application launch, but must be logged
                android.util.Log.e("PureTubeApp", "Defensive fallback: failed to auto-seed starter pack", e)
            }
        }
    }
}
