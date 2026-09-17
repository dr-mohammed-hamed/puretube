<!--
Sync Impact Report:
- Version change: 1.2.0 -> 1.3.0
- Scope Focus: Android Smartphone V1 Lean Edition (Multi-platform/Desktop & Keyboard shortcuts explicitly deferred)
- Modified principles:
  - IV. Resilience & Network Politeness (Expanded: Mandatory NIO Desugaring Invariant (`desugar_jdk_libs_nio`) to guarantee ABI compatibility for `java.net.URLEncoder/URLDecoder` and `java.nio` across Android 7.0–12L / API 24–32)
  - VII. Zero-Emoji Discipline & Presentation Error Sanitization (Expanded: Strict ban on leaking raw JVM `Throwable.localizedMessage` or technical stack traces to Compose UI; mandatory Arabic semantic error mapping with technical traces restricted to Logcat)
- Templates requiring updates:
  - .specify/templates/plan-template.md (Updated Gate 4 with NIO desugaring invariant and Gate 6 with Presentation Error Sanitization)
- Follow-up TODOs: None
-->

# PureTube (نقِيّ) Constitution

## Core Principles

### I. Anti-Addiction & Mindful Intent (NON-NEGOTIABLE)
PureTube is an anti-addiction, protective environment. All features must serve intentional, mindful usage:
- No automated recommendation algorithms, "Trending" sections, or cookie-based rabbit holes.
- Zero infinite doomscrolling or unrequested Shorts feeds.
- Content appears strictly from channels the user has intentionally chosen to follow or saved to Watch Later.

### II. AI Safety & Hook Readiness (Zero-Copy AI Bridge)
The media player must maintain modular hooks ready for on-device AI moderation (`halalify-ai-core`):
- `VideoFrameHook` for real-time visual moderation/blurring.
- `AudioFilterHook` for real-time audio/music isolation.
- Zero CPU/memory overhead when filters are disabled (NoOp by default in V1).

### III. Privacy & Offline-First Data Ownership
All user data remains strictly on the user's device:
- Zero external tracking, zero intermediaries, zero telemetry.
- All subscriptions, history, and watch lists are stored locally in Room DB.
- Full data portability: Import/Export support for standard NewPipe and Google Takeout formats.

### IV. Resilience & Network Politeness
Interaction with YouTube streams and data must be robust and considerate:
- Concurrency limiting via coroutine semaphores (`limit = 4`) to prevent IP throttling and memory bloat.
- Graceful degradation on YouTube extraction changes with user-friendly explanations.
- Automatic merging of separate DASH video and audio streams via Media3.
- Media timestamps, durations, and seek offsets MUST be stored and computed as `Long` milliseconds throughout domain and database layers to prevent fractional drift.
- **Universal Throwable Catching**: All extraction wrappers (`safeExtract`) and third-party reflection-heavy bridges MUST catch `Throwable` (re-throwing `CancellationException`) instead of only `Exception`. This guarantees that bytecode linkage failures (`NoSuchMethodError`, `NoClassDefFoundError`, `LinkageError`) are caught gracefully and never kill the Android OS process.
- **Mandatory NIO Desugaring Invariant**: The build configuration MUST exclusively use `com.android.tools:desugar_jdk_libs_nio` (never standard or minimal `desugar_jdk_libs`). This guarantees that D8/L8 retargets modern Java 11/NIO and network signatures (`URLEncoder.encode(String, Charset)` and `URLDecoder.decode(String, Charset)`) to bundled `j$.net` implementations, preventing `NoSuchMethodError` on Android 7.0–12L (API 24–32) devices.

### V. Feature-First Modern Android Architecture (V1 Smartphone Focused)
- V1 is strictly focused on Android smartphone touch experience. Large-screen/desktop parity and external hardware shortcut listeners are deferred to post-V1 milestones.
- Clean modular structure: `core` (database, designsystem, extractor, aibridge), `player`, and `features` (feed, subscriptions, watchlater, history, settings).
- Jetpack Compose with Vibrant Purity palette (Radiant Emerald, Obsidian Charcoal, Warm Amber) featuring centralized token architecture (Single Source of Truth: `PureTheme.colors.*`) for zero-friction color edits and dynamic multi-theme support, plus RTL-first Arabic layout.
- Zero business logic inside UI composables.
- **Unified Screen Header Contract**: All feature screens MUST use consistent, high-performance custom header composables (e.g. `Row` with `statusBarsPadding()`, `IconButton`, and `Text` styled via `PureTheme` tokens). Spurious usage of isolated Material 3 `TopAppBar` or components with conflicting WindowInsets is STRICTLY FORBIDDEN to prevent layout, clipping, and ABI crashes under edge-to-edge display (`enableEdgeToEdge()`).

### VI. File Length Ceiling & Composable Modular Blueprint
- **Hard File Length Ceiling**: No handwritten Kotlin file (Composables, ViewModels, Repositories, DAOs) SHALL exceed **1,000 lines** under any circumstances. Generated files are explicitly exempted.
- **Recommended Architectural Target**: Individual Composable UI files SHOULD remain below **300 lines**.
- **Flexible Composable Structure**:
  1. **Feature Screen (`<Feature>Screen.kt`)**: Root composable handling screen scaffolding, state collection via `collectAsStateWithLifecycle()`, and event hoisting (< 300 lines).
  2. **Feature ViewModel (`<Feature>ViewModel.kt`)**: Dedicated `ViewModel` encapsulating `StateFlow<UiState>`, repository calls, and user intent handling (< 250 lines). Zero Compose UI code or Android View Context references.
  3. **Section Extraction (`components/<Feature><Section>.kt`)**: Sub-sections and list items SHOULD be extracted into a dedicated `components/` subfolder whenever the screen file approaches or exceeds 300 lines.
  4. **Modals & Bottom Sheets (`sheets/<Feature><Action>Sheet.kt`)**: Standalone bottom sheet or dialog composables (< 200 lines).
