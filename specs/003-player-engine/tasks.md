# Tasks Breakdown: Media3 ExoPlayer Engine (003-player-engine)

**Feature Branch / Directory**: `specs/003-player-engine`  
**Spec Reference**: [spec.md](file:///d:/b/puretube/specs/003-player-engine/spec.md) | **Plan**: [plan.md](file:///d:/b/puretube/specs/003-player-engine/plan.md)  

---

## Phase 1: Setup & Manifest Infrastructure

- [X] T001 Declare foreground service permissions (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`) and register `PurePlaybackService` in `app/src/main/AndroidManifest.xml`
- [X] T002 Enable Picture-in-Picture support (`android:supportsPictureInPicture="true"`) and config changes in `app/src/main/AndroidManifest.xml`
- [X] T003 Create stream format domain models in `app/src/main/java/com/dr/tech/puretube/core/data/model/ExtractedVideoDetails.kt`

---

## Phase 2: Foundational Media & Stream Extraction Layer

- [X] T004 [P] Implement `PureMediaSourceFactory` for DASH video/audio stream merging in `app/src/main/java/com/dr/tech/puretube/player/PureMediaSourceFactory.kt`
- [X] T005 [P] Implement `VideoDetailsRepository` and NewPipeExtractor DASH stream extractor in `app/src/main/java/com/dr/tech/puretube/core/data/repository/VideoDetailsRepository.kt`
- [X] T006 Implement `PurePlayerManager` with ExoPlayer, Media3 session binding, and HTTP 403 recovery in `app/src/main/java/com/dr/tech/puretube/player/PurePlayerManager.kt`
- [X] T007 Implement `PurePlaybackService` (AndroidX Media3 `MediaSessionService`) in `app/src/main/java/com/dr/tech/puretube/player/PurePlaybackService.kt`
- [X] T008 Register player and repository components in Koin dependency injection in `app/src/main/java/com/dr/tech/puretube/player/di/PlayerModule.kt`

---

## Phase 3: User Story 1 - Video & Audio Playback with DASH Merging (Priority: P1)

- [X] T009 [US1] Create unit tests for stream URL extraction and format selection in `app/src/test/java/com/dr/tech/puretube/core/data/repository/VideoDetailsRepositoryTest.kt`
- [X] T010 [US1] Implement `PlayerViewModel` managing stream extraction, playback state, and quality selection in `app/src/main/java/com/dr/tech/puretube/features/player/PlayerViewModel.kt`
- [X] T011 [US1] Implement `PlayerVideoSurface` wrapping Media3 `PlayerView` with AndroidView in `app/src/main/java/com/dr/tech/puretube/features/player/components/PlayerVideoSurface.kt`
- [X] T012 [US1] Implement `PlayerControlsOverlay` with play/pause, seek scrubber, and time indicators in `app/src/main/java/com/dr/tech/puretube/features/player/components/PlayerControlsOverlay.kt`

---

## Phase 4: User Story 2 - Audio-Only Background Playback (Priority: P1)

- [X] T013 [US2] Implement audio-only track selection toggle in `PurePlayerManager` in `app/src/main/java/com/dr/tech/puretube/player/PurePlayerManager.kt`
- [X] T014 [US2] Implement notification channel and MediaStyle media notification handling in `app/src/main/java/com/dr/tech/puretube/player/PurePlaybackService.kt`
- [X] T015 [US2] Add audio-only toggle button and state binding in `app/src/main/java/com/dr/tech/puretube/features/player/components/PlayerControlsOverlay.kt`

---

## Phase 5: User Story 3 - Touch Gestures & Picture-in-Picture (Priority: P2)

- [X] T016 [US3] Implement double-tap seeking (+10s/-10s) with animated visual indicator in `app/src/main/java/com/dr/tech/puretube/features/player/components/PlayerVideoSurface.kt`
- [X] T017 [US3] Configure PiP params, auto-enter on home gesture, and lifecycle listeners in `app/src/main/java/com/dr/tech/puretube/MainActivity.kt`
- [X] T018 [US3] Implement PiP mode UI filter hiding all overlays when in PiP mode in `app/src/main/java/com/dr/tech/puretube/features/player/PlayerScreen.kt`

---

## Phase 6: User Story 4 - Mindful Player UI & Channel Integration (Priority: P2)

- [X] T019 [US4] Implement `VideoMetadataSection` displaying channel info and live Room DB subscription toggle in `app/src/main/java/com/dr/tech/puretube/features/player/components/VideoMetadataSection.kt`
- [X] T020 [US4] Implement `MindfulRelatedList` showing only videos from the same channel/subscriptions in `app/src/main/java/com/dr/tech/puretube/features/player/components/MindfulRelatedList.kt`
- [X] T021 [US4] [P] Implement `PlaybackSpeedSheet` (0.75x to 2.0x) in `app/src/main/java/com/dr/tech/puretube/features/player/sheets/PlaybackSpeedSheet.kt`
- [X] T022 [US4] [P] Implement `QualitySelectionSheet` (Auto to 1080p) in `app/src/main/java/com/dr/tech/puretube/features/player/sheets/QualitySelectionSheet.kt`

---

## Phase 7: User Story 5 - Playback Resume & Watch History Sync (Priority: P3)

- [X] T023 [US5] Wire periodic playback position saving (`HistoryRepository.updatePlaybackPosition`) in `app/src/main/java/com/dr/tech/puretube/features/player/PlayerViewModel.kt`
- [X] T024 [US5] Implement automatic initial seek to saved `HistoryEntity.playbackPositionMs` upon video load in `app/src/main/java/com/dr/tech/puretube/features/player/PlayerViewModel.kt`
- [X] T025 [US5] Wire video completion mark (`HistoryRepository.markCompleted`) when video reaches >95% in `app/src/main/java/com/dr/tech/puretube/features/player/PlayerViewModel.kt`

---

## Phase 8: User Story 6 - AI Bridge Hook Integration (Priority: P3)

- [X] T026 [US6] Wire `VideoFrameHook` and `AudioFilterHook` into player initialization in `app/src/main/java/com/dr/tech/puretube/player/PurePlayerManager.kt`

---

## Phase 9: Polish, Navigation Integration & Verification

- [X] T027 Connect `onVideoClick(videoId)` in `MainNavigationShell.kt` and `MainActivity.kt` to present `PlayerScreen`
- [X] T028 Audit all player UI files for strict `PureTheme.colors.*` token compliance and zero Unicode emojis
- [X] T029 Execute `./gradlew test` and `./gradlew assembleDebug` to verify zero compile or runtime regressions
