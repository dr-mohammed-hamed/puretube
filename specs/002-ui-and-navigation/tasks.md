# Tasks: UI & Navigation Shell (002-ui-and-navigation)

**Input**: Design documents from `specs/002-ui-and-navigation/`  
**Prerequisites**: [spec.md](file:///d:/b/puretube/specs/002-ui-and-navigation/spec.md), [plan.md](file:///d:/b/puretube/specs/002-ui-and-navigation/plan.md), [data-model.md](file:///d:/b/puretube/specs/002-ui-and-navigation/data-model.md)  
**Status**: Completed (100% Verified)  

---

## Format: `- [x] [TaskID] [P?] [Story?] Description with file path`
- **[P]**: Parallelizable task (different files, no blocking dependencies)
- **[Story]**: Related User Story:
  - `[US1]`: Main Navigation Shell & Tab Switching
  - `[US2]`: Mindful Chronological Feed Screen
  - `[US3]`: Subscriptions Management & Portability
  - `[US4]`: Mindful Watch Later Screen
  - `[US5]`: Settings & Theme Selector

---

## Phase 1: Setup & Foundational Infrastructure

- [x] T001 Implement `ThemePreferences` helper for persistent theme storage in `app/src/main/java/com/dr/tech/puretube/core/designsystem/theme/ThemePreferences.kt`
- [x] T002 `[P]` Define `featuresModule` registering all upcoming ViewModels in `app/src/main/java/com/dr/tech/puretube/features/di/FeaturesModule.kt`
- [x] T003 Update `PureTubeApp.kt` to register `featuresModule` in `app/src/main/java/com/dr/tech/puretube/PureTubeApp.kt`

---

## Phase 2: Core Shared UI Components (Vibrant Purity Tokens)

- [x] T004 `[P]` Implement `PureVideoCard` with Coil image loading, duration badge, and Watch Later action in `app/src/main/java/com/dr/tech/puretube/core/components/PureVideoCard.kt`
- [x] T005 `[P]` Implement `PureEmptyState` with anti-addiction messaging and call-to-action in `app/src/main/java/com/dr/tech/puretube/core/components/PureEmptyState.kt`
- [x] T006 `[P]` Implement `PureLoadingShimmer` placeholder for smooth content loading in `app/src/main/java/com/dr/tech/puretube/core/components/PureLoadingShimmer.kt`
- [x] T007 `[P]` Implement `PureErrorState` with retry callback and friendly error messages in `app/src/main/java/com/dr/tech/puretube/core/components/PureErrorState.kt`
- [x] T008 `[P]` Implement `PureBottomNavBar` with 4 tabs and active glow in `app/src/main/java/com/dr/tech/puretube/core/components/PureBottomNavBar.kt`

---

## Phase 3: User Story 1 — Permanent Main Navigation Shell

- [x] T009 `[US1]` Define `PureNavTab` enum with tab metadata and Material vector icons in `app/src/main/java/com/dr/tech/puretube/features/navigation/PureNavTab.kt`
- [x] T010 `[US1]` Implement `MainNavigationShell` managing tab state and crossfade transitions in `app/src/main/java/com/dr/tech/puretube/features/navigation/MainNavigationShell.kt`
- [x] T011 `[US1]` Update `MainActivity.kt` to host `MainNavigationShell` with reactive theme observation in `app/src/main/java/com/dr/tech/puretube/MainActivity.kt`

---

## Phase 4: User Story 2 — Mindful Chronological Feed Screen

- [x] T012 `[US2]` Implement `FeedViewModel` handling feed aggregation, caching, and refresh in `app/src/main/java/com/dr/tech/puretube/features/feed/FeedViewModel.kt`
- [x] T013 `[P]` `[US2]` Implement `FeedStarterBanner` for new users without subscriptions in `app/src/main/java/com/dr/tech/puretube/features/feed/components/FeedStarterBanner.kt`
- [x] T014 `[US2]` Implement `FeedScreen` with `LazyColumn`, pull-to-refresh, and key stability in `app/src/main/java/com/dr/tech/puretube/features/feed/FeedScreen.kt`

---

## Phase 5: User Story 3 — Subscriptions Management & Portability Hub

- [x] T015 `[US3]` Implement `SubscriptionsViewModel` managing channel lookup, pack activation, and import/export in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/SubscriptionsViewModel.kt`
- [x] T016 `[P]` `[US3]` Implement `ChannelListItem` displaying channel avatar, title, and unsubscribe button in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/components/ChannelListItem.kt`
- [x] T017 `[P]` `[US3]` Implement `ChannelSearchBar` for URL/@handle entry in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/components/ChannelSearchBar.kt`
- [x] T018 `[P]` `[US3]` Implement `UnsubscribeConfirmDialog` to prevent accidental unfollows in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/sheets/UnsubscribeConfirmDialog.kt`
- [x] T019 `[P]` `[US3]` Implement `ImportExportSheet` for NewPipe JSON and Takeout CSV handling in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/sheets/ImportExportSheet.kt`
- [x] T020 `[US3]` Implement `SubscriptionsScreen` assembling the management hub in `app/src/main/java/com/dr/tech/puretube/features/subscriptions/SubscriptionsScreen.kt`

---

## Phase 6: User Story 4 — Mindful Watch Later Screen

- [x] T021 `[US4]` Implement `WatchLaterViewModel` managing saved bookmarks in `app/src/main/java/com/dr/tech/puretube/features/watchlater/WatchLaterViewModel.kt`
- [x] T022 `[P]` `[US4]` Implement `WatchLaterItemCard` with thumbnail, duration, and delete action in `app/src/main/java/com/dr/tech/puretube/features/watchlater/components/WatchLaterItemCard.kt`
- [x] T023 `[US4]` Implement `WatchLaterScreen` displaying saved videos and empty state in `app/src/main/java/com/dr/tech/puretube/features/watchlater/WatchLaterScreen.kt`

---

## Phase 7: User Story 5 — Settings & Theme Selector

- [x] T024 `[US5]` Implement `SettingsViewModel` managing theme persistence and DB stats in `app/src/main/java/com/dr/tech/puretube/features/settings/SettingsViewModel.kt`
- [x] T025 `[P]` `[US5]` Implement `ThemeSelectorGrid` displaying the 6 visual theme presets in `app/src/main/java/com/dr/tech/puretube/features/settings/components/ThemeSelectorGrid.kt`
- [x] T026 `[P]` `[US5]` Implement `DatabaseStatsCard` displaying channel and saved item counts in `app/src/main/java/com/dr/tech/puretube/features/settings/components/DatabaseStatsCard.kt`
- [x] T027 `[US5]` Implement `SettingsScreen` assembling theme selection, stats, and About PureTube in `app/src/main/java/com/dr/tech/puretube/features/settings/SettingsScreen.kt`

---

## Phase 8: Polish, Automated Verification & Quality Gates

- [x] T028 `[P]` Write unit tests for `FeedViewModel`, `SubscriptionsViewModel`, and `WatchLaterViewModel` in `app/src/test/java/com/dr/tech/puretube/features/`
- [x] T029 `[P]` Write unit test for `ThemePreferences` verifying persistence in `app/src/test/java/com/dr/tech/puretube/core/designsystem/ThemePreferencesTest.kt`
- [x] T030 Execute `./gradlew test` to confirm 100% test pass rate across all layers (39 tests passing)
- [x] T031 Execute `./gradlew assembleDebug` to verify complete compile-time build with 0 errors
- [x] T032 Verify Zero-Emoji compliance and 100% `PureTheme.colors.*` semantic token usage across all UI files