- **Anti-God-Composable Rule**: Developers and AI subagents ARE FORBIDDEN from piling sprawling private `@Composable private fun _buildXxx()` helper methods inside monolithic screen files. Exceeding line thresholds requires immediate modular decomposition.

### VII. Zero-Emoji Discipline & Presentation Error Sanitization in System UI
The usage of Unicode emojis (emoticons / pictographs) in developer-authored user interfaces, buttons, titles, dialogues, snackbars, and notification messages IS STRICTLY FORBIDDEN. Standard vector icons (Material Icons / SVG) and clear Arabic typography (Cairo font) MUST be used exclusively to preserve high visual contrast, professional dignity, and cross-device rendering consistency. (Note: Unmodified external user content and original video titles fetched from YouTube streams are exempt).
- **Presentation Error Sanitization**: Developers and AI agents ARE STRICTLY FORBIDDEN from leaking raw JVM `Throwable.localizedMessage` or technical stack traces (e.g. `NoSuchMethodError`, `ClassNotFoundException`, `SocketTimeoutException`, `LinkageError`, or Java package paths) into user-facing Compose UI. All ViewModels MUST map failures through dedicated sanitization functions into dignified, respectful Arabic explanations (Cairo font). Raw technical errors and stack traces MUST be routed exclusively to Android `Logcat` for developer diagnostics.

### VIII. Single Source of Truth (SSOT) for Entity State & Stream Separation
- **Authoritative Database State**: Channel subscriptions, Watch Later items, playback history, and resume positions MUST have exactly ONE authoritative DAO in Room DB. Secondary screens or widgets SHALL NOT query or calculate bookmark status via redundant, parallel, or private queries.
- **Ephemeral Stream Separation**: Ephemeral YouTube streaming URLs and signed DASH formats MUST be managed exclusively in-memory by `PurePlayerManager` with automatic re-extraction upon HTTP 403 expiration. Expiring streaming tokens SHALL NOT be stored in Room DB as persistent entity state.

### IX. Coroutine Lifecycle Safety & Defensive Null-Safety
- Asynchronous operations MUST be explicitly bound to `viewModelScope` or `rememberCoroutineScope()`. Dangling un-scoped coroutines are strictly prohibited.
- **Mandatory Coroutine Circuit Breakers**: All feature ViewModels MUST install a root `CoroutineExceptionHandler` on asynchronous extraction and mutation jobs (`launch(handler)`). Uncaught runtime or bytecode errors MUST be routed gracefully to `PureErrorState` UI rather than letting the coroutine tree propagate exceptions up to the default thread handler, guaranteeing zero process-level crashes.
- Double-click race conditions on buttons during async calls MUST be prevented via UI debounce or busy state flags.
- Forced null unwrap operators (`!!`) are forbidden; Kotlin defensive idioms (`?.`, `?:`, `takeIf`) MUST be used.
- All network and extraction calls MUST be wrapped in `Result<T>` with user-friendly recovery states.

### X. Seed Data & Curated Pack Integrity
Bundled starter content, curated channels, and initial entity states MUST be verified against live YouTube endpoints prior to shipping:
- Fictitious, placeholder, or synthesized channel IDs and URLs are strictly forbidden in production code.
- Curated channels and starter packs MUST reference valid, public YouTube channel IDs or canonical handles (`@handle`).
- Any seed loader MUST handle extraction failures defensively without breaking the onboarding experience.

## Technical Stack & Platform Constraints (V1)
- **Platform**: Android Mobile (minSdk 24, targetSdk 36).
- **Java Compatibility**: Core library desugaring via `com.android.tools:desugar_jdk_libs_nio` (Java 11/NIO and network parity across API 24–32).
- **UI Toolkit**: Jetpack Compose (Material 3 semantic tokens via `PureTheme.colors`).
- **Media Engine**: AndroidX Media3 ExoPlayer with Picture-in-Picture (PiP) and background audio service.
- **Data & Extraction**: Room SQLite database with Coroutine Flow reactive queries, NewPipeExtractor with `Semaphore(4)` throttling.

## Governance
This Constitution defines the architectural boundaries and product ethos for PureTube. 
- **Amendment Process**: Any amendments require updating this document, incrementing the semantic version, and updating dependent templates (`.specify/templates/*`).
- **Versioning Policy**: Semantic versioning (MAJOR.MINOR.PATCH) is enforced for governance changes:
  - **MAJOR**: Removals or redefinitions of core principles breaking existing workflows.
  - **MINOR**: Addition of new principles, tech stack updates, or expanded constraints.
  - **PATCH**: Non-semantic clarifications, formatting, or typo fixes.
- **Compliance Enforcement**: Automated tools, subagents, and human developers MUST audit code against these non-negotiable principles.

**Version**: 1.3.0 | **Ratified**: 2026-09-14 | **Last Amended**: 2026-09-17
