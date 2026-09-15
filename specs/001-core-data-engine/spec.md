# Feature Specification: Core Data Engine & Extractor (محرك البيانات والاستخراج)

**Feature Directory**: `specs/001-core-data-engine`  
**Created**: 2026-09-14  
**Status**: Draft  
**Target Milestone**: Phase 2 (محرك البيانات والاستخراج وقاعدة بيانات Room)  

---

## 1. Executive Summary & Anti-Addiction Intent

In accordance with the **PureTube Constitution (Principles I, III, IV, VIII, IX)** and **SPECIFICATION.md**, Phase 2 delivers the offline-first data core and network-resilient extraction pipeline for PureTube. 

This engine is strictly built to uphold mindful, intentional consumption:
- **No Algorithm / No Recommendations**: There are zero discovery recommendation tables or predictive feeds.
- **Local Sovereignty (Room SQLite)**: Channel subscriptions, Watch Later items, playback positions, and viewing history exist purely on-device with zero telemetry and full export/import portability (NewPipe JSON & Google Takeout).
- **Network Politeness & Throttling**: YouTube communication via `NewPipeExtractor` is throttled through `ExtractorThrottler` (`Semaphore(4)` concurrency limit) on `Dispatchers.IO` to eliminate IP bans and memory leaks.
- **Stream Ephemerality**: Expiring DASH streams and YouTube signed tokens are kept strictly in-memory by `PurePlayerManager` and are never persisted in the database.

---

## 2. User Scenarios & Prioritized User Stories

### User Story 1 - Automatic Curated Starter Pack Seeding & Activation (Priority: P1)
As a user opening PureTube for the first time, I want the app to automatically seed and activate a pre-vetted "Curated Safe Pack" (قرآن، برامج نافعة، قنوات تعليمية موثوقة) if my subscriptions list is empty, so that I immediately encounter wholesome, beneficial content and never face an empty or depressing screen.

**Why this priority**: Solves the "blank slate" depression problem on initial boot (avoiding the clinical deprivation trap) while upholding intentional, algorithm-free viewing.

**Independent Test**:
Can be verified by querying `SubscriptionDao.getAllFlow()`. On first app launch with an empty database, the database is automatically seeded with the curated channels via Room `@Transaction`.

**Acceptance Scenarios**:
1. **Given** a fresh app installation with 0 subscriptions, **When** the app initializes the database, **Then** the Curated Starter Pack channels are automatically batch-inserted into `SubscriptionEntity`.
2. **Given** an existing user who has unsubscribed from some channels or added others, **When** the app boots again, **Then** auto-seeding is skipped and user preferences are strictly preserved.

---

### User Story 2 - Resilient Feed Aggregation from Subscriptions (Priority: P1)
As a mindful user, I want my video feed to show the latest videos published strictly by my subscribed channels, sorted reverse-chronologically, so that I only see content from sources I intentionally chose to follow.

**Why this priority**: Core value proposition of PureTube — intentional, algorithm-free viewing.

**Independent Test**:
Given 10 subscribed channels in Room DB, calling `FeedRepository.getAggregatedFeed()` queries NewPipeExtractor with maximum 4 concurrent requests, merges latest videos in memory, sorts by upload date descending, and returns within 5 seconds without UI stutter.

**Acceptance Scenarios**:
1. **Given** 15 subscribed channels, **When** the feed is refreshed, **Then** channel updates are fetched in batches using `Semaphore(4)` on background threads without blocking the main UI thread.
2. **Given** one channel extraction fails due to a network glitch or YouTube signature change, **When** aggregating the feed, **Then** the remaining 14 channels load successfully, and the error is isolated gracefully (`Result<T>` wrapping).

---

### User Story 3 - Mindful Watch Later Management (Priority: P2)
As a user encountering a beneficial long-form lecture or tutorial, I want to save the video to "Watch Later" so that I can schedule intentional viewing rather than impulsive consumption.

**Why this priority**: Essential mindful utility allowing users to postpone viewing to appropriate times.

**Independent Test**:
Calling `WatchLaterRepository.addToWatchLater(video)` inserts the item into `WatchLaterEntity` in Room DB. The reactive Flow immediately updates downstream collectors.

