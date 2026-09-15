package com.dr.tech.puretube.core.data.repository

import com.dr.tech.puretube.core.data.model.AudioStreamFormat
import com.dr.tech.puretube.core.data.model.ExtractedVideoDetails
import com.dr.tech.puretube.core.data.model.FeedVideoItem
import com.dr.tech.puretube.core.data.model.VideoStreamFormat
import com.dr.tech.puretube.core.extractor.ExtractorThrottler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelExtractor
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException

/**
 * Repository interface for extracting video details, DASH streams, and author's videos.
 * Follows Constitution Principles I, IV, and VIII.
 */
interface VideoDetailsRepository {
    /**
     * Extracts full video details including DASH video and audio stream URLs.
     */
    suspend fun extractVideoDetails(videoId: String): Result<ExtractedVideoDetails>

    /**
     * Fetches other videos from the same channel (anti-addiction: no external algorithms).
     */
    suspend fun getChannelVideos(channelId: String, limit: Int = 20): Result<List<FeedVideoItem>>
}

class VideoDetailsRepositoryImpl(
    private val extractorThrottler: ExtractorThrottler,
    private val customStreamFetcher: (suspend (videoId: String) -> ExtractedVideoDetails)? = null,
    private val customChannelVideosFetcher: (suspend (channelId: String, limit: Int) -> List<FeedVideoItem>)? = null,
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
) : VideoDetailsRepository {

    override suspend fun extractVideoDetails(videoId: String): Result<ExtractedVideoDetails> = withContext(ioDispatcher) {
        if (customStreamFetcher != null) {
            return@withContext runCatching { customStreamFetcher.invoke(videoId) }
        }

        val videoUrl = if (videoId.startsWith("http://") || videoId.startsWith("https://")) {
            videoId
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }

        extractorThrottler.safeExtract {
            val streamInfo = StreamInfo.getInfo(ServiceList.YouTube, videoUrl)

            val videoStreams = mutableListOf<VideoStreamFormat>()

            // 1. Separate DASH Video Streams (1080p, 720p, etc.)
            streamInfo.videoOnlyStreams?.forEach { stream ->
                if (stream.content.isNullOrBlank()) return@forEach
                videoStreams.add(
                    VideoStreamFormat(
                        url = stream.content,
                        resolution = stream.resolution ?: "${stream.height}p",
                        format = stream.format?.name ?: "mp4",
                        width = stream.width,
                        height = stream.height,
                        isVideoOnly = true
                    )
                )
            }

            // 2. Progressive combined streams (fallback <=720p)
            streamInfo.videoStreams?.forEach { stream ->
                if (stream.content.isNullOrBlank()) return@forEach
                videoStreams.add(
                    VideoStreamFormat(
                        url = stream.content,
                        resolution = stream.resolution ?: "${stream.height}p",
                        format = stream.format?.name ?: "mp4",
                        width = stream.width,
                        height = stream.height,
                        isVideoOnly = false
                    )
                )
            }

            // 3. Audio Streams
            val audioStreams = mutableListOf<AudioStreamFormat>()
            streamInfo.audioStreams?.forEach { stream ->
                if (stream.content.isNullOrBlank()) return@forEach
                audioStreams.add(
                    AudioStreamFormat(
                        url = stream.content,
                        bitrate = stream.averageBitrate.takeIf { it > 0 } ?: stream.bitrate,
                        format = stream.format?.name ?: "m4a",
                        averageBitrate = stream.averageBitrate
                    )
                )
            }

            val rawDurationSec = streamInfo.duration
            val durationMs = if (rawDurationSec > 0L) rawDurationSec * 1000L else 0L

            ExtractedVideoDetails(
                videoId = streamInfo.id ?: videoId,
                title = streamInfo.name ?: "",
                channelId = streamInfo.uploaderUrl?.substringAfterLast("/") ?: "",
                channelName = streamInfo.uploaderName ?: "",
                channelAvatarUrl = streamInfo.uploaderAvatars?.firstOrNull()?.url,
                thumbnailUrl = streamInfo.thumbnails?.firstOrNull()?.url ?: "https://i.ytimg.com/vi/${streamInfo.id ?: videoId}/hqdefault.jpg",
                subscriberCount = streamInfo.uploaderSubscriberCount.takeIf { it >= 0 },
                durationMs = durationMs,
                viewCount = streamInfo.viewCount.takeIf { it >= 0 } ?: 0L,
                uploadDateText = streamInfo.textualUploadDate,
                description = streamInfo.description?.content,
                videoStreams = videoStreams,
                audioStreams = audioStreams
            )
        }
    }

    override suspend fun getChannelVideos(channelId: String, limit: Int): Result<List<FeedVideoItem>> = withContext(ioDispatcher) {
        if (customChannelVideosFetcher != null) {
            return@withContext runCatching { customChannelVideosFetcher.invoke(channelId, limit) }
        }
        if (channelId.isBlank()) return@withContext Result.success(emptyList())

        val channelUrl = if (channelId.startsWith("http://") || channelId.startsWith("https://")) {
            channelId
        } else if (channelId.startsWith("UC")) {
            "https://www.youtube.com/channel/$channelId"
        } else if (channelId.startsWith("@")) {
            "https://www.youtube.com/$channelId"
        } else {
            "https://www.youtube.com/@$channelId"
        }

        extractorThrottler.safeExtract {
            val service = ServiceList.YouTube
            val channelExtractor = service.getChannelExtractor(channelUrl)
            channelExtractor.fetchPage()

            val tabs = channelExtractor.tabs ?: emptyList()
            val videoTabHandler = tabs.firstOrNull {
                it.url.contains("video", ignoreCase = true)
            } ?: tabs.firstOrNull() ?: return@safeExtract emptyList<FeedVideoItem>()

            val tabExtractor = service.getChannelTabExtractor(videoTabHandler)
            tabExtractor.fetchPage()

            val initialPage = tabExtractor.initialPage ?: return@safeExtract emptyList<FeedVideoItem>()
            val items = initialPage.items ?: return@safeExtract emptyList<FeedVideoItem>()

            items.filterIsInstance<StreamInfoItem>().take(limit).mapNotNull { streamItem ->
                val rawUrl = streamItem.url ?: return@mapNotNull null
                val extractedId = when {
                    rawUrl.contains("v=") -> rawUrl.substringAfter("v=").substringBefore("&")
                    rawUrl.contains("youtu.be/") -> rawUrl.substringAfter("youtu.be/").substringBefore("?")
                    rawUrl.contains("/shorts/") -> rawUrl.substringAfter("/shorts/").substringBefore("?")
                    else -> rawUrl.substringAfterLast("/").substringBefore("?")
                }.trim()
                if (extractedId.isBlank() || extractedId.contains("/")) return@mapNotNull null

                val durationMs = if (streamItem.duration > 0L) streamItem.duration * 1000L else 0L
                FeedVideoItem(
                    videoId = extractedId,
                    title = streamItem.name ?: extractedId,
                    channelId = streamItem.uploaderUrl?.substringAfterLast("/") ?: channelId,
                    channelName = streamItem.uploaderName ?: "",
                    thumbnailUrl = streamItem.thumbnails?.firstOrNull()?.url,
                    durationMs = durationMs,
                    uploadDateText = streamItem.textualUploadDate
                )
            }
        }
    }
}
