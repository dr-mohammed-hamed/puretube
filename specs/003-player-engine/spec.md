# Feature Specification: Media3 ExoPlayer Engine (مشغل الوسائط والتشغيل في الخلفية و PiP)

**Feature Branch / Directory**: `specs/003-player-engine`  
**Created**: 2026-09-15  
**Status**: Ready for Implementation  
**Target Milestone**: Phase 4 (مشغل الفيديو والتشغيل في الخلفية - Player Engine)  

---

## 1. Executive Summary & Anti-Addiction Intent

In strict compliance with the **PureTube Constitution (Principles I through IX)**, **SPECIFICATION.md (Section 5 & 6)**, and the **Vibrant Purity Design System (DESIGN_SYSTEM.md)**, Phase 4 delivers the foundational Media3 ExoPlayer playback engine for PureTube.

Phase 4 fulfills the core media consumption promise of PureTube while protecting the user from algorithmic manipulation and dopamine traps:
- **Zero Algorithmic Rabbit Holes**: The player interface displays ZERO automated recommendations, zero trending sidebars, and zero unrequested autoplay loops. Recommended next videos are strictly bounded to videos from the current channel and channels the user has intentionally followed.
- **Resilient DASH Stream Merging**: Merges independent high-definition video (1080p/720p) and audio streams via AndroidX Media3 `MergingMediaSource`.
- **Ephemeral Stream Token Separation**: Streaming URLs and signed tokens are kept strictly in-memory within `PurePlayerManager` (Constitution Principle VIII). Stale stream URLs expiring with HTTP 403 Forbidden are re-extracted automatically in memory without corrupting Room DB.
- **Foreground Audio-Only Playback Service**: Dedicated AndroidX Media3 `MediaSessionService` (`PurePlaybackService`) supporting background audio streaming, lock screen media controls, and headset media button events with negligible battery overhead.
- **Seamless Picture-in-Picture (PiP)**: Smooth transition to PiP mode upon device home/back gesture, automatically hiding distracting UI overlays and controls.
- **Exact Resume & History Tracking**: Playback timestamps and durations are computed and stored as `Long` milliseconds in Room DB via `HistoryRepository` (Constitution Principle IV).
- **AI Hook Readiness**: Modular zero-copy integration with `VideoFrameHook` and `AudioFilterHook` (NoOp in V1 with zero overhead).
- **Zero-Emoji Dignity & PureTheme**: All player controls, icons, and menus exclusively use vector icons (Material/SVG) and semantic color tokens (`PureTheme.colors.*`).

---

## 2. Clarifications (Session 2026-09-15)

- **Q: How should next/related videos below the player be selected without violating anti-addiction rules?**  
  → **A**: Videos shown below the player MUST strictly be fetched from the same channel currently playing and/or the user's explicit subscriptions in Room DB. No external algorithmic recommendations or trending feeds are permitted.
- **Q: How should stream expiration (HTTP 403 Forbidden) be handled during long playback sessions?**  
  → **A**: Intercept `HttpDataSourceException` in `Player.Listener.onPlayerError`. Automatically trigger in-memory re-extraction via `VideoDetailsRepository` / `NewPipeExtractor`, re-assemble `MergingMediaSource`, and resume playback at `currentPositionMs` without interrupting the user or altering persistent Room DB.
- **Q: How should player UI behave when transitioning into Picture-in-Picture (PiP) mode?**  
  → **A**: Instantly hide all UI overlays, scrubbers, buttons, and system bars, rendering purely the clean video surface (`PlayerView`). Re-display controls upon returning to fullscreen.
- **Q: What is the unit of measurement for seek timestamps, video duration, and resume position?**  
  → **A**: Long milliseconds (`Long` ms) across all models, repository layers, and database records to prevent fractional drift.

---

## 3. User Scenarios & Prioritized User Stories

