# Tasks: Core Data Engine & Extractor (001-core-data-engine)

**Input**: Design documents from `specs/001-core-data-engine/`  
**Prerequisites**: [spec.md](file:///d:/b/puretube/specs/001-core-data-engine/spec.md), [plan.md](file:///d:/b/puretube/specs/001-core-data-engine/plan.md)  
**Status**: Ready for execution upon user approval  

---

## Format: `[ID] [P?] [Story] Description`
- **[P]**: Parallelizable task
- **[Story]**: Related User Story (US1 = Starter Pack, US2 = Feed, US3 = Watch Later, US4 = History, US5 = Import/Export)

---

## Phase 1: Build & Dependencies Setup (Prerequisites)

- [x] **T001**: Configure KSP `2.0.21-1.0.28` and `androidx.room:room-compiler` in [gradle/libs.versions.toml](file:///d:/b/puretube/gradle/libs.versions.toml).
- [x] **T002**: Register KSP plugin alias in root [build.gradle.kts](file:///d:/b/puretube/build.gradle.kts).
- [x] **T003**: Apply KSP plugin and add `ksp(libs.room.compiler)` in [app/build.gradle.kts](file:///d:/b/puretube/app/build.gradle.kts).
- [x] **T004**: Verify Gradle build sync and KSP processing with a quick dry compile.

---

## Phase 2: Foundational Room SQLite Architecture

- [x] **T005** `[P]`: Create `SubscriptionEntity` in [app/.../core/database/entity/SubscriptionEntity.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/entity/SubscriptionEntity.kt).
- [x] **T006** `[P]`: Create `WatchLaterEntity` in [app/.../core/database/entity/WatchLaterEntity.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/entity/WatchLaterEntity.kt).
- [x] **T007** `[P]`: Create `HistoryEntity` in [app/.../core/database/entity/HistoryEntity.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/entity/HistoryEntity.kt).
- [x] **T008** `[P]`: Implement `SubscriptionDao` in [app/.../core/database/dao/SubscriptionDao.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/dao/SubscriptionDao.kt).
- [x] **T009** `[P]`: Implement `WatchLaterDao` in [app/.../core/database/dao/WatchLaterDao.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/dao/WatchLaterDao.kt).
- [x] **T010** `[P]`: Implement `HistoryDao` in [app/.../core/database/dao/HistoryDao.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/dao/HistoryDao.kt).
- [x] **T011**: Implement `PureTubeDatabase` in [app/.../core/database/PureTubeDatabase.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/PureTubeDatabase.kt).
- [x] **T012**: Define `databaseModule` in [app/.../core/database/di/DatabaseModule.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/database/di/DatabaseModule.kt).

---

## Phase 3: User Story 1 & 5 — Curated Starter Pack & Portability

- [x] **T013** `[US1]`: Implement `CuratedStarterPack` catalog with approved channels (Quran, education, tech) in [app/.../core/extractor/pack/CuratedStarterPack.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/extractor/pack/CuratedStarterPack.kt).
- [x] **T014** `[US5]`: Implement `ImportExportService` for NewPipe JSON and Google Takeout CSV/JSON in [app/.../core/extractor/portability/ImportExportService.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/extractor/portability/ImportExportService.kt).
- [x] **T015** `[US1/US5]`: Implement `SubscriptionRepository` in [app/.../core/data/repository/SubscriptionRepository.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/data/repository/SubscriptionRepository.kt) with starter pack activation and import/export methods.

---

## Phase 4: User Story 2 — Resilient Feed Aggregation

- [x] **T016** `[US2]`: Implement `FeedRepository` in [app/.../core/data/repository/FeedRepository.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/data/repository/FeedRepository.kt) querying subscribed channel uploads in parallel chunks using `ExtractorThrottler` (limit 4).
- [x] **T017** `[US2]`: Implement in-memory feed caching (TTL = 15 minutes) and reverse-chronological sorting.

---

## Phase 5: User Story 3 & 4 — Watch Later & Playback History

- [x] **T018** `[US3]`: Implement `WatchLaterRepository` in [app/.../core/data/repository/WatchLaterRepository.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/data/repository/WatchLaterRepository.kt).
- [x] **T019** `[US4]`: Implement `HistoryRepository` in [app/.../core/data/repository/HistoryRepository.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/data/repository/HistoryRepository.kt) with millisecond resume offset tracking.

---

## Phase 6: Dependency Injection & Application Wiring

- [x] **T020**: Define `repositoryModule` in [app/.../core/data/di/RepositoryModule.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/core/data/di/RepositoryModule.kt).
- [x] **T021**: Register `databaseModule` and `repositoryModule` in [PureTubeApp.kt](file:///d:/b/puretube/app/src/main/java/com/dr/tech/puretube/PureTubeApp.kt).

---

## Phase 7: Verification & Quality Gate

- [x] **T022**: Write unit tests for `SubscriptionDao`, `WatchLaterDao`, and `HistoryDao` using Room in-memory DB.
- [x] **T023**: Write unit tests for `ImportExportService` (NewPipe JSON & Takeout CSV).
- [x] **T024**: Execute `./gradlew test` and confirm 100% test pass rate.
- [x] **T025**: Execute `./gradlew assembleDebug` to verify compile-time KSP and Room validation with 0 errors.
