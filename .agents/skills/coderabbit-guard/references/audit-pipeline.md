# CodeRabbit Audit Pipeline Details (Android & Kotlin)

This reference file defines the 5-stage automated audit pipeline used by `coderabbit-guard` for PureTube.

---

## Stage 1: AST-like Diff Retrieval & Layer Chunking

1. Retrieve active git changes:
   - For working directory: `git diff HEAD` or staged `git diff --cached`.
   - For specific commit: `git show <commit_hash>`.
2. Parse added/modified chunks:
   - Separate code diffs from configuration (`build.gradle.kts`, `libs.versions.toml`).
   - Group modified Kotlin symbols (Classes, Interfaces, Composable Functions, ViewModels, Repositories, DAOs).
3. Classify file layer:
   - **Data/DB**: `core/database/`, `dao/`, `entities/`, `core/extractor/`.
   - **State/Logic**: `viewmodel/`, `repository/`, `domain/`.
   - **UI/Presentation**: `core/designsystem/`, `features/`, `@Composable` components.

---

## Stage 2: Mechanical Static Linter Pass

Execute `./gradlew lintDebug` or `./gradlew test` via `run_command`:
- Filter output to only include errors/warnings affecting changed files.
- Treat compiler errors as immediate `[CRITICAL]` failures.
- Treat warnings (`UnusedVariable`, `UnnecessarySafeCall`, missing Compose stability) as input context for Subagents.

---

## Stage 3: AST Symbol Tracing & Graphify Impact Mapping

1. **AST & Caller Search (`code-index-mcp` / `ast-grep`)**:
   - For every modified function or interface, search callers and symbol usages.
   - Verify if any caller site was broken by parameter signature changes or return type modifications.
2. **Graphify Dependency Mapping (`graphify`)**:
   - Query `graphify` knowledge graph for modified file nodes.
   - Trace callers up 2 dependency levels to detect indirect side effects.

---

## Stage 4: Subagent Execution Protocol

Subagents are executed in parallel (or sequential dedicated prompt blocks) with strict non-overlapping responsibilities:
- Subagent A: Focuses exclusively on Security, Exceptions, Null Safety, and Extractor Throttling.
- Subagent B: Focuses exclusively on Room DB Concurrency, Compose Recompositions, and Coroutine Lifecycle Safety.
- Subagent C: Focuses exclusively on API Contract Integrity, Signature Parity, and Strict Design System (`PureTheme`) Compliance.

---

## Stage 5: Noise Reduction & Severity Deduplication

Before rendering the final report:
1. Deduplicate findings across subagents.
2. Filter out subjective style nitpicks unless explicitly requested.
3. Categorize severity:
   - `[CRITICAL]`: Main thread Room DB access, SQL injection, hardcoded UI colors bypassing `PureTheme`, app crashes on extraction errors, broken contracts.
   - `[MAJOR]`: Silent exception swallowing, missing `@Transaction` on multi-table writes, unscoped coroutines, unnecessary heavy recompositions.
   - `[MINOR]`: Redundant imports, minor code duplication.
4. Output concise GitHub-flavored markdown with code diffs.
