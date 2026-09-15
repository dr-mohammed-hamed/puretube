package com.dr.tech.puretube.core.data.di

import com.dr.tech.puretube.core.data.repository.FeedRepository
import com.dr.tech.puretube.core.data.repository.HistoryRepository
import com.dr.tech.puretube.core.data.repository.SubscriptionRepository
import com.dr.tech.puretube.core.data.repository.WatchLaterRepository
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.dsl.module

/**
 * Koin module providing Repositories and Portability services.
 */
val repositoryModule = module {
    single { ImportExportService() }

    single {
        SubscriptionRepository(
            subscriptionDao = get(),
            extractorThrottler = get(),
            importExportService = get()
        )
    }

    single(createdAtStart = true) {
        val subscriptionRepo = get<SubscriptionRepository>()
        FeedRepository(
            subscriptionDao = get(),
            extractorThrottler = get()
        ).also { feedRepo ->
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                subscriptionRepo.subscriptionEvents.collect {
                    feedRepo.clearCache()
                }
            }
        }
    }

    single {
        WatchLaterRepository(
            watchLaterDao = get()
        )
    }

    single {
        HistoryRepository(
            historyDao = get()
        )
    }
}
