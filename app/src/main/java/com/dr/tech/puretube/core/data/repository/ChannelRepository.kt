package com.dr.tech.puretube.core.data.repository

import android.util.Log
import com.dr.tech.puretube.core.data.model.ChannelPlaylistItem
import com.dr.tech.puretube.core.data.model.ChannelProfileDetails
import com.dr.tech.puretube.core.data.model.ChannelSortOrder
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * Repository responsible for extracting and caching channel profiles, videos, playlists,
 * and live streams using NewPipeExtractor under ExtractorThrottler Semaphore(4).
 * Conforms to Constitution Principles I, IV, and VIII.
 */
open class ChannelRepository(
    private val extractorThrottler: ExtractorThrottler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val cacheMutex = Mutex()
    private val profileCache = mutableMapOf<String, ChannelProfileDetails>()
    private val videosCache = mutableMapOf<String, List<FeedVideoItem>>()
    private val playlistsCache = mutableMapOf<String, List<ChannelPlaylistItem>>()
    private val liveCache = mutableMapOf<String, List<FeedVideoItem>>()

    /**
     * Resolves a raw channelId or URL into a standardized YouTube channel URL.
     */
    open fun resolveChannelUrl(channelIdOrUrl: String): String {
        val trimmed = channelIdOrUrl.trim().trimEnd('/')
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.startsWith("@") -> "https://www.youtube.com/$trimmed"
            trimmed.startsWith("UC") -> "https://www.youtube.com/channel/$trimmed"
            else -> "https://www.youtube.com/@$trimmed"
        }
    }

    /**
     * Extracts full channel profile metadata.
     */
    open suspend fun getChannelProfile(
        channelIdOrUrl: String,
        forceRefresh: Boolean = false
    ): Result<ChannelProfileDetails> = withContext(ioDispatcher) {
        val targetUrl = resolveChannelUrl(channelIdOrUrl)
        Log.d(TAG, "getChannelProfile called for '$channelIdOrUrl' (targetUrl: $targetUrl, forceRefresh: $forceRefresh)")

        if (!forceRefresh) {
            cacheMutex.withLock {
                val cached = profileCache[targetUrl] ?: profileCache[channelIdOrUrl]
                if (cached != null) {
                    Log.d(TAG, "Serving channel profile from cache for '$channelIdOrUrl'")
                    return@withContext Result.success(cached)
                }
            }
        }

        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            Log.d(TAG, "--> Extracting channel profile from YouTube: $targetUrl")
            val extractor = service.getChannelExtractor(targetUrl)
            extractor.fetchPage()

            val rawId = try { extractor.id?.ifBlank { null } } catch (_: Throwable) { null } ?: targetUrl.substringAfterLast("/")
            val name = try { extractor.name?.ifBlank { null } } catch (_: Throwable) { null } ?: rawId
            val handle = if (targetUrl.contains("/@")) {
                "@" + targetUrl.substringAfter("/@").substringBefore("/").substringBefore("?").substringBefore("#")
            } else null

            val avatarUrl = try { extractor.avatars?.firstOrNull()?.url } catch (_: Throwable) { null }
            val bannerUrl = try { extractor.banners?.firstOrNull()?.url } catch (_: Throwable) { null }
            val subscriberCount = try { extractor.subscriberCount } catch (_: Throwable) { 0L }
            val subscriberCountText = if (subscriberCount > 0L) "$subscriberCount" else null
            val description = try { extractor.description } catch (_: Throwable) { null }
            val isVerified = try { extractor.isVerified } catch (_: Throwable) { false }

            Log.d(TAG, "<-- Channel profile loaded: name='$name', id='$rawId', handle='$handle', subscribers=$subscriberCount")

            val profile = ChannelProfileDetails(
                channelId = rawId,
                channelName = name,
                channelHandle = handle,
                avatarUrl = avatarUrl,
                bannerUrl = bannerUrl,
                subscriberCount = subscriberCount,
                subscriberCountText = subscriberCountText,
                videoCount = 0L,
                description = description,
                joinedDateText = null,
                viewCount = 0L,
                isVerified = isVerified
            )

            cacheMutex.withLock {
                profileCache[targetUrl] = profile
                profileCache[rawId] = profile
            }

            profile
        }.onFailure { err ->
            Log.e(TAG, "getChannelProfile failed for '$channelIdOrUrl': ${err.javaClass.simpleName} - ${err.message}", err)
        }
    }

    /**
     * Extracts channel videos with sorting support (LATEST, POPULAR, OLDEST).
     */
    open suspend fun getChannelVideos(
        channelIdOrUrl: String,
        sortOrder: ChannelSortOrder = ChannelSortOrder.LATEST,
        forceRefresh: Boolean = false,
        limit: Int = 50
    ): Result<List<FeedVideoItem>> = withContext(ioDispatcher) {
        val targetUrl = resolveChannelUrl(channelIdOrUrl)
        Log.d(TAG, "getChannelVideos called for '$channelIdOrUrl' (targetUrl: $targetUrl, sortOrder: $sortOrder, forceRefresh: $forceRefresh)")

        val rawVideos: List<FeedVideoItem> = if (!forceRefresh) {
            val cached = cacheMutex.withLock { videosCache[targetUrl] ?: videosCache[channelIdOrUrl] }
            if (cached != null) {
                Log.d(TAG, "Serving ${cached.size} channel videos from cache for '$channelIdOrUrl'")
            }
            cached ?: fetchAndCacheVideos(targetUrl, channelIdOrUrl, limit).getOrElse { return@withContext Result.failure(it) }
        } else {
            fetchAndCacheVideos(targetUrl, channelIdOrUrl, limit).getOrElse { return@withContext Result.failure(it) }
        }

        val hasUploadDates = rawVideos.any { it.uploadDateMs > 0L }
        val sorted = when (sortOrder) {
            ChannelSortOrder.LATEST -> if (hasUploadDates) {
                rawVideos.sortedWith(
                    compareByDescending<FeedVideoItem> { it.uploadDateMs }
                        .thenByDescending { it.videoId }
                )
            } else {
                rawVideos
            }
            ChannelSortOrder.POPULAR -> rawVideos.sortedByDescending { it.viewCount }
            ChannelSortOrder.OLDEST -> if (hasUploadDates) {
                rawVideos.sortedWith(
                    compareBy<FeedVideoItem> { if (it.uploadDateMs > 0L) it.uploadDateMs else Long.MAX_VALUE }
                        .thenBy { it.videoId }
                )
            } else {
                rawVideos.reversed()
            }
        }

        Result.success(sorted)
    }

    private suspend fun fetchAndCacheVideos(
        targetUrl: String,
        channelKey: String,
        limit: Int
    ): Result<List<FeedVideoItem>> = extractorThrottler.safeExtract {
        val service = ServiceList.YouTube
        Log.d(TAG, "--> fetchAndCacheVideos for '$channelKey' (URL: $targetUrl)")
        val channelExtractor = service.getChannelExtractor(targetUrl)
        channelExtractor.fetchPage()

        val tabs = try { channelExtractor.tabs ?: emptyList() } catch (_: Throwable) { emptyList() }
        Log.d(TAG, "Channel '$channelKey': discovered ${tabs.size} tabs")

        val videoTabHandler = tabs.firstOrNull { tab ->
            val u = tab.url
            u != null && (u.contains("video", ignoreCase = true) || u.contains("/videos"))
        } ?: tabs.firstOrNull { tab ->
            val u = tab.url
            u != null && !u.contains("community", ignoreCase = true) && !u.contains("about", ignoreCase = true) && !u.contains("playlists", ignoreCase = true)
        } ?: throw java.io.IOException("لم يتم العثور على محتوى مرئي للقناة")

        Log.d(TAG, "Channel '$channelKey': Selected tab URL '${videoTabHandler.url}' (id: ${videoTabHandler.id})")

        val tabExtractor = service.getChannelTabExtractor(videoTabHandler)
        tabExtractor.fetchPage()

        val initialPage = tabExtractor.initialPage
            ?: throw org.schabi.newpipe.extractor.exceptions.ExtractionException("تعذر استخراج الصفحة الأولى لمقاطع الفيديو")
        val items = initialPage.items ?: emptyList()
        Log.d(TAG, "Channel '$channelKey': initialPage returned ${items.size} raw items")

        val channelName = try { channelExtractor.name ?: "" } catch (_: Throwable) { "" }
        val channelId = try { channelExtractor.id ?: channelKey } catch (_: Throwable) { channelKey }
        val channelAvatarUrl = try {
            channelExtractor.avatars?.firstOrNull()?.url?.ifBlank { null }
        } catch (_: Throwable) {
            null
        }

        val videos = items.filterIsInstance<StreamInfoItem>().take(limit).mapNotNull { item ->
            val rawUrl = item.url ?: return@mapNotNull null
            val videoId = when {
                rawUrl.contains("v=") -> rawUrl.substringAfter("v=").substringBefore("&")
                rawUrl.contains("youtu.be/") -> rawUrl.substringAfter("youtu.be/").substringBefore("?")
                rawUrl.contains("/shorts/") -> rawUrl.substringAfter("/shorts/").substringBefore("?")
                else -> rawUrl.substringAfterLast("/").substringBefore("?")
            }.trim()
            if (videoId.isBlank() || videoId.contains("/")) return@mapNotNull null

            val durationMs = if (item.duration > 0L) item.duration * 1000L else 0L
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

            val uploaderAvatar = try {
                item.uploaderAvatars?.firstOrNull()?.url?.ifBlank { null }
            } catch (_: Throwable) {
                null
            } ?: channelAvatarUrl

            FeedVideoItem(
                videoId = videoId,
                title = item.name ?: videoId,
                channelId = channelId,
                channelName = item.uploaderName?.ifBlank { null } ?: channelName,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
                uploadDateMs = uploadDateMs,
                uploadDateText = item.textualUploadDate,
                viewCount = if (item.viewCount >= 0) item.viewCount else 0L,
                channelAvatarUrl = uploaderAvatar
            )
        }

        Log.d(TAG, "<-- Channel '$channelKey': Successfully parsed ${videos.size} videos")

        if (videos.isNotEmpty()) {
            cacheMutex.withLock {
                videosCache[targetUrl] = videos
                videosCache[channelKey] = videos
            }
        }

        videos
    }.onFailure { err ->
        Log.e(TAG, "<-- Channel '$channelKey' fetchAndCacheVideos failed: ${err.javaClass.simpleName} - ${err.message}", err)
    }

    /**
     * Extracts channel playlists.
     */
    open suspend fun getChannelPlaylists(
        channelIdOrUrl: String,
        forceRefresh: Boolean = false
    ): Result<List<ChannelPlaylistItem>> = withContext(ioDispatcher) {
        val targetUrl = resolveChannelUrl(channelIdOrUrl)

        if (!forceRefresh) {
            cacheMutex.withLock {
                val cached = playlistsCache[targetUrl] ?: playlistsCache[channelIdOrUrl]
                if (cached != null) return@withContext Result.success(cached)
            }
        }

        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val channelExtractor = service.getChannelExtractor(targetUrl)
            channelExtractor.fetchPage()

            val tabs = try { channelExtractor.tabs ?: emptyList() } catch (_: Throwable) { emptyList() }
            val playlistTabHandler = tabs.firstOrNull {
                it.url?.contains("playlist", ignoreCase = true) == true
            } ?: return@safeExtract emptyList<ChannelPlaylistItem>()

            val tabExtractor = service.getChannelTabExtractor(playlistTabHandler)
            tabExtractor.fetchPage()

            val initialPage = tabExtractor.initialPage
                ?: throw org.schabi.newpipe.extractor.exceptions.ExtractionException("تعذر استخراج الصفحة الأولى لقوائم التشغيل")
            val items = initialPage.items ?: emptyList()

            val playlists = items.filterIsInstance<PlaylistInfoItem>().mapNotNull { item ->
                val rawUrl = item.url ?: return@mapNotNull null
                val playlistId = when {
                    rawUrl.contains("list=") -> rawUrl.substringAfter("list=").substringBefore("&")
                    else -> rawUrl.substringAfterLast("/").substringBefore("?")
                }.trim()
                if (playlistId.isBlank()) return@mapNotNull null

                val thumb = try {
                    item.thumbnails?.firstOrNull()?.url
                } catch (_: Throwable) {
                    null
                }

                ChannelPlaylistItem(
                    playlistId = playlistId,
                    title = item.name ?: playlistId,
                    thumbnailUrl = thumb,
                    streamCount = item.streamCount,
                    uploaderName = item.uploaderName
                )
            }

            if (playlists.isNotEmpty()) {
                cacheMutex.withLock {
                    playlistsCache[targetUrl] = playlists
                    playlistsCache[channelIdOrUrl] = playlists
                }
            }

            playlists
        }
    }

    /**
     * Extracts live streams / live tab items.
     */
    open suspend fun getChannelLiveStreams(
        channelIdOrUrl: String,
        forceRefresh: Boolean = false
    ): Result<List<FeedVideoItem>> = withContext(ioDispatcher) {
        val targetUrl = resolveChannelUrl(channelIdOrUrl)

        if (!forceRefresh) {
            cacheMutex.withLock {
                val cached = liveCache[targetUrl] ?: liveCache[channelIdOrUrl]
                if (cached != null) return@withContext Result.success(cached)
            }
        }

        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val channelExtractor = service.getChannelExtractor(targetUrl)
            channelExtractor.fetchPage()

            val tabs = try { channelExtractor.tabs ?: emptyList() } catch (_: Throwable) { emptyList() }
            val streamsTabHandler = tabs.firstOrNull {
                val u = it.url
                u != null && (u.contains("stream", ignoreCase = true) || u.contains("live", ignoreCase = true))
            } ?: return@safeExtract emptyList<FeedVideoItem>()

            val tabExtractor = service.getChannelTabExtractor(streamsTabHandler)
            tabExtractor.fetchPage()

            val initialPage = tabExtractor.initialPage
                ?: throw org.schabi.newpipe.extractor.exceptions.ExtractionException("تعذر استخراج الصفحة الأولى للبث المباشر")
            val items = initialPage.items ?: emptyList()

            val channelName = try { channelExtractor.name ?: "" } catch (_: Throwable) { "" }
            val channelId = try { channelExtractor.id ?: channelIdOrUrl } catch (_: Throwable) { channelIdOrUrl }
            val channelAvatarUrl = try {
                channelExtractor.avatars?.firstOrNull()?.url?.ifBlank { null }
            } catch (_: Throwable) {
                null
            }

            val liveVideos = items.filterIsInstance<StreamInfoItem>().mapNotNull { item ->
                val rawUrl = item.url ?: return@mapNotNull null
                val videoId = when {
                    rawUrl.contains("v=") -> rawUrl.substringAfter("v=").substringBefore("&")
                    rawUrl.contains("youtu.be/") -> rawUrl.substringAfter("youtu.be/").substringBefore("?")
                    else -> rawUrl.substringAfterLast("/").substringBefore("?")
                }.trim()
                if (videoId.isBlank()) return@mapNotNull null

                val durationMs = if (item.duration > 0L) item.duration * 1000L else 0L
                val uploadDateMs = try {
                    item.uploadDate?.offsetDateTime()?.toInstant()?.toEpochMilli() ?: 0L
                } catch (_: Throwable) {
                    0L
                }
                val thumb = try {
                    item.thumbnails?.firstOrNull()?.url
                } catch (_: Throwable) {
                    null
                }

                val uploaderAvatar = try {
                    item.uploaderAvatars?.firstOrNull()?.url?.ifBlank { null }
                } catch (_: Throwable) {
                    null
                } ?: channelAvatarUrl

                FeedVideoItem(
                    videoId = videoId,
                    title = item.name ?: videoId,
                    channelId = channelId,
                    channelName = item.uploaderName?.ifBlank { null } ?: channelName,
                    thumbnailUrl = thumb,
                    durationMs = durationMs,
                    uploadDateMs = uploadDateMs,
                    uploadDateText = item.textualUploadDate,
                    viewCount = if (item.viewCount >= 0) item.viewCount else 0L,
                    channelAvatarUrl = uploaderAvatar
                )
            }

            if (liveVideos.isNotEmpty()) {
                cacheMutex.withLock {
                    liveCache[targetUrl] = liveVideos
                    liveCache[channelIdOrUrl] = liveVideos
                }
            }

            liveVideos
        }
    }

    /**
     * Explicitly clears in-memory caches for all or specific channel.
     */
    suspend fun clearCache(channelIdOrUrl: String? = null) = cacheMutex.withLock {
        if (channelIdOrUrl == null) {
            profileCache.clear()
            videosCache.clear()
            playlistsCache.clear()
            liveCache.clear()
        } else {
            val targetUrl = resolveChannelUrl(channelIdOrUrl)
            profileCache.remove(targetUrl)
            profileCache.remove(channelIdOrUrl)
            videosCache.remove(targetUrl)
            videosCache.remove(channelIdOrUrl)
            playlistsCache.remove(targetUrl)
            playlistsCache.remove(channelIdOrUrl)
            liveCache.remove(targetUrl)
            liveCache.remove(channelIdOrUrl)
        }
    }

    companion object {
        private const val TAG = "PureTube-Channel"
    }
}
