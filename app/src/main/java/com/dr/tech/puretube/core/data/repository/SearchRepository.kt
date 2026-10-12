package com.dr.tech.puretube.core.data.repository

import androidx.annotation.VisibleForTesting
import com.dr.tech.puretube.core.data.model.SearchFilter
import com.dr.tech.puretube.core.data.model.SearchResultItem
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.search.SearchExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.util.UUID

/**
 * مستودع البحث العام في يوتيوب (فيديوهات + قنوات) عبر NewPipeExtractor.
 * الاستعلام الفارغ يُرجع نجاحاً فارغاً دون أي استخراج (حماية F1).
 */
interface SearchRepository {
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Result<Pair<List<SearchResultItem>, String?>>

    suspend fun getNextPage(pageUrl: String): Result<Pair<List<SearchResultItem>, String?>>
}

class SearchRepositoryImpl(
    private val extractorThrottler: ExtractorThrottler,
    private val customSearchFetcher: (suspend (query: String, filter: SearchFilter) -> Pair<List<SearchResultItem>, String?>)? = null,
    private val customNextPageFetcher: (suspend (pageUrl: String) -> Pair<List<SearchResultItem>, String?>)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SearchRepository {

    private data class PendingSearch(val extractor: SearchExtractor, val page: Page)

    private val pageMutex = Mutex()
    private val pendingPages = linkedMapOf<String, PendingSearch>()

    override suspend fun search(
        query: String,
        filter: SearchFilter
    ): Result<Pair<List<SearchResultItem>, String?>> = withContext(ioDispatcher) {
        if (query.isBlank()) return@withContext Result.success(emptyList<SearchResultItem>() to null)
        if (customSearchFetcher != null) {
            return@withContext runCatching { customSearchFetcher.invoke(query, filter) }
        }

        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val extractor = service.getSearchExtractor(query.trim(), filter.toContentFilter(), "")
            extractor.fetchPage()

            val initialPage = extractor.initialPage
                ?: return@safeExtract emptyList<SearchResultItem>() to null
            val items = mapSearchInfoItems(initialPage.items ?: emptyList())
            items to storeNextPage(extractor, initialPage.nextPage)
        }
    }

    override suspend fun getNextPage(pageUrl: String): Result<Pair<List<SearchResultItem>, String?>> =
        withContext(ioDispatcher) {
            if (pageUrl.isBlank()) return@withContext Result.success(emptyList<SearchResultItem>() to null)
            if (customNextPageFetcher != null) {
                return@withContext runCatching { customNextPageFetcher.invoke(pageUrl) }
            }

            val pending = pageMutex.withLock { pendingPages.remove(pageUrl) }
                ?: return@withContext Result.failure(IOException("انتهت صلاحية صفحة البحث"))

            val extractResult = extractorThrottler.safeExtract {
                val page = pending.extractor.getPage(pending.page)
                val items = mapSearchInfoItems(page.items ?: emptyList())
                items to storeNextPage(pending.extractor, page.nextPage)
            }
            if (extractResult.isFailure) {
                pageMutex.withLock {
                    if (pendingPages.size >= 10) {
                        pendingPages.remove(pendingPages.keys.first())
                    }
                    pendingPages[pageUrl] = pending
                }
            }
            extractResult
        }

    private suspend fun storeNextPage(extractor: SearchExtractor, nextPage: Page?): String? {
        if (nextPage == null || !Page.isValid(nextPage)) return null
        val token = UUID.randomUUID().toString()
        pageMutex.withLock {
            if (pendingPages.size >= 10) {
                pendingPages.remove(pendingPages.keys.first())
            }
            pendingPages[token] = PendingSearch(extractor, nextPage)
        }
        return token
    }
}

/** يستخرج معرّف الفيديو من روابط watch/youtu.be/shorts. */
internal fun extractSearchVideoId(rawUrl: String): String {
    val cleaned = rawUrl.trim()
    return when {
        cleaned.contains("v=") -> cleaned.substringAfter("v=").substringBefore("&").substringBefore("?").substringBefore("#").substringBefore("/")
        cleaned.contains("youtu.be/") -> cleaned.substringAfter("youtu.be/").substringBefore("?").substringBefore("&").substringBefore("#").substringBefore("/")
        cleaned.contains("/shorts/") -> cleaned.substringAfter("/shorts/").substringBefore("?").substringBefore("&").substringBefore("#").substringBefore("/")
        else -> cleaned.substringAfterLast("/").substringBefore("?").substringBefore("&").substringBefore("#")
    }.trim()
}

@VisibleForTesting
internal fun mapStreamInfoItem(item: StreamInfoItem): SearchResultItem.VideoItem? {
    val rawUrl = item.url ?: return null
    val videoId = extractSearchVideoId(rawUrl)
    if (videoId.isBlank() || videoId.contains("/")) return null
    val durationMs = if (item.duration > 0L) item.duration * 1000L else 0L
    return SearchResultItem.VideoItem(
        videoId = videoId,
        title = item.name ?: videoId,
        channelId = item.uploaderUrl?.substringAfterLast("/")?.substringBefore("?")?.trim().orEmpty(),
        channelName = item.uploaderName ?: "",
        thumbnailUrl = item.thumbnails?.firstOrNull()?.url,
        durationMs = durationMs,
        viewCount = item.viewCount.takeIf { it >= 0 } ?: 0L,
        uploadDateText = item.textualUploadDate
    )
}

@VisibleForTesting
internal fun mapChannelInfoItem(item: ChannelInfoItem): SearchResultItem.ChannelItem {
    val rawUrl = item.url ?: ""
    val channelId = when {
        rawUrl.contains("/channel/") -> rawUrl.substringAfter("/channel/").substringBefore("/").substringBefore("?")
        rawUrl.contains("/@") -> "@" + rawUrl.substringAfter("/@").substringBefore("/").substringBefore("?").substringBefore("#")
        rawUrl.contains("/c/") -> rawUrl.substringAfter("/c/").substringBefore("/").substringBefore("?")
        rawUrl.contains("/user/") -> rawUrl.substringAfter("/user/").substringBefore("/").substringBefore("?")
        else -> rawUrl.substringAfterLast("/").substringBefore("?").substringBefore("#")
    }.trim()
    val handle = if (rawUrl.contains("/@")) {
        "@" + rawUrl.substringAfter("/@").substringBefore("/").substringBefore("?").substringBefore("#")
    } else {
        null
    }
    return SearchResultItem.ChannelItem(
        channelId = channelId.ifBlank { rawUrl },
        channelName = item.name ?: channelId,
        handle = handle,
        avatarUrl = item.thumbnails?.firstOrNull()?.url,
        subscriberCountText = item.subscriberCount.takeIf { it > 0L }?.toString()
    )
}

@VisibleForTesting
internal fun mapSearchInfoItems(items: List<InfoItem>): List<SearchResultItem> = items.mapNotNull { item ->
    when (item) {
        is StreamInfoItem -> mapStreamInfoItem(item)
        is ChannelInfoItem -> mapChannelInfoItem(item)
        else -> null
    }
}
