# Implementation Plan: UI & Navigation Shell (002-ui-and-navigation)

**Branch**: `002-ui-and-navigation` | **Date**: 2026-09-15 | **Spec**: [spec.md](file:///d:/b/puretube/specs/002-ui-and-navigation/spec.md)

**Input**: Feature specification from `specs/002-ui-and-navigation/spec.md`

---

## 1. Summary

Phase 3 transitions PureTube from the Phase 1 & 2 infrastructure and core data engine into a complete, interactive, and production-ready Android user interface. It completely replaces the temporary smoke-test harness in `MainActivity.kt` with `MainNavigationShell`, implementing:
1. A permanent bottom navigation shell with 4 top-level destinations (Feed, Subscriptions, Watch Later, Settings).
2. The anti-addiction Feed screen with reverse-chronological updates from intentionally subscribed channels, Coil image loading, and mm:ss duration formatting.
3. The Subscriptions hub with instant channel adding via URL/@handle, Curated Starter Pack activation, and NewPipe/Takeout data portability.
4. The mindful Watch Later screen for intentional postponed consumption.
5. The permanent Settings screen with multi-theme selection across 6 presets and persistent storage in `ThemePreferences`.

---

## 2. Technical Context

- **Language/Version**: Kotlin 2.0.21, Java 17, minSdk 24, targetSdk 36.
- **UI Toolkit**: Jetpack Compose with Material 3 and Compose BOM 2024.10.01.
- **Dependency Injection**: Koin 3.5.6 (`koin-androidx-compose`).
- **Image Loading**: Coil 2.7.0 (`coil-compose`).
- **Data & Repositories**: Room 2.7.0 SQLite (`PureTubeDatabase`), `FeedRepository`, `SubscriptionRepository`, `WatchLaterRepository`, `HistoryRepository`.
- **State Management**: AndroidX `ViewModel`, Kotlin Coroutines `StateFlow`, `collectAsStateWithLifecycle()`.
- **Target Platform**: Android Smartphone touch experience (mobile-first, RTL Arabic typography).
- **Performance Goals**: 60fps scrolling in `LazyColumn`, <50ms tab transition, zero frame drops.
- **Constraints**: Hard ceiling of 1,000 LOC per file, <300 LOC per Composable, 100% semantic colors via `PureTheme.colors.*`, zero Unicode emojis in system UI.

---

## 3. Constitution Check

*All 7 Gates evaluated and verified for Phase 3:*

- [x] **Gate 1: Anti-Addiction & Intentionality**: PureTube Feed contains ZERO automated recommendation algorithms, zero trending sections, and zero infinite doomscrolling Shorts. All videos originate exclusively from user-subscribed channels.
- [x] **Gate 2: AI Safety Hooks**: Video player hooks (`VideoFrameHook`, `AudioFilterHook`) remain intact and modular, ready for Phase 4 player integration.
- [x] **Gate 3: Privacy & Offline-First SSOT**: Room DB is the sole authoritative SSOT for subscriptions and watch later bookmarks. No user data leaves the device.
- [x] **Gate 4: Network Resilience & Temporal Precision**: Feed extraction utilizes `ExtractorThrottler` with `Semaphore(4)`. Durations and seek timestamps are strictly `Long` milliseconds.
- [x] **Gate 5: File Length & 4-Tier Blueprint**: Hard 1,000 LOC ceiling respected; all screens remain under 300 LOC with subcomponents extracted into `components/` and sheets into `sheets/`.
- [x] **Gate 6: Zero-Emoji & Theme Compliance**: 100% compliant with `PureTheme.colors.*` semantic tokens. Emojis are strictly banned from system UI, replaced by Material vector icons and Cairo typography.
- [x] **Gate 7: Coroutines & Null-Safety Discipline**: All asynchronous calls are bound to `viewModelScope`. Double-tap prevention via busy state flags. Zero `!!` double-bang operators used. All failures wrapped in `Result<T>`.

---

## 4. Project Structure & Blueprint

```text
app/src/main/java/com/dr/tech/puretube/
├── MainActivity.kt                                   # Main entry point hosting MainNavigationShell
├── PureTubeApp.kt                                    # Application class registering Koin modules
│
├── core/
│   ├── designsystem/
│   │   └── theme/
│   │       ├── Color.kt                              # Palette primitives
│   │       ├── PureColors.kt                         # Semantic color tokens (PureTheme.colors.*)
│   │       ├── PureTheme.kt                          # CompositionLocal theme wrapper
│   │       ├── ThemePreset.kt                        # The 6 approved color presets
│   │       ├── ThemePreferences.kt                   # [NEW] Persistent theme preference helper
│   │       └── Type.kt                               # Arabic Cairo typography scale
│   │
│   └── components/
│       ├── PureBottomNavBar.kt                       # [NEW] 4-tab thumb-friendly bottom bar
│       ├── PureVideoCard.kt                          # [NEW] Mindful video item card with Coil
│       ├── PureEmptyState.kt                         # [NEW] Dignified empty state card
│       ├── PureLoadingShimmer.kt                     # [NEW] Shimmer loading placeholder
│       └── PureErrorState.kt                         # [NEW] Graceful error recovery with retry
│
└── features/
    ├── di/
    │   └── FeaturesModule.kt                         # [NEW] Koin module injecting all ViewModels
    │
    ├── navigation/
    │   ├── MainNavigationShell.kt                    # [NEW] Root shell with AnimatedContent
    │   └── PureNavTab.kt                             # [NEW] Navigation tab enum & icons
    │
    ├── feed/
    │   ├── FeedScreen.kt                             # [NEW] Screen composable (< 300 LOC)
    │   ├── FeedViewModel.kt                          # [NEW] StateFlow<FeedUiState> (< 250 LOC)
    │   └── components/
    │       └── FeedStarterBanner.kt                  # [NEW] Welcome banner for fresh installs
    │
    ├── subscriptions/
    │   ├── SubscriptionsScreen.kt                    # [NEW] Screen composable (< 300 LOC)
    │   ├── SubscriptionsViewModel.kt                 # [NEW] Channel addition & portability
    │   ├── components/
    │   │   ├── ChannelListItem.kt                    # [NEW] Single channel row with avatar
    │   │   └── ChannelSearchBar.kt                   # [NEW] Flexible URL/@handle input bar
    │   └── sheets/
    │       ├── ImportExportSheet.kt                  # [NEW] Bottom sheet for NewPipe/Takeout
    │       └── UnsubscribeConfirmDialog.kt           # [NEW] Confirmation dialog
    │
    ├── watchlater/
    │   ├── WatchLaterScreen.kt                       # [NEW] Screen composable (< 300 LOC)
    │   ├── WatchLaterViewModel.kt                    # [NEW] WatchLater state & deletion
    │   └── components/
    │       └── WatchLaterItemCard.kt                 # [NEW] Saved item row
    │
    └── settings/
        ├── SettingsScreen.kt                         # [NEW] Permanent settings screen
        ├── SettingsViewModel.kt                      # [NEW] Theme switching & DB stats
        └── components/
            ├── ThemeSelectorGrid.kt                  # [NEW] Extracted theme selector cards
            └── DatabaseStatsCard.kt                  # [NEW] Subscriptions & saved count
```

---

## 5. Phase 0: Research & Key Technical Decisions
*Detailed findings in [research.md](file:///d:/b/puretube/specs/002-ui-and-navigation/research.md)*:
1. **Navigation**: State-driven tab shell (`PureNavTab` + `AnimatedContent`) chosen over Navigation Compose to ensure zero boilerplate, rock-solid backstack stability, and smooth state retention across tabs.
2. **Theme Persistence**: `ThemePreferences` backed by Android `SharedPreferences` persists `ThemePreset` and emits reactive `StateFlow<ThemePreset>`.
3. **Performance**: Coil memory caching, immutable state objects, and explicit item keys (`videoId`) prevent recomposition storms.

---

## 6. Phase 1: Data Model & Contract Definitions
*Full details in [data-model.md](file:///d:/b/puretube/specs/002-ui-and-navigation/data-model.md) and [contracts/](file:///d:/b/puretube/specs/002-ui-and-navigation/contracts/navigation-contract.md)*:
- Unified UiState models: `FeedUiState`, `SubscriptionsUiState`, `WatchLaterUiState`, `SettingsUiState`.
- Formal contracts between Composables and ViewModels via event hoisting and lambda callbacks.

---

## 7. Verification Plan & Test Strategy
- Unit tests for all ViewModels using `TestCoroutineDispatcher` and mocked repositories.
- Unit test for `ThemePreferences` verifying persistence and default fallback.
- Compile-time verification via `./gradlew assembleDebug`.
- Automated regression check via `./gradlew test`.
- Hardcoded hex scan via `git grep` to enforce single source of truth (`PureTheme.colors`).