**Acceptance Scenarios**:
1. **Given** a video details view, **When** the user taps "Watch Later", **Then** the video is stored in `WatchLaterEntity` with exact millisecond duration (`Long`), timestamp, title, and channel info.
2. **Given** a saved video, **When** the user removes it or marks it as watched, **Then** the record is cleanly deleted from Room DB.

---

### User Story 4 - Playback History & Exact Resume Position (Priority: P2)
As a user studying an in-depth lecture, I want the app to remember my exact playback timestamp (in milliseconds) so that when I resume later, it begins exactly where I left off without scrubbing.

**Why this priority**: Eliminates frustration when resuming long-form educational content and lectures.

**Independent Test**:
Updating playback offset via `HistoryRepository.updatePlaybackPosition(videoId, positionMs)` records the timestamp in `HistoryEntity`. Querying `getPlaybackPosition(videoId)` returns the exact millisecond offset.

**Acceptance Scenarios**:
1. **Given** an active playback session at 14 minutes and 32 seconds (`872000L` ms), **When** playback pauses or the user exits, **Then** `lastPlaybackPositionMs` is written to Room DB on `Dispatchers.IO`.
2. **Given** a video watched to 95% completion, **When** playback finishes, **Then** the video is marked as `isCompleted = true` and resume position resets to 0.

---

### User Story 5 - Data Portability & Subscription Import/Export (Priority: P3)
As a user transitioning from NewPipe or Google Takeout, I want to import my existing subscription file (JSON / CSV) into PureTube, and also be able to export my subscriptions to an offline backup at any time.

**Why this priority**: Privacy and user data sovereignty (Constitution Principle III).

**Independent Test**:
Passing a valid NewPipe subscription JSON string or Google Takeout subscriptions CSV string to `SubscriptionRepository.importSubscriptions(data, format)` successfully parses and populates `SubscriptionEntity` in Room.

**Acceptance Scenarios**:
1. **Given** a NewPipe export JSON file containing 25 channels, **When** imported, **Then** all 25 channels are verified, deduplicated, and inserted into Room DB.
2. **Given** an export action, **When** triggered, **Then** the app generates a clean JSON backup file containing all current subscriptions.

---

## 3. Edge Cases & Failure Modes

1. **Network Disconnection / Flight Mode**:
   - What happens when feed refresh is triggered offline?
   - Handling: `FeedRepository` emits cached feed results or returns `Result.failure(NetworkException)` with a dignified Cairo-typography message indicating offline status.
2. **Rapid Double-Tap on Watch Later / Subscribe**:
   - What happens when the user rapidly taps the subscribe button 5 times?
   - Handling: Protected by UI state debouncing + Room DB `OnConflictStrategy.REPLACE` / `IGNORE` to guarantee idempotency and zero duplicate rows.
3. **Channel Renaming or Deletion on YouTube**:
   - What happens when a subscribed channel is deleted or renamed on YouTube?
   - Handling: The local subscription remains intact in Room DB. `FeedRepository` logs extraction failure for that channel ID and skips it without corrupting other feeds.
4. **Mass Subscription Import (500+ Channels)**:
   - What happens if a user imports a Takeout file with 500 channels?
   - Handling: Channel records are inserted into SQLite in chunked transactions of 100 items. Background thumbnail resolution and feed polling are throttled via `Semaphore(4)` to prevent memory spikes.

---

## 4. Requirements & Specifications

### Functional Requirements

