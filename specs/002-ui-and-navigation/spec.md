# Feature Specification: UI & Navigation Shell (شريط التوجيه والواجهات الوقائية)

**Feature Branch / Directory**: `specs/002-ui-and-navigation`  
**Created**: 2026-09-15  
**Status**: Draft (Ready for Plan & Execution)  
**Target Milestone**: Phase 3 (الواجهات وشريط التبويبات - UI & Navigation)  

---

## 1. Executive Summary & Anti-Addiction Intent

In strict compliance with the **PureTube Constitution (Principles I, III, IV, V, VI, VII, VIII, IX)**, **SPECIFICATION.md**, and the **Vibrant Purity Design System (DESIGN_SYSTEM.md)**, Phase 3 establishes the permanent, production-ready user interface and navigation architecture for PureTube.

Phase 3 supersedes and completely replaces the temporary smoke-test harness (`TemporaryMilestone1HarnessScreen`) in `MainActivity.kt` with the permanent `MainNavigationShell`. It provides a mindful, protective digital environment:
- **No Algorithm / No Doomscrolling**: The Feed strictly aggregates content chronologically from channels the user has intentionally subscribed to.
- **Vibrant Purity Aesthetics**: 100% compliant with semantic tokens (`PureTheme.colors.*`) supporting instant multi-theme switching across 6 presets.
- **Zero-Emoji Dignity**: Exclusive usage of Material/SVG vector icons and Cairo typography across all system UI.
- **Modularity & Clean Architecture**: All screens remain strictly below 300 lines of code, hoisting state to dedicated Koin ViewModels (`StateFlow<UiState>`) with zero business logic in Composables.

---

## 2. User Scenarios & Prioritized User Stories

### User Story 1 - Permanent Main Navigation Shell (Priority: P1)
As a user opening PureTube, I want a permanent, thumb-friendly bottom navigation bar allowing me to transition seamlessly between my Feed (الرئيسية), Subscriptions (الاشتراكات), Watch Later (المشاهدة لاحقاً), and Settings (الإعدادات), with preserved scroll state and zero UI flickering.

**Why this priority**: The foundational shell of the app that replaces the temporary test harness and houses all primary user journeys.

**Independent Test**:
Can be verified by launching `MainActivity`. The bottom navigation bar renders 4 tabs with active indicators styled in `PureTheme.colors.primary` and subtle glow. Tapping any tab switches the content instantly without reloading or resetting state.

**Acceptance Scenarios**:
1. **Given** the app is launched, **When** the main view renders, **Then** `MainNavigationShell` displays the 4 bottom tabs with Arabic labels and vector icons, defaulting to the Feed tab.
2. **Given** the user is viewing the Subscriptions tab, **When** tapping the Watch Later tab, **Then** the view transitions smoothly with state preservation and the active tab indicator updates immediately.

---

### User Story 2 - Mindful Chronological Feed Screen (Priority: P1)
As a mindful user, I want to browse recent videos published strictly by my subscribed channels, presented as elegant video cards with thumbnails, duration badges, and channel metadata, without any algorithmic recommendations or unsolicited Shorts.

**Why this priority**: Core value proposition of PureTube — intentional, algorithm-free content consumption.

**Independent Test**:
Given existing subscriptions in Room DB, `FeedScreen` displays a `LazyColumn` of `PureVideoCard` items sorted by upload date descending. Pulling down triggers a refresh via `FeedViewModel`. If subscriptions are empty, a welcoming empty state prompts the user to activate the curated pack.

**Acceptance Scenarios**:
1. **Given** 10 subscribed channels, **When** opening the Feed tab, **Then** videos load with Coil cached thumbnails, duration badges in mm:ss (`durationMs`), title in `PureTheme.colors.textPrimary`, and channel name in `textSecondary`.
2. **Given** an ongoing network load, **When** data is fetching, **Then** a graceful shimmer loading placeholder is displayed.
3. **Given** total network failure and no local cache, **When** fetching fails, **Then** an error state with an explicit retry button is displayed without crashing.
4. **Given** a video card in the feed, **When** tapping the "Watch Later" quick action, **Then** the video is bookmarked into `WatchLaterEntity` and a feedback Snackbar confirms the action.

---

### User Story 3 - Subscriptions Management & Portability Hub (Priority: P2)
As a user managing my digital intake, I want to view my subscribed channels, add new channels via handle (`@channel`), URL, or video link, activate the pre-vetted Curated Starter Pack, and import/export my subscriptions using NewPipe JSON or Google Takeout CSV.

**Why this priority**: Gives users total control over their sources (Constitution Principle III) and eliminates the cold-start problem.

**Independent Test**:
Tapping "Activate Starter Pack" populates the list with vetted channels. Entering a valid `@handle` or YouTube link in the input bar and clicking "Add" resolves channel metadata via `SubscriptionRepository.fetchChannelDetails()` and inserts it into Room DB.

**Acceptance Scenarios**:
1. **Given** an empty subscriptions list, **When** tapping "Activate Curated Pack" (تفعيل الحزمة النموذجية المعتمدة), **Then** approved channels are seeded into Room DB and the list updates reactively.
2. **Given** the channel input bar, **When** the user pastes a YouTube channel or video link and taps Add, **Then** the channel is extracted with a loading spinner, saved, and added to the list.
3. **Given** a channel item in the list, **When** tapping "Unsubscribe", **Then** a confirmation dialog appears to prevent accidental deletions. Upon confirmation, the channel is removed from Room DB.
4. **Given** a NewPipe JSON or Google Takeout file content, **When** triggering import, **Then** channels are parsed, deduplicated, and inserted, displaying an import summary.

