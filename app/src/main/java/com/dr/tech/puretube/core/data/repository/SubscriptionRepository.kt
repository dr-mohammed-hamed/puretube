package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import com.dr.tech.puretube.core.extractor.pack.CuratedStarterPack
import com.dr.tech.puretube.core.extractor.portability.ImportExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    var onSubscriptionsChanged: (suspend () -> Unit)? = null
) {
    private val _subscriptionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val subscriptionEvents: SharedFlow<Unit> = _subscriptionEvents.asSharedFlow()

    private suspend fun notifySubscriptionsChanged() {
        _subscriptionEvents.tryEmit(Unit)
        onSubscriptionsChanged?.invoke()
    }

    /**
     * Emits the reactive list of user subscriptions, ordered by subscription date descending.
     */
    fun getSubscriptionsFlow(): Flow<List<SubscriptionEntity>> = subscriptionDao.getAllFlow()

    /**
     * Checks if a channel is subscribed reactively.
     */
    fun isSubscribed(channelId: String): Flow<Boolean> = subscriptionDao.isSubscribed(channelId)

    /**
     * Returns the one-shot list of all subscriptions on Dispatchers.IO.
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
     */
    suspend fun fetchChannelDetails(channelUrlOrId: String): Result<SubscriptionEntity> {
        val targetUrl = when {
            channelUrlOrId.startsWith("http://") || channelUrlOrId.startsWith("https://") -> channelUrlOrId
            channelUrlOrId.startsWith("@") -> "https://www.youtube.com/$channelUrlOrId"
            else -> "https://www.youtube.com/channel/$channelUrlOrId"
        }

        return extractorThrottler.safeExtract {
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