### User Story 1 - High-Fidelity Video & Audio Playback with DASH Merging (Priority: P1)
As a mindful viewer, I want to tap any video card in my Feed, Subscriptions, or Watch Later list and have it play smoothly in full HD (1080p/720p/480p/360p) with synchronized audio, without stuttering or infinite loading spinners.

**Why this priority**: Foundational core functionality of the media app.

**Independent Test**:
Clicking any video card in `FeedScreen` or `WatchLaterScreen` triggers `onVideoClick(videoId)`. The player resolves stream formats, merges video and audio DASH sources, and initiates playback within <1.5s on standard broadband.

**Acceptance Scenarios**:
1. **Given** a valid video ID, **When** the video is loaded, **Then** `VideoDetailsRepository` extracts video and audio stream streams and `PureMediaSourceFactory` merges them into a single `MediaSource`.
2. **Given** 1080p stream availability, **When** "Auto" or "1080p" is selected, **Then** the video plays in 1080p with crystal-clear synchronized audio.
3. **Given** an expired stream URL throwing HTTP 403, **When** the player catches the error, **Then** it transparently fetches fresh stream URLs and resumes from the exact millisecond offset.

---

### User Story 2 - Audio-Only Background Playback & MediaSessionService (Priority: P1)
As a student or commuter listening to an educational lecture or podcast, I want to turn off my device screen or switch to other apps while audio continues playing seamlessly, with controls accessible from the notification shade and lock screen.

**Why this priority**: Essential for anti-screen addiction, allowing purposeful listening without staring at a glowing display.

**Independent Test**:
Tapping the "Audio Only" toggle switches off the video stream surface, keeps the audio track active, and binds to `PurePlaybackService`. Locking the phone keeps audio playing; notification displays video title, channel, play/pause, and seek controls.

**Acceptance Scenarios**:
1. **Given** active playback, **When** the user taps "Audio Only" (صوتي فقط), **Then** ExoPlayer disables video track rendering to conserve battery and data while keeping audio running.
2. **Given** playback in progress, **When** the user presses the power button or switches apps, **Then** `PurePlaybackService` maintains foreground playback with a `MediaStyle` notification.
3. **Given** lock screen controls, **When** user taps pause or seek (+10s / -10s), **Then** the playback state updates immediately.

---

### User Story 3 - Touch Gestures & Picture-in-Picture (PiP) (Priority: P2)
As a mobile user, I want intuitive gestures (double-tap to seek 10s forward/backward, swipe for volume/brightness) and seamless Picture-in-Picture mode when navigating away, so that my viewing remains flexible and effortless.

**Why this priority**: Standard modern Android smartphone experience fulfilling Constitution Principle V.

**Independent Test**:
Double-tapping the right half of the player surface seeks forward 10 seconds with a clean ripple indicator; double-tapping the left seeks backward 10 seconds. Swiping up on home gesture transitions the activity to PiP mode.

**Acceptance Scenarios**:
1. **Given** video playback, **When** the user double-taps the right side, **Then** playback jumps +10,000ms and a "+10" vector badge animates.
2. **Given** video playback, **When** the user double-taps the left side, **Then** playback jumps -10,000ms and a "-10" vector badge animates.
3. **Given** the user navigates home while video is playing, **When** Android triggers PiP transition, **Then** the player collapses into a floating window with all overlay controls hidden.

---

### User Story 4 - Mindful Player UI & Channel Integration (Priority: P2)
As a user watching a video, I want an uncluttered player interface with clear playback speed options (0.75x to 2x), quality selector, channel subscribe button, and a clean list of other videos strictly from the same channel, with zero distraction.

**Why this priority**: Direct implementation of anti-addiction design philosophy.

**Independent Test**:
Below the player view, channel info is displayed with a live "Subscribe" button connected to `SubscriptionDao`. The related videos list contains only videos from the same author.

