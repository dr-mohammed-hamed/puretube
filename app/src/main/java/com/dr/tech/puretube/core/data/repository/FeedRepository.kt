package com.dr.tech.puretube.core.data.repository

import androidx.annotation.VisibleForTesting
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.database.dao.SubscriptionDao
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

/**
 * Repository for aggregating video feeds from intentionally subscribed channels.
 * Conforms to Constitution Principles I, IV, and VIII:
 * - Anti-Addiction: Zero recommendation algorithms; strictly reverse-chronological feed from subscriptions.
 * - Concurrency Throttled: Semaphore(4) limit enforced via ExtractorThrottler.
 * - 15-Minute In-Memory Cache: Re-opening within 15 minutes uses cache; re-queries on expiry or pull-to-refresh.
 * - Zero Ephemeral Streaming URLs persisted in database.
 */
class FeedRepository(
    private val subscriptionDao: SubscriptionDao,
    private val extractorThrottler: ExtractorThrottler,
    private val cacheTtlMs: Long = 15 * 60 * 1000L, // 15 minutes default TTL
    private val channelVideosFetcher: (suspend (channelId: String, channelName: String) -> Result<List<FeedVideoItem>>)? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) {
    private val cacheMutex = Mutex()
    private val fetchMutex = Mutex()
    private var cachedFeed: List<FeedVideoItem>? = null
    private var lastFetchedTimestampMs: Long = 0L

    /**
     * Checks if the in-memory cache is currently valid.
     */
    suspend fun isCacheValid(): Boolean = cacheMutex.withLock {
        cachedFeed != null && (System.currentTimeMillis() - lastFetchedTimestampMs < cacheTtlMs)
    }

    /**
     * Clears the in-memory feed cache explicitly.
     */
    suspend fun clearCache() = cacheMutex.withLock {
        cachedFeed = null
        lastFetchedTimestampMs = 0L
    }

    @VisibleForTesting
    internal suspend fun setCachedFeedForTesting(
        feed: List<FeedVideoItem>,
        timestampMs: Long = System.currentTimeMillis()
    ) = cacheMutex.withLock {
        cachedFeed = feed
        lastFetchedTimestampMs = timestampMs
    }

    /**
     * Retrieves the feed of videos from all subscribed channels.
     * Reuses in-memory cache if within the 15-minute window and not force-refreshed.
     * Prevents thundering-herd duplicate network sweeps via fetchMutex.
     */
    suspend fun getFeed(forceRefresh: Boolean = false): Result<List<FeedVideoItem>> = withContext(ioDispatcher) {
        // Fast-path: return cached feed if valid and not force-refreshing
        if (!forceRefresh) {
            cacheMutex.withLock {
                val now = System.currentTimeMillis()
                val valid = cachedFeed?.takeIf { now - lastFetchedTimestampMs < cacheTtlMs }
                if (valid != null) {
                    return@withContext Result.success(valid)
                }
            }
        }

        // Prevent thundering herd: only one fetch executes at a time
        fetchMutex.withLock {
            // Re-check cache inside lock in case another concurrent fetch just succeeded
            if (!forceRefresh) {
                cacheMutex.withLock {
                    val now = System.currentTimeMillis()
                    val valid = cachedFeed?.takeIf { now - lastFetchedTimestampMs < cacheTtlMs }
                    if (valid != null) {
                        android.util.Log.d(TAG, "Serving feed from valid cache (${valid.size} videos, age: ${now - lastFetchedTimestampMs}ms)")
                        return@withContext Result.success(valid)
                    }
                }
            }

            android.util.Log.d(TAG, "Starting feed fetch (forceRefresh=$forceRefresh)...")

            runCatching {
                val subscriptions = subscriptionDao.getAll()
                android.util.Log.d(TAG, "Subscribed channels in database: ${subscriptions.size}")

                if (subscriptions.isEmpty()) {
                    cacheMutex.withLock {
                        cachedFeed = emptyList()
                        lastFetchedTimestampMs = System.currentTimeMillis()
                    }
                    android.util.Log.d(TAG, "No subscriptions present, returning empty feed.")
                    return@runCatching emptyList<FeedVideoItem>()
                }

                // Concurrently fetch recent videos for each channel throttled via ExtractorThrottler
                val channelResults = coroutineScope {
                    val deferredList = subscriptions.map { sub ->
                        async {
                            channelVideosFetcher?.invoke(sub.channelId, sub.channelName)
                                ?: fetchRecentVideosForChannel(sub.channelId, sub.channelName)
                        }
                    }
                    deferredList.awaitAll()
                }

                val successfulFetches = channelResults.filter { it.isSuccess }
                val failedFetches = channelResults.filter { it.isFailure }

                android.util.Log.d(TAG, "Feed channels fetch completed: ${successfulFetches.size}/${subscriptions.size} succeeded, ${failedFetches.size} failed.")

                if (failedFetches.isNotEmpty()) {
                    failedFetches.forEachIndexed { index, result ->
                        android.util.Log.w(TAG, "Channel fetch failure #${index + 1}: ${result.exceptionOrNull()?.message}")
                    }
                }

                // In case all channel fetches fail and subscriptions are not empty:
                // - If cachedFeed is not null and not empty, DO NOT overwrite it with empty list; return cachedFeed!
                // - If no cache exists, throw the network error so runCatching catches it properly and recoverCatching can handle it.
                if (successfulFetches.isEmpty() && subscriptions.isNotEmpty()) {
                    val fallback = cacheMutex.withLock { cachedFeed }
                    if (!fallback.isNullOrEmpty()) {
                        android.util.Log.w(TAG, "All channels failed, falling back to existing cache (${fallback.size} videos).")
                        return@runCatching fallback
                    }

                    val firstError = channelResults.firstOrNull { it.isFailure }?.exceptionOrNull()
                        ?: IOException("Failed to fetch feed from all subscribed channels")
                    android.util.Log.e(TAG, "All channels failed and no cache exists: ${firstError.message}")
                    throw firstError
                }

                val allVideos = successfulFetches.flatMap { it.getOrDefault(emptyList()) }
                android.util.Log.d(TAG, "Total extracted videos across all channels: ${allVideos.size}")

                // Sort reverse-chronologically (newest uploads first)
                val sorted = allVideos.sortedWith(
                    compareByDescending<FeedVideoItem> { it.uploadDateMs }
                        .thenByDescending { it.videoId }
                )

                // Ensure cachedFeed is only updated when at least one channel succeeds or when subscriptions are truly empty
                cacheMutex.withLock {
                    cachedFeed = sorted
                    lastFetchedTimestampMs = System.currentTimeMillis()
                }

                android.util.Log.d(TAG, "Feed ready: ${sorted.size} videos cached successfully.")
                sorted
            }.recoverCatching { error ->
                android.util.Log.e(TAG, "Feed fetch failed with exception: ${error.message}", error)
                // In case of total network failure, return existing cache if available rather than crashing
                cacheMutex.withLock {
                    val fallback = cachedFeed
                    if (!fallback.isNullOrEmpty()) {
                        android.util.Log.w(TAG, "Recovering from error using cache (${fallback.size} videos).")
                        return@recoverCatching fallback
                    }
                }
                throw error
            }
        }
    }

    /**
     * Fetches recent videos from a single channel with Semaphore(4) throttling.
     * Returns Result<List<FeedVideoItem>> to distinguish network failures from empty channel uploads.
     */
    suspend fun fetchRecentVideosForChannel(
        channelId: String,
        channelName: String
    ): Result<List<FeedVideoItem>> {
        val targetUrl = resolveChannelUrl(channelId)
        android.util.Log.d(TAG, "--> Fetching channel: '$channelName' (ID: $channelId, URL: $targetUrl)")

        return extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val channelExtractor = service.getChannelExtractor(targetUrl)
            channelExtractor.fetchPage()

            val tabs = channelExtractor.tabs ?: emptyList()
            android.util.Log.d(TAG, "Channel '$channelName': discovered ${tabs.size} tabs")

            val videoTabHandler = tabs.firstOrNull { tab ->
                val u = tab.url
                u != null && (u.contains("video", ignoreCase = true) || u.contains("/videos"))
            } ?: tabs.firstOrNull { tab ->
                val u = tab.url
                u != null && !u.contains("community", ignoreCase = true) && !u.contains("about", ignoreCase = true) && !u.contains("playlists", ignoreCase = true)
            }

            if (videoTabHandler == null) {
                android.util.Log.w(TAG, "Channel '$channelName': No suitable video tab found among ${tabs.size} tabs")
                throw java.io.IOException("لم يتم العثور على تبويب الفيديوهات للقناة $channelName")
            }

            android.util.Log.d(TAG, "Channel '$channelName': Selected tab URL '${videoTabHandler.url}' (id: ${videoTabHandler.id})")

            val tabExtractor = service.getChannelTabExtractor(videoTabHandler)
            tabExtractor.fetchPage()

            val initialPage = tabExtractor.initialPage
                ?: throw org.schabi.newpipe.extractor.exceptions.ExtractionException("تعذر استخراج الصفحة الأولى لمقاطع الفيديو للقناة $channelName")
            val items = initialPage.items ?: emptyList()
            android.util.Log.d(TAG, "Channel '$channelName': initialPage returned ${items.size} raw items")

            val videos = items.filterIsInstance<StreamInfoItem>().mapNotNull { item: StreamInfoItem ->
                val rawUrl = item.url ?: return@mapNotNull null
                val videoId = extractVideoId(rawUrl)
                if (videoId.isBlank()) return@mapNotNull null

                val title = item.name ?: videoId
                val durationMs = if (item.duration > 0) item.duration * 1000L else 0L
                val uploadDateMs = try {
                    item.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0L
                } catch (_: Throwable) {
                    0L
                }
                val thumbnailUrl = try {
                    item.thumbnails?.firstOrNull()?.url
                } catch (_: Throwable) {
                    null
                }

                FeedVideoItem(
                    videoId = videoId,
                    title = title,
                    channelId = channelId,
                    channelName = item.uploaderName?.ifBlank { null } ?: channelName,
                    thumbnailUrl = thumbnailUrl,
                    durationMs = durationMs,
                    uploadDateMs = uploadDateMs,
                    uploadDateText = item.textualUploadDate
                )
            }

            android.util.Log.d(TAG, "<-- Channel '$channelName': Successfully extracted ${videos.size} videos")
            videos
        }.onFailure { err ->
            android.util.Log.e(TAG, "<-- Channel '$channelName' ($channelId) extraction failed: ${err.javaClass.simpleName} - ${err.message}", err)
        }
    }

    companion object {
        private const val TAG = "PureTube-Feed"

        fun resolveChannelUrl(channelId: String): String {
            val trimmed = channelId.trim().trimEnd('/')
            return when {
                trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
                trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
                trimmed.startsWith("UC") -> "https://www.youtube.com/channel/$trimmed"
                else -> "https://www.youtube.com/@$trimmed"
            }
        }

        fun extractVideoId(url: String): String {
            val cleaned = url.trim()
            return when {
                cleaned.contains("v=") -> cleaned.substringAfter("v=").substringBefore("&")
                cleaned.contains("youtu.be/") -> cleaned.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
                cleaned.contains("/shorts/") -> cleaned.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
                else -> cleaned.substringAfterLast("/")
            }
        }
    }
}