---

### User Story 4 - Mindful Watch Later Screen (Priority: P2)
As a user who wants to postpone viewing to an intentional time, I want to review my saved Watch Later videos in a dedicated, distraction-free screen, with options to remove watched videos or clear items.

**Why this priority**: Directly supports the anti-addiction ethos by separating discovery from consumption.

**Independent Test**:
Videos saved from the Feed appear immediately in `WatchLaterScreen` via reactive Flow. Removing an item removes it from `WatchLaterEntity` in SQLite.

**Acceptance Scenarios**:
1. **Given** 3 saved videos, **When** opening Watch Later, **Then** all 3 items render with thumbnail, title, channel, and duration.
2. **Given** a saved item, **When** tapping the remove/delete icon, **Then** the item is removed from Room DB with immediate UI removal.
3. **Given** zero saved videos, **When** viewing the screen, **Then** an empty state displays an encouraging anti-addiction message ("احفظ المقاطع النافعة لتشاهدها بوعي").

---

### User Story 5 - Settings & Centralized Theme Selector (Priority: P3)
As a user, I want to customize my visual theme from the 6 approved presets (Radiant Emerald, Royal Indigo, Nordic Sage, Warm Espresso, OLED Pure Black, Sapphire Ocean), persist my choice across app restarts, and inspect local database statistics.

**Why this priority**: Fulfills the requirement in `MainActivity.kt` to move `ThemeSelectorSection` to the permanent `SettingsScreen` and ensures persistent user experience.

**Independent Test**:
Selecting "OLED Pure Black" in Settings immediately updates `PureTheme` across all screens, and remains active when reopening the app.

**Acceptance Scenarios**:
1. **Given** the Settings screen, **When** a user taps a theme preset card, **Then** the theme changes instantly and the selection is saved to `ThemePreferences`.
2. **Given** the Settings screen, **When** inspecting stats, **Then** the total count of subscribed channels and saved videos is displayed accurately from Room DAOs.
3. **Given** the "About" card, **When** viewed, **Then** the version number and PureTube mission statement are presented with dignified Cairo typography.

---

## 3. Edge Cases & Failure Modes

1. **Slow Network / Extractor Delay on Feed Fetch**:
   - **Behavior**: While `FeedRepository.getFeed()` is fetching, `FeedViewModel` sets `isLoading = true`. The UI displays shimmer placeholders. If cached data is available, cached data is shown with a non-intrusive background sync indicator.
2. **Double-Tap on Add Subscription or Bookmark**:
   - **Behavior**: All action buttons are bound to `isActionInProgress` in ViewModel. Additional taps are dropped until the first async operation finishes.
3. **App Killed / Restarted During Theme Switch**:
   - **Behavior**: `ThemePreferences` writes synchronously or via DataStore/SharedPreferences commit so the preset is never corrupted.
4. **Empty Subscriptions on First Launch**:
   - **Behavior**: Automatic seeding of `CuratedStarterPack` runs, but if it takes a moment, `FeedScreen` displays a dedicated "Welcome to PureTube" card allowing manual one-tap pack activation.

---

## 4. Requirements & Specifications

### Functional Requirements
- **FR-001**: Replace `TemporaryMilestone1HarnessScreen` in `MainActivity.kt` with `MainNavigationShell`.
- **FR-002**: Implement `PureBottomNavBar` with 4 tabs: Feed, Subscriptions, Watch Later, and Settings.
- **FR-003**: Implement `FeedScreen` and `FeedViewModel` with pull-to-refresh and reactive `StateFlow<FeedUiState>`.
- **FR-004**: Implement `PureVideoCard` component with Coil image loading, mm:ss duration badge, and Watch Later bookmark action.
- **FR-005**: Implement `SubscriptionsScreen` and `SubscriptionsViewModel` supporting channel lookup by URL/@handle, Curated Starter Pack activation, and unsubscription dialog.
- **FR-006**: Implement `ImportExportBottomSheet` for NewPipe JSON and Google Takeout CSV importing and exporting.
- **FR-007**: Implement `WatchLaterScreen` and `WatchLaterViewModel` observing `WatchLaterRepository.getWatchLaterFlow()`.
- **FR-008**: Implement `SettingsScreen` and `SettingsViewModel` containing the permanent `ThemeSelectorSection` and DB stats.
- **FR-009**: Implement `ThemePreferences` in `core/designsystem` backed by Android SharedPreferences to persist the active `ThemePreset`.
- **FR-010**: All UI components MUST exclusively reference `PureTheme.colors.*` (zero hardcoded hex codes).
- **FR-011**: All handwritten Composable files MUST remain under 300 LOC (Constitution Principle VI).
- **FR-012**: System UI MUST strictly prohibit Unicode emojis, using Material/SVG vector icons exclusively (Constitution Principle VII).
- **FR-013**: ViewModels and UI state MUST be registered in Koin (`featuresModule` or `uiModule`).

---

## 5. Success Criteria & Quality Metrics

- **SC-001**: 100% of UI screens compile cleanly with zero errors via `./gradlew assembleDebug`.
- **SC-002**: All unit tests for ViewModels, repositories, and theme persistence pass 100% via `./gradlew test`.
- **SC-003**: Switching between tabs takes under 50ms with zero recomposition jank (60fps target).
- **SC-004**: Zero raw hex color values anywhere in UI feature composables (`grep_search` verification).
- **SC-005**: Zero Unicode emojis in system authored UI.
- **SC-006**: Every Composable file is strictly below 300 LOC.
