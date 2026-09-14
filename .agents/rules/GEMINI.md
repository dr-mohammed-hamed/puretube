# Workspace Rules & Quality Standards — PureTube (نَقِيّ)

Strict guidelines, architecture contracts, and quality gates for developer agents in this workspace.

---

## 1. Constitution & Architectural Foundation
* **Mandatory Session Boot Hook (إقلاع الجلسة الإلزامي)**: Before proposing any architectural plan, answering queries, or writing code in a new session, the agent **MUST** immediately inspect [.specify/memory/constitution.md](file:///d:/b/puretube/.specify/memory/constitution.md), [SPECIFICATION.md](file:///d:/b/puretube/SPECIFICATION.md), and [DESIGN_SYSTEM.md](file:///d:/b/puretube/DESIGN_SYSTEM.md) to ground itself in the Project Manager's decisions, anti-addiction ethos, and single-source-of-truth theme architecture.
* **Constitution Supreme**: Read and strictly follow [.specify/memory/constitution.md](file:///d:/b/puretube/.specify/memory/constitution.md) before research, design, or implementation.
* **Feature-First Architecture**:
  - `core/`: database (Room SQLite), designsystem (Centralized Theme & Tokens), extractor (NewPipeExtractor bridge), aibridge (Media3 AI Hooks).
  - `player/`: Media3 ExoPlayer integration, audio-only background service, Picture-in-Picture (PiP).
  - `features/`: feed (subscriptions only), subscriptions (management & import), watchlater, history, settings.
  - Zero business logic inside UI composables.
* **Design System Single Source of Truth**: All UI components MUST exclusively reference `PureTheme.colors.*` and semantic tokens defined in `core/designsystem`. Hardcoded hex color codes anywhere in UI files are **STRICTLY FORBIDDEN**.
* **Graphify & Dependency Mapping**: Query `graphify` / `code-index-mcp` to inspect callers and dependencies before modifying or deleting existing symbols.

---

## 2. Codebase Investigation & Tool Hierarchy
* **Strict Tool Hierarchy**:
  1. `code-index-mcp` (AST / Symbol body / Semantic search) & `ast-grep` (AST search and rewrite) — **Primary tools**.
  2. `graphify` (Knowledge Graph / Impact Analysis).
  3. `grep_search` — **Strictly last resort** (only for unindexed raw strings or non-code files).
* **End-to-End Tracing (Anti-Blindspot)**: Trace complete flow across Jetpack Compose UI, ViewModels/State, Repositories/Extractor, and Room SQLite DB. Never inspect snippets in isolation.
* **Network & Resilience Politeness**: Strictly throttle YouTube extraction calls using Coroutine `Semaphore(4)`. Wrap all `NewPipeExtractor` calls in `Dispatchers.IO` with graceful error handling and user-friendly explanations.

---

## 3. Platform Standards (Modern Android / Jetpack Compose)
* **Compose Best Practices**:
  - Use `remember` and `derivedStateOf` appropriately to avoid unnecessary recompositions.
  - Hoist state; keep composables stateless where possible.
  - Support RTL (Right-to-Left) natively across all layouts and Arabic typography.
* **Coroutines & Lifecycle Safety**:
  - Always tie asynchronous work to `viewModelScope` or `rememberCoroutineScope()`. Never launch dangling un-scoped coroutines.
  - Prevent double-click race conditions on buttons during async calls.
* **Error Handling & Resilience**:
  - Wrap all network, database, and extraction calls in `try-catch` blocks or `Result<T>`.
  - Never crash the app on extraction errors or stream expiration; provide clear, respectful UI recovery states.

---

## 4. Mandatory Android Code Quality & Safety Standards
* **Strict Null Safety**: Leverage Kotlin's null-safety idioms (`?.`, `?:`, `takeIf`). Never use the `!!` (double-bang) operator without verified non-null guarantees.
* **Room Database Safety**: Run database read/write operations on background dispatchers via Coroutines/Flow. Use `@Transaction` for multi-table atomic updates.
* **Production-Ready & Zero Placeholders**: No `// TODO` or partial code stubs. Write complete, type-safe, production-ready Kotlin code.
* **Verification Gate**: Run `./gradlew test` and `./gradlew assembleDebug` (or lint) after code changes to ensure zero compiler errors or regressions.

---

## 5. Planning, Ambiguity Gate & Execution Protocol
* **Ambiguity Gate**: If requirements have multiple interpretations or confidence is < **85%**, stop and ask targeted questions or recommend `/grill-me`. Never make silent assumptions on core features.
* **Self-Critique (Red-Teaming)**: Before presenting any plan or completing an analysis, identify at least 3 potential failure modes, race conditions, or edge cases and address them.
* **Sequential Thinking**: Opt-in only (when requested or for high-risk DB schema / state migrations). Audit Happy Path, Unexpected Failure, and Rapid Interaction scenarios.
* **Subagent Delegation & Governance**:
  - Always grant full capabilities (`enable_write_tools: true`, `enable_subagent_tools: true`, `enable_mcp_tools: true`) when defining or invoking subagents.
  - **Zero-Improvisation Blueprint**: The Leader Agent MUST provide an exhaustive architectural spec in the executor's prompt.
  - **Leader Pre-Audit Git Diff Gate**: Before dispatching code to auditors, the Leader MUST inspect `git diff` for scope creep, debug litter (`println`), and partial implementations.
  - **Auditor Sanctity**: Never kill or terminate auditing subagents prematurely. They must run their full test matrix to completion.

---

## 6. Communication & Reporting (Caveman + PM-Focused)
* **Caveman Mode (Active on Demand)**: Compressed, direct, no filler words, no pleasantries. Preserve 100% technical precision.
* **Artifact Protocol**: Never re-summarize artifact contents in chat; point directly to the created/updated artifact file.
* **PM-Focused Verification**: Frame user updates around functionality and clear, actionable manual verification steps.