- **FR-001**: The system MUST implement a Room SQLite database (`PureTubeDatabase`) containing tables for Subscriptions, Watch Later, and History.
- **FR-002**: All temporal fields (timestamps, durations, seek offsets) MUST be stored as `Long` integers in milliseconds to prevent fractional drift.
- **FR-003**: Ephemeral YouTube streaming URLs and signed DASH manifests MUST NOT be stored in Room DB (Constitution Principle VIII).
- **FR-004**: The system MUST provide `SubscriptionDao` exposing reactive `Flow<List<SubscriptionEntity>>` for real-time UI updates.
- **FR-005**: The system MUST provide `WatchLaterDao` exposing reactive `Flow<List<WatchLaterEntity>>`.
- **FR-006**: The system MUST provide `HistoryDao` with upsert support and playback offset tracking in milliseconds.
- **FR-007**: YouTube extractions MUST be throttled through `ExtractorThrottler` using `Semaphore(4)` on `Dispatchers.IO`.
- **FR-008**: The system MUST provide a pre-configured `CuratedStarterPack` containing verified beneficial channels (Quran, education, wholesome tech/nature), automatically seeded on first app boot if the subscriptions table is empty, and re-activatable on demand.
- **FR-009**: The system MUST provide `SubscriptionRepository` to handle channel subscription lifecycle, starter pack seeding, and channel metadata retrieval.
- **FR-010**: The system MUST provide `FeedRepository` to aggregate recent videos from subscribed channels, sort them by upload date, and maintain an in-memory cache with a 15-minute TTL to prevent redundant YouTube requests when re-opening the app.
- **FR-011**: The system MUST provide `WatchLaterRepository` to add, remove, and query watch later items.
- **FR-012**: The system MUST provide `HistoryRepository` to record playback history, update resume offsets, and clear history.
- **FR-013**: The system MUST support importing subscriptions from NewPipe JSON format and Google Takeout CSV/JSON format.
- **FR-014**: The system MUST support exporting subscriptions to a local JSON backup file.
- **FR-015**: All Room database DAOs, Repositories, and Extractor services MUST be registered and injected via Koin (`databaseModule`, `repositoryModule`).
- **FR-016**: The build configuration MUST include KSP plugin (`2.0.21-1.0.28`) and `androidx.room:room-compiler` to enable annotation processing for Room 2.6.1.

### Key Entities

- **SubscriptionEntity**:
  - `channelId`: String (Primary Key, e.g. "UC...")
  - `channelName`: String
  - `channelHandle`: String? (e.g. "@channel")
  - `avatarUrl`: String?
  - `subscriberCountText`: String?
  - `isNotificationsEnabled`: Boolean (default: true)
  - `subscribedAtMs`: Long (System timestamp in ms)

- **WatchLaterEntity**:
  - `videoId`: String (Primary Key, YouTube 11-char ID)
  - `title`: String
  - `channelId`: String
  - `channelName`: String
  - `thumbnailUrl`: String?
  - `durationMs`: Long
  - `addedAtMs`: Long

- **HistoryEntity**:
  - `videoId`: String (Primary Key)
  - `title`: String
  - `channelId`: String
  - `channelName`: String
  - `thumbnailUrl`: String?
  - `durationMs`: Long
  - `lastPlaybackPositionMs`: Long
  - `lastPlayedAtMs`: Long
  - `isCompleted`: Boolean

---

## 5. Success Criteria & Quality Metrics

### Measurable Outcomes

- **SC-001**: Room SQLite compilation and schema verification pass with 0 warnings or errors via `./gradlew assembleDebug`.
- **SC-002**: Aggregating feeds from 10 subscribed channels finishes in under 3.5 seconds on average network conditions.
- **SC-003**: 100% of Room DAO queries and mutations run on `Dispatchers.IO`, ensuring 0 frame drops on the Main UI thread.
- **SC-004**: Memory consumption during concurrent feed fetching remains under 85MB peak heap usage.
- **SC-005**: All unit tests for DAOs, Repositories, and Parser converters pass with 100% success rate (`./gradlew test`).
- **SC-006**: Subscriptions import parser successfully parses both 100-channel NewPipe JSON and Google Takeout CSV formats without data loss.

---

## 6. Assumptions & Scope Boundaries

- **Assumption 1**: Devices have Android 7.0 (API 24) or higher.
- **Assumption 2**: NewPipeExtractor library provides up-to-date YouTube parsing logic, backed by our `PureDownloader` OkHttp client with disk caching.
- **Scope Boundary for Phase 2**: Phase 2 delivers the pure Data Engine, Room Database, Extractor services, Repositories, and Koin modules. The Composable UI screens (FeedScreen, SubscriptionsScreen, WatchLaterScreen, PlayerScreen) belong strictly to Phase 3 & 4. Phase 2 verifies functionality via comprehensive Android unit tests and repository instrumentation.
