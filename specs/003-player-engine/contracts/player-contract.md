# Player & Service Interface Contracts: 003-player-engine

**Feature Branch / Directory**: `specs/003-player-engine`  
**Spec Reference**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md)  

---

## 1. PurePlayerController Contract

Defines the player control interface exposed by `PurePlayerManager` to ViewModels and UI.

```kotlin
interface PurePlayerController {
    val player: androidx.media3.common.Player
    val playbackStateFlow: StateFlow<PlayerPlaybackState>
    val currentPositionMsFlow: StateFlow<Long>
    val durationMsFlow: StateFlow<Long>

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekRelative(offsetMs: Long) // e.g. +10,000ms or -10,000ms
    fun setPlaybackSpeed(speed: Float)
    fun setQuality(quality: PlaybackQuality)
    fun setAudioOnly(audioOnly: Boolean)
    fun loadVideo(details: ExtractedVideoDetails, startPositionMs: Long = 0L)
    fun release()
}
```

---

## 2. VideoDetailsRepository Contract

Repository bridging `NewPipeExtractor` with coroutine-based stream resolution and network throttling.

```kotlin
interface VideoDetailsRepository {
    suspend fun extractVideoDetails(videoId: String): Result<ExtractedVideoDetails>
    suspend fun getChannelVideos(channelId: String, limit: Int = 20): Result<List<FeedVideoItem>>
}
```

---

## 3. Playback State Enum

```kotlin
enum class PlayerPlaybackState {
    IDLE,
    BUFFERING,
    READY,
    ENDED,
    ERROR
}
```

---

## 4. AI Bridge Hook Contract (Zero-Copy)

Interface defined in `core/aibridge/AIHooks.kt`:
```kotlin
interface VideoFrameHook {
    fun processFrame(frameBuffer: Any): Any?
    val isEnabled: Boolean
}

interface AudioFilterHook {
    fun processAudioSamples(buffer: ByteBuffer, channelCount: Int, sampleRate: Int)
    val isEnabled: Boolean
}
```
`PurePlayerManager` registers `NoOpVideoFrameHook` and `NoOpAudioFilterHook` by default in V1, ready for V2 `halalify-ai-core`.
