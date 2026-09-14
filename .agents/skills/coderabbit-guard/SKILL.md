---
name: coderabbit-guard
description: Strict CodeRabbit-grade code reviewer & AI failure-mode detector for Android & Kotlin. Parses git diffs, runs static analysis, extracts AST/Graphify dependencies, audits Compose recompositions, Room DB concurrency, and Media3 lifecycle safety. Use when user says "coderabbit", "review PR", "audit changes", "coderabbit-guard", "فحص الكود قبل الـ commit", or asks for deep strict AI code review.
---

# CodeRabbit Guard (Android & Kotlin Agentic Reviewer)

## Overview

`coderabbit-guard` performs strict, multi-agent AI code reviews tailored for Android, Kotlin & Jetpack Compose applications. It replicates CodeRabbit's deep inspection pipeline without external paid APIs by combining git diff chunk parsing, AST/symbol analysis (`code-index-mcp`), dependency impact mapping (`graphify`), static compiler diagnostics (`./gradlew lint`), and 3 specialized subagent reviewers.

## Workflow

When triggered (e.g. before `git commit` or when reviewing changes):

1. **Diff Retrieval & Layer Chunking**:
   - Extract staged/unstaged changes or target commit diff.
   - Categorize changed files into: Data/DB Layer (`Room`, `extractor`), State/Logic Layer (`ViewModels`, `Repositories`), and UI Layer (`Compose`, `designsystem`).
   - Details: See [references/audit-pipeline.md](references/audit-pipeline.md).

2. **Static Linter & Compiler Pass**:
   - Run `./gradlew lintDebug` or `./gradlew test` to capture all compiler & linter warnings.

3. **Symbol Tracing & Impact Enrichment**:
   - Query `code-index-mcp` and `graphify` (if available) for modified symbol callers and transitive component impact.

4. **Parallel Subagent Audit**:
   Spawn 3 parallel subagents (or focused inspection passes) using roles defined in [references/subagents-roles.md](references/subagents-roles.md):
   - **Subagent A (Security, Resiliency & Extractor Safety)**: Parameterized SQL bindings in Room, NewPipeExtractor concurrency throttling (`Semaphore(4)`), unhandled exceptions, and data leaks.
   - **Subagent B (Concurrency, Compose Recomposition & Persistence)**: Main thread Room DB access, Coroutine lifecycle safety (`rememberCoroutineScope` vs `LaunchedEffect`), state observables, and **Mandatory Red-Teaming Audit** (3 scenarios: rapid double clicks, network timeouts, stale memory cache).
   - **Subagent C (API Contracts & Design System Compliance)**: Method signature changes, broken call sites, and **STRICT compliance with `PureTheme.colors.*`** (flagging any hardcoded hex color values in UI files).

5. **Framework-Specific Guardrails**:
   Apply rules from [references/android-checks.md](references/android-checks.md) or project environment rules.

6. **Synthesis & Severity Rating**:
   Consolidate findings into a clean report:
   - `[CRITICAL]`: Must fix before commit (Main thread DB query, SQL Injection, crash on extraction error, app crash, broken contracts, hardcoded colors bypassing design system).
   - `[MAJOR]`: Maintainability/performance risk (swallowed errors, unbounded recompositions, missing transaction on multi-table writes).
   - `[MINOR]`: Code style & minor optimization.
   Provide exact code replacement diffs for all `[CRITICAL]` and `[MAJOR]` findings.

## Output Format

Report findings using:
- Priority breakdown (`[CRITICAL]`, `[MAJOR]`, `[MINOR]`).
- Exact file path & line numbers `[file_basename](file:///path/to/file#L10-L25)`.
- Root cause explanation (why CodeRabbit would flag this).
- Drop-in git diff replacement.
