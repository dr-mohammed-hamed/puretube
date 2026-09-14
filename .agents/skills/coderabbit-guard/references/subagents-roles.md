# Specialized Subagent Roles & Prompts (Android & Kotlin)

`coderabbit-guard` uses 3 specialized reviewer roles to audit code diffs with maximum depth and zero blind spots.

---

## Subagent A: Security, Exceptions & Extractor Safety

**Core Mission**: Find silent failure points, unhandled boundary cases, network abuse, and security/data leak risks.

**Strict Audit Checklist**:
1. **Error Swallowing**:
   - Flag any `catch (e: Exception) {}` that suppresses exceptions without logging, reporting, or user feedback.
   - Flag returning dummy `null` or empty lists inside a catch block when the caller expects an explicit error or valid payload.
2. **Null & Collection Safety**:
   - Check if collection access (`list.first()`, `list[0]`) assumes non-empty state without checking `.isEmpty()` or using `.firstOrNull()`.
   - Check force-unwrap operators (`!!`) on nullable variables. Verify if null safety is bypassed unsafely.
3. **Network & Extractor Politeness**:
   - Ensure all `NewPipeExtractor` calls are guarded by Coroutine `Semaphore(4)` to prevent YouTube IP throttling.
   - Verify that extraction calls run strictly on `Dispatchers.IO`.
4. **Data Leaks & PII**:
   - Ensure sensitive logs, tokens, or PII are not hardcoded or printed via `println()` or `Log.d()` in production release paths.
5. **SQL Injection & Parameterized Bindings**:
   - Flag any Room query using raw string interpolation (`"$id"`) instead of parameterized bindings (`:id`).

---

## Subagent B: Concurrency, Room DB & Compose Lifecycle

**Core Mission**: Detect race conditions, UI freezing, Room thread collisions, and recomposition bloat.

**Strict Audit Checklist**:
1. **Thread Safety & Dispatchers**:
   - Flag any Room DAO query or transaction called on the Android Main thread.
   - Verify `Dispatchers.IO` is used for database and network operations.
2. **Compose Lifecycle & Coroutine Scoping**:
   - Flag launching coroutines inside composables without `LaunchedEffect` or `rememberCoroutineScope()`.
   - Flag missing `remember` wrappers around expensive calculations or object creations.
3. **Database Transactions & Atomicity**:
   - Verify multi-table writes in Room use `@Transaction`.
4. **Preventing Double-Click Race Conditions**:
   - Check if action buttons (e.g. Subscribe, Download, Add to Watch Later) disable interaction or guard against rapid multiple taps while async operations are running.
5. **Mandatory Red-Teaming Audit (3 Failure Scenarios)**:
   - *Scenario 1 (Race Conditions / Rapid Inputs)*: Audit rapid overlapping taps or out-of-order extraction responses.
   - *Scenario 2 (Network / Stream Timeout)*: Evaluate handling when YouTube stream links expire or network drops mid-stream.
   - *Scenario 3 (Database vs Memory Desync)*: Check if memory state updates reflect local Room database changes.

---

## Subagent C: Contracts, Design System & Arabic RTL Parity

**Core Mission**: Enforce interface contract integrity, strict `PureTheme` design token compliance, and RTL layout parity.

**Strict Audit Checklist**:
1. **Strict Design System Compliance (Zero Hardcoded Colors)**:
   - **MANDATORY**: Flag ANY hardcoded hex color (e.g. `Color(0xFF...)`) or raw Material color inside UI composables.
   - Verify that all visual elements use `PureTheme.colors.*` so that dynamic theme switching works without friction.
2. **Call Site & Signature Matching**:
   - For every modified method or repository interface, verify that ALL callers across the codebase (UI composables, ViewModels, unit tests) match the new signature.
3. **Arabic & RTL Parity**:
   - Verify layout direction support and proper typography scaling (`PureTypography`).
   - Catch missing start/end padding (using `PaddingValues` or `Modifier.padding(start/end)` rather than hardcoded left/right).
