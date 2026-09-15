# Phase 0 Research & Technical Decisions: 003-player-engine

**Feature Branch / Directory**: `specs/003-player-engine`  
**Date**: 2026-09-15  
**Spec Reference**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md)  

---

## 1. Technical Decisions & Trade-Off Analysis

### Decision 1: AndroidX Media3 ExoPlayer Architecture & MediaSessionService
- **Decision**: Implement a foreground `MediaSessionService` (`PurePlaybackService`) hosting the `ExoPlayer` instance, coupled with a `MediaSession`. The UI interacts via a dedicated `PurePlayerManager` and `PlayerViewModel`.
- **Rationale**: Media3 is Google's unified media framework combining playback, media session, and foreground notification management. Hosting `ExoPlayer` in `MediaSessionService` allows continuous audio streaming when the screen turns off or the app is minimized, handles lock screen controls, and conforms to Android 14+ foreground service requirements (`foregroundServiceType="mediaPlayback"`).
- **Alternatives Considered**:
  - *In-Activity ExoPlayer instance*: Simpler to wire directly into Compose, but terminates or stalls when the activity is backgrounded or killed, making background audio and robust PiP difficult. Rejected.

### Decision 2: MergingMediaSource for YouTube Separate DASH Streams
- **Decision**: Use Media3 `MergingMediaSource` to merge independent video (e.g. 1080p, 720p H.264/VP9) and audio (e.g. 128kbps/160kbps AAC/Opus) stream URLs extracted by `NewPipeExtractor`.
- **Rationale**: YouTube does not provide combined progressive streams for resolutions above 720p. For 1080p and higher, video and audio streams are separated. Media3's `MergingMediaSource` handles presentation clock synchronization seamlessly.
- **Alternatives Considered**:
  - *Restricting to progressive single-stream URLs (<=720p)*: Low quality, limits user experience. Rejected.
  - *On-device FFmpeg muxing*: Massive CPU/battery overhead and high latency. Rejected.

### Decision 3: Ephemeral In-Memory Stream Separation & HTTP 403 Auto-Recovery
- **Decision**: In strict adherence to Constitution Principle VIII, YouTube streaming URLs are never persisted in Room DB. They are held exclusively in-memory by `PurePlayerManager`. If an HTTP 403/410 occurs due to YouTube URL expiration, `PurePlayerManager` traps the `HttpDataSourceException` via `Player.Listener.onPlayerError`, issues an in-memory re-extraction via `VideoDetailsRepository`, and transparently resumes at `currentPositionMs`.
- **Rationale**: YouTube signed URL tokens expire within a few hours. Persisting them in SQLite would cause stale links and broken resumes. Re-extracting upon HTTP 403 ensures unbreakable long-term playback sessions.
- **Alternatives Considered**:
  - *Failing immediately and asking user to restart*: Frustrating user experience. Rejected.

### Decision 4: Picture-in-Picture (PiP) and Auto-Enter on Home Gesture
- **Decision**: Integrate Android 12+ (API 31+) `PictureInPictureParams.Builder.setAutoEnterEnabled(true)` in `MainActivity` when video is actively playing. For Android 8.0-11, override `onUserLeaveHint()`. When `isInPictureInPictureMode` is true, Compose renders strictly the `PlayerView` with all overlay controls, scrubbers, and headers completely hidden.
- **Rationale**: Provides smooth OS-level PiP transitions without UI flickering or touch target misalignment.
- **Alternatives Considered**:
  - *Custom in-app floating window (Overlay)*: Requires intrusive `SYSTEM_ALERT_WINDOW` permission, which violates privacy guidelines. Rejected.

### Decision 5: Anti-Addiction Video Recommendations (Channel & Subscriptions Only)
- **Decision**: Below the video player, only display videos from the same channel (via `NewPipeExtractor.getChannel()`) or user-subscribed channels from Room DB. Zero trending feeds, zero algorithmic recommendations.
- **Rationale**: Directly honors Constitution Principle I (Anti-Addiction & Mindful Intent).

---

## 2. Dependencies & Permissions

1. **Gradle Dependencies** (Already declared in `app/build.gradle.kts`):
   - `androidx.media3:media3-exoplayer:1.5.1`
   - `androidx.media3:media3-ui:1.5.1`
   - `androidx.media3:media3-session:1.5.1`
2. **Android Manifest Permissions**:
   - `android.permission.FOREGROUND_SERVICE`
   - `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
   - `android.permission.POST_NOTIFICATIONS` (for Android 13+)
   - `android:supportsPictureInPicture="true"` on `MainActivity`.
