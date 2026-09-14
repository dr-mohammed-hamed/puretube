---
name: android-media3-expert
description: Expert in Android Media3 ExoPlayer architecture, MergingMediaSource for separate DASH streams, background playback MediaSessionService, Picture-in-Picture, and modular AI moderation hooks.
version: 1.0.0
tags: [android, media3, exoplayer, audio, video, streaming, pip, aibridge]
---

# Android Media3 & ExoPlayer Expert

## 1. Overview & Architectural Role

This skill governs the media playback engine, audio-only background services, adaptive streaming, and AI frame/audio hook interfaces using **AndroidX Media3**.

### Core Principles
1. **DASH Stream Merging**: High-resolution video streams (1080p+) often provide video and audio in separate streams. The player must transparently combine them using `MergingMediaSource`.
2. **Background Audio & Session Service**: Background and screen-off playback must be implemented via `MediaSessionService` with proper foreground notifications, lockscreen media controls, and Audio Focus handling.
3. **Picture-in-Picture (PiP) Readiness**: Support smooth auto-enter PiP on user navigation via `PictureInPictureParams` without restarting playback.
4. **Zero-Copy AI Hook Contracts**: Maintain modular, decoupled hook interfaces (`VideoFrameHook` and `AudioFilterHook`) that default to zero-overhead NoOp implementations when disabled.
5. **Playback Resilience & Error Recovery**: Transparently handle stream URL expiration, network buffering gaps, and decoder exceptions with user-friendly recovery states.

---

## 2. General Architecture & Patterns

### 2.1 DASH Stream Merging Pattern
When combining separate adaptive audio and video streams into a unified playback source:

```kotlin
fun buildMergedMediaSource(
    videoSource: MediaSource,
    audioSource: MediaSource
): MediaSource {
    return MergingMediaSource(
        /* adjustPeriodTimeOffsets = */ true,
        /* clipDurations = */ true,
        videoSource,
        audioSource
    )
}
```

### 2.2 Background Playback Architecture (`MediaSessionService`)
- Encapsulate the `ExoPlayer` instance inside a dedicated `MediaSessionService`.
- Expose a `MediaSession` allowing UI components and system notification controllers to interact via standard `MediaController`.
- Properly manage `Player.Listener` events (`onPlaybackStateChanged`, `onIsPlayingChanged`, `onPlayerError`).

### 2.3 Modular AI Hook Contracts (NoOp by Default)
To support real-time on-device moderation (e.g., visual filtering or music isolation) without player coupling or performance penalty when disabled:

```kotlin
interface VideoFrameHook {
    val isEnabled: Boolean
    fun processFrame(frameBuffer: Any): Any?
}

interface AudioFilterHook {
    val isEnabled: Boolean
    fun processAudioSamples(buffer: java.nio.ByteBuffer, channelCount: Int, sampleRate: Int)
}

object NoOpVideoFrameHook : VideoFrameHook {
    override val isEnabled: Boolean = false
    override fun processFrame(frameBuffer: Any): Any? = null
}

object NoOpAudioFilterHook : AudioFilterHook {
    override val isEnabled: Boolean = false
    override fun processAudioSamples(buffer: java.nio.ByteBuffer, channelCount: Int, sampleRate: Int) {}
}
```

---

## 3. Playback Quality & Gestures

- **Resolution Switching**: Provide seamless track selection parameters across available qualities (Auto, 1080p, 720p, 480p, 360p) without stalling the player pipeline.
- **Playback Speed**: Support variable playback rates (0.5x to 2.0x) using `PlaybackParameters(speed)`.
- **Double-Tap Seeking & Scrubbing**: Handle seek offsets smoothly with fast-forward and rewind animations.

---

## 4. Verification & Testing

- Test player lifecycle binding to ensure `player.release()` is always called when the playback session terminates.
- Verify Audio Focus ducking and pause behavior when incoming phone calls or external audio interrupts occur.
