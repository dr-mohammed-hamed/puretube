# Data Model & State Specifications: 003-player-engine

**Feature Branch / Directory**: `specs/003-player-engine`  
**Spec Reference**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md)  

---

## 1. Domain Models & Stream Representations

### VideoStreamFormat
Represents an extracted visual video stream track from YouTube.
```kotlin
data class VideoStreamFormat(
    val url: String,
    val resolution: String, // "1080p", "720p", "480p", "360p"
    val format: String,     // "mp4", "webm"
    val width: Int,
    val height: Int,
    val isVideoOnly: Boolean = true // DASH video-only stream
)
```

### AudioStreamFormat
Represents an extracted audio stream track.
```kotlin
data class AudioStreamFormat(
    val url: String,
    val bitrate: Int,       // in kbps (e.g. 128, 160)
    val format: String,     // "m4a", "opus"
    val averageBitrate: Int = bitrate
)
```

### PlaybackQuality
Enum defining available user-facing resolution choices.
```kotlin
enum class PlaybackQuality(val label: String, val targetHeight: Int) {
    AUTO("تلقائي", 0),
    HD_1080("1080p", 1080),
    HD_720("720p", 720),
    SD_480("480p", 480),
    SD_360("360p", 360)
}
```

### ExtractedVideoDetails
Full resolved video metadata for player screen display.
```kotlin
data class ExtractedVideoDetails(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val channelAvatarUrl: String?,
    val subscriberCount: Long?,
    val durationMs: Long,
    val viewCount: Long,
    val uploadDate: String?,
    val description: String?,
    val videoStreams: List<VideoStreamFormat>,
    val audioStreams: List<AudioStreamFormat>
)
```

---

## 2. UI State & State Transitions

### PlayerUiState
Immutable state representation for `PlayerViewModel` and `PlayerScreen`.
```kotlin
data class PlayerUiState(
    val isLoading: Boolean = true,
    val isBuffering: Boolean = false,
    val isPlaying: Boolean = false,
    val isAudioOnly: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentQuality: PlaybackQuality = PlaybackQuality.AUTO,
    val playbackSpeed: Float = 1.0f,
    val videoDetails: ExtractedVideoDetails? = null,
    val isSubscribed: Boolean = false,
    val isSavedToWatchLater: Boolean = false,
    val relatedVideos: List<FeedVideoItem> = emptyList(),
    val errorMessage: String? = null
)
```

---

## 3. Database State Mapping (SSOT)

| Feature Layer | Entity / Table | Authoritative Responsibility |
|---|---|---|
| **Resume Position** | `HistoryEntity` (`watch_history`) | Stores `playbackPositionMs: Long`, `durationMs: Long`, `timestamp: Long`. Updated periodically during playback. |
| **Channel Status** | `SubscriptionEntity` (`subscriptions`) | Live subscription flag `isSubscribed`. Toggle updates Room DB via `SubscriptionRepository`. |
| **Watch Later** | `WatchLaterEntity` (`watch_later`) | Saved bookmark state. Toggle updates Room DB via `WatchLaterRepository`. |
| **Streaming URLs** | *IN-MEMORY ONLY* | Ephemeral DASH URLs are stored in `PurePlayerManager` state cache. Never saved to Room DB. |
