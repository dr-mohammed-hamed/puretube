package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.pack.CuratedStarterPack
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

/**
 * Repository managing channel subscriptions, local Room persistence,
 * curated starter pack auto-seeding, and data portability.
 * Conforms to Constitution Principles I, III, IV, and VIII.
 */
class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao,
    private val extractorThrottler: ExtractorThrottler,
    private val importExportService: ImportExportService,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {
    private val toggleMutex = Mutex()
    private val _subscriptionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val subscriptionEvents: SharedFlow<Unit> = _subscriptionEvents.asSharedFlow()

    private suspend fun notifySubscriptionsChanged() {
        _subscriptionEvents.tryEmit(Unit)
    }

    /**
     * Emits the reactive list of user subscriptions, ordered by subscription date descending.
     */
    fun getSubscriptionsFlow(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllFlow()

    /**
     * Checks if a channel is subscribed reactively.
     * Sole reactive source of subscription truth (single Room EXISTS query, Constitution VIII).
     */
    fun isSubscribed(channelId: String): Flow<Boolean> = subscriptionDao.isSubscribed(channelId)

    /**
     * One-shot subscription check against the same single Room EXISTS query backing
     * [isSubscribed]. Authoritative decision source for toggles — callers MUST NOT
     * decide subscribe/unsubscribe from in-memory snapshots.
     */
    suspend fun isSubscribedOnce(channelId: String): Boolean = withContext(ioDispatcher) {
        subscriptionDao.isSubscribed(channelId).first()
    }

    /**
     * Atomically toggles subscription state from Room truth (no in-memory snapshot input).
     * Read + DAO mutation run atomically under [toggleMutex] on [ioDispatcher];
     * only [notifySubscriptionsChanged] (arbitrary [onSubscriptionsChanged] callback)
     * runs after lock release so the mutex is never held across it.
     * Cancellation is rethrown (never swallowed into Result).
     * Returns [Result] with the new subscribed state (true = now subscribed).
     */
    suspend fun toggleSubscription(entity: SubscriptionEntity): Result<Boolean> {
        val shouldSubscribe: Boolean = try {
            withContext(ioDispatcher) {
                toggleMutex.withLock {
                    val subscribed = subscriptionDao.isSubscribed(entity.channelId).first()
                    if (!subscribed) {
                        subscriptionDao.insert(entity)
                    } else {
                        subscriptionDao.deleteById(entity.channelId)
                    }
                    !subscribed
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }
        return try {
            notifySubscriptionsChanged()
            Result.success(shouldSubscribe)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Notify-only failure: DB already committed, don't report as toggle failure.
            android.util.Log.w("SubscriptionRepository", "notify failed after toggle", e)
            Result.success(shouldSubscribe)
        }
    }

    /**
     * Returns the one-shot list of all subscriptions on Dispatchers.IO.
     * Bulk-read only (export/seed flows) — NEVER scan this list to derive a single
     * channel's subscription status; use [isSubscribed]/[isSubscribedOnce] instead.
     */
    suspend fun getAllSubscriptions(): List<SubscriptionEntity> = withContext(ioDispatcher) {
        subscriptionDao.getAll()
    }

    /**
     * Checks if the subscription database is empty on app startup.
     * If empty, seeds the CuratedStarterPack without overriding existing preferences later.
     */
    suspend fun checkAndSeedStarterPackIfEmpty(): Boolean = withContext(ioDispatcher) {
        if (subscriptionDao.getCount() == 0) {
            subscriptionDao.insertAll(CuratedStarterPack.channels)
            notifySubscriptionsChanged()
            true
        } else {
            false
        }
    }

    /**
     * Re-activates / re-seeds the Curated Starter Pack on demand.
     */
    suspend fun reactivateStarterPack() = withContext(ioDispatcher) {
        subscriptionDao.insertAll(CuratedStarterPack.channels)
        notifySubscriptionsChanged()
    }

    /**
     * Subscribes to a channel.
     */
    suspend fun subscribe(entity: SubscriptionEntity) = withContext(ioDispatcher) {
        subscriptionDao.insert(entity)
        notifySubscriptionsChanged()
    }

    /**
     * Unsubscribes from a channel by ID.
     */
    suspend fun unsubscribe(channelId: String) = withContext(ioDispatcher) {
        subscriptionDao.deleteById(channelId)
        notifySubscriptionsChanged()
    }

    /**
     * Resolves channel metadata from YouTube using NewPipeExtractor under ExtractorThrottler.
     * Supports channel URLs, channel IDs, @handles, and direct video URLs (SPECIFICATION.md Section 4.A.2).
     * Confined to [ioDispatcher]: extractor fetchPage does network + parsing, never Main.
     */
    suspend fun fetchChannelDetails(channelUrlOrId: String): Result<SubscriptionEntity> =
        withContext(ioDispatcher) {
            val targetUrl = when {
                channelUrlOrId.startsWith("http://") || channelUrlOrId.startsWith("https://") -> channelUrlOrId
                channelUrlOrId.startsWith("@") -> "https://www.youtube.com/$channelUrlOrId"
                else -> "https://www.youtube.com/channel/$channelUrlOrId"
            }

            extractorThrottler.safeExtract {
                val service = ServiceList.YouTube
                val isVideoUrl = targetUrl.contains("watch?v=") || targetUrl.contains("youtu.be/") || targetUrl.contains("/shorts/")

                val resolvedChannelUrl: String = if (isVideoUrl) {
                    val streamExtractor = service.getStreamExtractor(targetUrl)
                    streamExtractor.fetchPage()
                    streamExtractor.uploaderUrl?.takeIf { it.isNotBlank() } ?: targetUrl
                } else {
                    targetUrl
                }

                val extractor = service.getChannelExtractor(resolvedChannelUrl)
                extractor.fetchPage()

                val rawId = extractor.id?.ifBlank { null } ?: importExportService.extractChannelIdFromUrl(resolvedChannelUrl)
                val channelId = rawId.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Failed to extract valid channel ID from: $channelUrlOrId")
                val channelName = extractor.name?.ifBlank { null } ?: channelId

                SubscriptionEntity(
                    channelId = channelId,
                    channelName = channelName,
                    channelHandle = if (resolvedChannelUrl.contains("/@")) "@" + resolvedChannelUrl.substringAfter("/@").substringBefore("/").substringBefore("?").substringBefore("#") else null,
                    avatarUrl = extractor.avatars?.firstOrNull()?.url,
                    subscriberCountText = extractor.subscriberCount?.takeIf { it > 0 }?.let { "$it" },
                    isNotificationsEnabled = true,
                    subscribedAtMs = System.currentTimeMillis()
                )
            }
        }

    /**
     * Imports subscriptions from NewPipe JSON format.
     * Returns count of imported items.
     */
    suspend fun importFromNewPipeJson(jsonContent: String): Result<Int> = withContext(ioDispatcher) {
        importExportService.parseNewPipeJson(jsonContent).mapCatching { entities ->
            if (entities.isNotEmpty()) {
                subscriptionDao.insertAll(entities)
                notifySubscriptionsChanged()
            }
            entities.size
        }
    }

    /**
     * Imports subscriptions from Google Takeout CSV format.
     * Returns count of imported items.
     */
    suspend fun importFromGoogleTakeoutCsv(csvContent: String): Result<Int> = withContext(ioDispatcher) {
        importExportService.parseGoogleTakeoutCsv(csvContent).mapCatching { entities ->
            if (entities.isNotEmpty()) {
                subscriptionDao.insertAll(entities)
                notifySubscriptionsChanged()
            }
            entities.size
        }
    }

    /**
     * Exports current subscriptions into NewPipe-compatible JSON string.
     */
    suspend fun exportToNewPipeJson(): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val all = subscriptionDao.getAll()
            importExportService.exportToNewPipeJson(all)
        }
    }

    /**
     * Returns total count of subscriptions.
     */
    suspend fun getSubscriptionCount(): Int = withContext(ioDispatcher) {
        subscriptionDao.getCount()
    }
}