**Acceptance Scenarios**:
1. **Given** active video playback, **When** inspecting the controls overlay, **Then** quality selector, speed selector (0.75x, 1.0x, 1.25x, 1.5x, 2.0x), and fullscreen buttons are accessible.
2. **Given** the area beneath the player, **When** rendered, **Then** channel name, subscriber count, and dynamic "Subscribe" / "Subscribed" button are shown using Room DB state.
3. **Given** the related videos section, **When** rendered, **Then** only videos from the same channel or user subscriptions are listed.

---

### User Story 5 - Playback Resume & Watch History Sync (Priority: P3)
As a user returning to a previously watched video, I want playback to resume exactly where I left off, and I want my watching progress reflected in the local history.

**Why this priority**: Prevents frustration, respects user time, and maintains local data ownership (Constitution Principle III & VIII).

**Independent Test**:
Playing a video for 45 seconds and exiting records `positionMs = 45000` in `HistoryEntity`. Reopening the video automatically seeks to 45 seconds.

**Acceptance Scenarios**:
1. **Given** a video played up to timestamp T, **When** the user exits or pauses, **Then** `HistoryRepository.updatePlaybackPosition(videoId, T)` records the position in Room DB.
2. **Given** a video with an existing history record, **When** opened in PlayerScreen, **Then** ExoPlayer prepares and immediately seeks to the saved `positionMs`.
3. **Given** video playback finishes (>95% duration), **When** playback ends, **Then** `HistoryRepository.markCompleted(videoId)` resets the resume bookmark.

---

### User Story 6 - Zero-Overhead AI Hook Pipeline Readiness (Priority: P3)
As an architect preparing for V2 on-device AI moderation (`halalify-ai-core`), I want the player pipeline to expose standardized hooks for video frame processing and audio sample filtering without imposing any performance overhead in V1.

**Why this priority**: Fulfills Constitution Principle II (Zero-Copy AI Bridge).

**Independent Test**:
`VideoFrameHook` and `AudioFilterHook` are registered in `PurePlayerManager`. In V1, `NoOpVideoFrameHook` and `NoOpAudioFilterHook` are used, measuring 0 CPU/memory overhead.

---

## 4. Edge Cases & Red-Teamed Failure Modes

1. **HTTP 403 / 410 Expired YouTube Signature**:
   - *Failure*: YouTube signs video/audio URLs with expiring tokens. If playback pauses for 30 minutes, resume fails with HTTP 403.
   - *Mitigation*: Catch `HttpDataSourceException` in `Player.Listener`, transparently trigger in-memory re-extraction via `ExtractorThrottler.safeExtract`, reconstruct `MergingMediaSource`, and seek to last saved `positionMs`.
2. **Audio Becoming Noisy / Headset Disconnect**:
   - *Failure*: Headset unplugs while playing in public or in audio-only mode, blasting speaker sound.
   - *Mitigation*: Configure `AudioAttributes` and enable `ExoPlayer.setHandleAudioBecomingNoisy(true)`. Playback automatically pauses immediately.
3. **PiP Size Distortion & Controller Collision**:
   - *Failure*: Entering PiP while timeline scrubber or quality sheet is open creates UI overlapping and touch target corruption.
   - *Mitigation*: Observe `LocalConfiguration` / Activity PiP state. When `isInPictureInPictureMode == true`, conditionally omit all Compose overlays, showing only the raw video texture.

---

## 5. Non-Functional Quality Attributes & Success Criteria

- **Buffer Time**: < 1,500ms time-to-first-frame on 10 Mbps+ network.
- **Frame Rate**: Smooth 60fps rendering without dropped frames during playback and scrubber interactions.
- **LOC Ceiling**: Every new Kotlin file strictly < 1,000 LOC; every composable < 300 LOC.
- **Design System SSOT**: 100% of UI elements styled via `PureTheme.colors.*`; 0 hardcoded hex colors.
- **Zero-Emoji Discipline**: 0 Unicode emojis in system UI titles, buttons, or dialogs.
- **Defensive Safety**: 0 double-bang `!!` operators; all coroutines scoped to `viewModelScope` or `LifecycleOwner`.
