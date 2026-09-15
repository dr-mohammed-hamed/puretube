# Implementation Plan: Media3 ExoPlayer Engine (003-player-engine)

**Branch**: `003-player-engine` | **Date**: 2026-09-15 | **Spec**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md)  

---

## 1. Summary

Phase 4 builds the core media engine of PureTube using AndroidX Media3 ExoPlayer. It introduces:
1. `PurePlaybackService`: Foreground `MediaSessionService` supporting background audio-only playback, lock screen controls, and audio focus management.
2. `PurePlayerManager`: Media3 ExoPlayer wrapper managing DASH stream merging (`MergingMediaSource`), ephemeral token expiration recovery (HTTP 403), playback speed/quality selection, and AI moderation hook bridges (`VideoFrameHook`, `AudioFilterHook`).
3. `VideoDetailsRepository`: Robust stream extraction bridge wrapping `NewPipeExtractor` with `ExtractorThrottler` (`Semaphore(4)`).
4. `PlayerScreen` & `PlayerViewModel`: Mindful Jetpack Compose UI adhering strictly to `PureTheme.colors.*`, Cairo typography, zero emojis, double-tap seek gestures, and related videos restricted strictly to the same author and user subscriptions.
5. Integration into `MainNavigationShell` and `MainActivity`: Linking video clicks from Feed/WatchLater to the player, with seamless Picture-in-Picture (PiP) support.

---

## 2. Technical Context

- **Language/Version**: Kotlin 2.0.21, Java 17, minSdk 24, targetSdk 36.
- **Media Engine**: AndroidX Media3 1.5.1 (`media3-exoplayer`, `media3-ui`, `media3-session`).
- **UI Toolkit**: Jetpack Compose BOM 2024.10.01, Material 3, PureTheme tokens.
- **Dependency Injection**: Koin 3.5.6.
- **Extraction**: NewPipeExtractor 0.24.4, OkHttp 4.12.0 with HTTP cache.
- **Database**: Room 2.7.0 SQLite (`HistoryDao`, `SubscriptionDao`, `WatchLaterDao`).
- **Constraints**: Hard file length ceiling < 1,000 LOC, Composable < 300 LOC, zero emojis, zero hardcoded hex colors.

---

## 3. Constitution Check

*All 7 Gates evaluated and verified for Phase 4:*

- [x] **Gate 1: Anti-Addiction & Intentionality**: No automated recommendation algorithms, no unrequested Shorts, related videos are strictly restricted to the current channel and user's followed channels.
- [x] **Gate 2: AI Safety Hooks**: Video player pipeline integrates `VideoFrameHook` and `AudioFilterHook` (NoOp by default in V1).
- [x] **Gate 3: Privacy & Offline-First SSOT**: Room DB is the sole SSOT for history and resume positions. No tracking, zero telemetry.
- [x] **Gate 4: Network Resilience & Temporal Precision**: `Semaphore(4)` throttling on all extraction calls. Long milliseconds precision for all time offsets. Automatic in-memory recovery from HTTP 403 stream expirations.
- [x] **Gate 5: File Length & 4-Tier Blueprint**: Screen < 300 LOC, subcomponents in `components/`, sheets in `sheets/`, ViewModel < 250 LOC.
- [x] **Gate 6: Zero-Emoji & Theme Compliance**: 100% compliant with `PureTheme.colors.*`. Pure vector icons and Cairo typography. Zero Unicode emojis.
- [x] **Gate 7: Coroutines & Null-Safety Discipline**: Scoped to `viewModelScope`. Busy state debounce on buttons. Zero `!!` double-bang operators. All results wrapped in `Result<T>`.

---

## 4. Project Structure & Blueprint

```text
app/src/main/java/com/dr/tech/puretube/
├── player/
│   ├── PurePlaybackService.kt                 # [NEW] MediaSessionService for background audio
│   ├── PurePlayerManager.kt                   # [NEW] ExoPlayer wrapper & DASH MergingMediaSource
│   ├── PureMediaSourceFactory.kt              # [NEW] Factory merging separate DASH video/audio streams
│   └── di/
│       └── PlayerModule.kt                    # [NEW] Koin module for Player components
│
├── core/
│   └── data/
│       ├── model/
│       │   └── ExtractedVideoDetails.kt       # [NEW] Domain model for video stream formats
│       └── repository/
│           ├── VideoDetailsRepository.kt      # [NEW] Extractor bridge for stream URLs
│           └── VideoDetailsRepositoryImpl.kt  # [NEW] Implementation using NewPipeExtractor
│
└── features/
    └── player/
        ├── PlayerScreen.kt                    # [NEW] Compose root screen (< 300 LOC)
        ├── PlayerViewModel.kt                 # [NEW] StateFlow<PlayerUiState> (< 250 LOC)
        ├── components/
        │   ├── PlayerVideoSurface.kt          # [NEW] AndroidView(PlayerView) with gesture detector
        │   ├── PlayerControlsOverlay.kt       # [NEW] Play/pause, scrubber, speed/quality buttons
        │   ├── VideoMetadataSection.kt        # [NEW] Channel subscribe button, title, view count
        │   └── MindfulRelatedList.kt          # [NEW] Channel-only related videos list
        └── sheets/
            ├── PlaybackSpeedSheet.kt          # [NEW] Speed selection modal bottom sheet
            └── QualitySelectionSheet.kt       # [NEW] Resolution selection modal bottom sheet
```

---

## 5. Subagent Task Decomposition Schema

| Subagent Role | Type | Domain & Responsibilities |
|---|---|---|
| **Subagent 1: Media Engine Specialist** | `self` | Implement `PurePlaybackService`, `PurePlayerManager`, `PureMediaSourceFactory`, HTTP 403 re-extraction, audio-only mode, and `PlayerModule`. |
| **Subagent 2: Repository & Stream Specialist** | `self` | Implement `ExtractedVideoDetails`, `VideoDetailsRepository`, NewPipeExtractor DASH stream parsing, and history resume wiring. |
| **Subagent 3: UI & Gestures Specialist** | `self` | Implement `PlayerScreen`, `PlayerControlsOverlay`, `PlayerVideoSurface` with double-tap gestures, PiP integration, sheets, and PureTheme compliance. |
| **Subagent 4: Neutral Auditor** | `self` | CodeRabbit-grade audit: Verify zero build errors, zero emojis, hard LOC limits, memory leak prevention, and run `./gradlew test`. |
