# Research & Architectural Decisions: UI & Navigation (002-ui-and-navigation)

**Feature**: UI & Navigation Shell  
**Created**: 2026-09-15  
**Status**: Completed  

---

## 1. Architectural Decisions

### Decision 1: Navigation Pattern (State-Driven Tab Shell vs Navigation Compose)
- **Decision**: Adopt a state-driven sealed class / enum navigation model (`PureNavTab`: `Feed`, `Subscriptions`, `WatchLater`, `Settings`) hoisted in `MainNavigationShell` using Compose `AnimatedContent` / crossfade transitions.
- **Rationale**:
  1. PureTube V1 is strictly focused on an Android smartphone touch experience (Constitution Principle V).
  2. The primary layout consists of a standard 4-tab bottom navigation shell. Adding `navigation-compose` introduces external routing string boilerplate, deep-linking serialization overhead, and backstack fragility without tangible benefit for a 4-tab top-level shell.
  3. Preserves scroll states and list positions across tab switches seamlessly.
  4. Keeps the app modular, lightweight, and 100% predictable in Jetpack Compose.
- **Alternatives Considered**:
  - `androidx.navigation:navigation-compose`: Evaluated and rejected for V1 due to unnecessary route string ceremony and version coupling.
  - Multi-Activity: Rejected as anti-pattern for modern Jetpack Compose.

---

### Decision 2: Theme Persistence Strategy (ThemePreferences)
- **Decision**: Implement `ThemePreferences` in `core/designsystem/theme/` backed by Android `SharedPreferences` (or Room key-value), exposing a reactive `StateFlow<ThemePreset>`.
- **Rationale**:
  1. `MainActivity.kt` currently holds `rememberSaveable { mutableStateOf(ThemePreset.EMERALD_NIGHT) }`, which does not persist across app process deaths or fresh launches.
  2. `ThemePreferences` allows user customization from `SettingsScreen` to immediately persist and update the root `PureTheme` wrapper without recomposition cascades or cold restarts.
  3. Fully decoupled from UI composables (Constitution Principle V).
- **Alternatives Considered**:
  - Room DB settings table: Excessive boilerplate for a single enum preference.
  - Jetpack DataStore: Viable, but standard encrypted/normal `SharedPreferences` provides instantaneous zero-overhead synchronous bootstrap on `Application.onCreate()`.

---

### Decision 3: Compose Performance & Recomposition Safeguards
- **Decision**:
  1. Mark domain models (`FeedVideoItem`) as stable or ensure all fields are immutable primitives/strings.
  2. Use explicit `key = { it.videoId }` in `LazyColumn` items.
  3. Use Coil `AsyncImage` with `crossfade(true)` and disk/memory cache policies enabled to prevent image flickering during list scrolling or feed updates.
  4. Collect state using `collectAsStateWithLifecycle()` to automatically pause Flow collection when the Activity/Screen is stopped in the background.
- **Rationale**: Guarantees silky smooth 60fps scrolling and zero UI jitter (Constitution Principle V & Design System § 5).

---

### Decision 4: Zero-Emoji Enforcement & Cairo Typography
- **Decision**: Strictly utilize Material Icons Extended (e.g. `Icons.Default.RssFeed`, `Icons.Default.Subscriptions`, `Icons.Default.BookmarkBorder`, `Icons.Default.Settings`) and Cairo font hierarchy.
- **Rationale**: Fully compliant with Constitution Principle VII (Zero-Emoji Discipline) ensuring dignified visual presentation.
