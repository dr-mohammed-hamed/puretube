# Android, Kotlin & Jetpack Compose Architectural Guardrails

This reference document outlines specific anti-patterns and failure modes unique to Android (Kotlin, Jetpack Compose, Room SQLite, Media3, and Coroutines).

---

## 1. Jetpack Compose & State Management Anti-Patterns

### Anti-Pattern 1.1: Direct State Mutation / Non-observable State
- **Defect**: Mutating regular Java/Kotlin collections (e.g. `list.add(item)`) or fields without `mutableStateOf` or `MutableStateFlow`.
- **Impact**: Jetpack Compose runtime will not trigger recomposition; UI appears stuck or unresponsive.

### Anti-Pattern 1.2: Hardcoded Colors & Design System Bypass
- **Defect**: Using raw `Color(0xFF...)` or static Material colors directly in UI composables instead of `PureTheme.colors.*`.
- **Impact**: Violates the single-source-of-truth theme architecture; prevents dynamic multi-theme switching and causes visual inconsistency.

### Anti-Pattern 1.3: Heavy Computation or Instantiation in Recomposition Scope
- **Defect**: Creating heavy objects, formatters, or parsing lists directly in the body of a `@Composable` function without wrapping in `remember`.
- **Impact**: Severe UI jank, frame drops (stuttering 120Hz/60Hz displays), and excessive memory allocation during animations or scroll.

### Anti-Pattern 1.4: Dangling or Mis-scoped Coroutines
- **Defect**: Launching `GlobalScope.launch` or launching coroutines inside composables without `LaunchedEffect` or `rememberCoroutineScope()`.
- **Impact**: Memory leaks, tasks continuing after screen disposal, and race conditions.

---

## 2. Room SQLite Database Anti-Patterns

### Anti-Pattern 2.1: Main Thread Query Execution
- **Defect**: Accessing Room DAOs or executing SQLite transactions on the Android main (UI) thread.
- **Impact**: `IllegalStateException: Cannot access database on the main thread since it may potentially lock the UI for a long period of time`.

### Anti-Pattern 2.2: Missing `@Transaction` on Multi-Table Operations
- **Defect**: Performing multiple related DAO writes (e.g. adding a subscription and inserting associated videos) across separate non-transactional methods.
- **Impact**: Partial failure leaves database in an inconsistent or corrupted state if an exception occurs mid-way.

### Anti-Pattern 2.3: Unparameterized Raw SQL Queries
- **Defect**: Executing raw SQLite queries using string interpolation (`"$query"`) instead of bound arguments (`?` or `:argument`).
- **Impact**: SQL Injection vulnerability and parsing syntax crashes on input with quotes/apostrophes.

---

## 3. Network, Extractor & Media3 Anti-Patterns

### Anti-Pattern 3.1: Unbounded Concurrent YouTube Extraction
- **Defect**: Launching parallel extraction requests for multiple channels or videos without concurrency throttling (`Semaphore(4)`).
- **Impact**: Rapid YouTube rate-limiting (HTTP 429), IP throttling, and network timeouts.

### Anti-Pattern 3.2: Swallowing Extraction & Stream Errors
- **Defect**: Catching `ExtractionException` without propagating a user-facing error state or retry mechanism.
- **Impact**: User sees a blank, frozen player without knowing why the video failed to load.

### Anti-Pattern 3.3: Missing ExoPlayer Release in Lifecycle
- **Defect**: Leaving `ExoPlayer` or `MediaController` instances unreleased when the player composable or service is disposed.
- **Impact**: Audio playback leaks, hardware video codec exhaustion, and high battery drain.
