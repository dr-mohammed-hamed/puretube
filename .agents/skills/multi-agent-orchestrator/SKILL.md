---
name: multi-agent-orchestrator
description: Autonomous multi-agent orchestration, recursive hierarchical tree execution, full parent permissions, static analysis, CodeRabbit-guard auditing, and iterative refinement loop. Use when user requests multi-agent execution, orchestration, /orchestrate, deep audit, gauntlet loop, or when complex code changes require multi-subagent execution with strict constitution auditing.
---

# Multi-Agent Orchestrator (المنسق متعدد الوكلاء الهرمي)

## Overview & Governance Rules

This skill transforms the primary agent into a **Leader Agent (PM/General Director)** managing a **Hierarchical Multi-Tier Tree of Subagents**. Every subagent is empowered with **full parent permissions** (`enable_write_tools: true`, `enable_subagent_tools: true`, `enable_mcp_tools: true`), enabling recursive task decomposition where any subagent can autonomously spawn specialized child subagents (Tier 2+) as needed for complex architectures.

The orchestrator coordinates a strict, multi-pass refinement loop integrating `/coderabbit-guard` with **Completely Neutral Auditing Subagents** to guarantee complete compliance with the project constitution (`.specify/memory/constitution.md`), clean architecture, zero false success declarations, zero unvetted trial-and-error code, and zero-error commit readiness.

### 🛡️ Four Foundational Ironclad Principles

1. **Zero-Improvisation Blueprinting (لا مجال للارتجال):** The Leader Agent MUST give the Lead Executor a comprehensive, end-to-end blueprint in its initial prompt. Vague prompts are strictly banned. The prompt must define the entire lifecycle matrix (e.g., initialization, refresh, error fallback, sibling providers) and explicit boundary files.
2. **Anti-Thrashing & Zero-Pollution Protocol (حظر التخبط وتلويث الكود):** Subagents are strictly forbidden from modifying unrelated production files, injecting exploratory queries (`q_test`, `q_five`), or inserting temporary `print()` statements when tests fail. If a test fails, the agent must diagnose root causes using read-only tools rather than mutating codebase files.
3. **Leader Pre-Audit Git Diff Inspection (بوابة مراجعة الفروقات للوكيل الرئيسي):** Before handing any code over to the auditor, the Leader Agent MUST independently run `git diff` and `git status` to inspect all modifications, verify adherence to the agreed blueprint, detect scope creep or panic-editing, and clean any residual artifacts.
4. **Auditor Sanctity (حظر إنهاء أو قتل الوكيل المدقق قبل اكتماله):** The Leader Agent is STRICTLY FORBIDDEN from calling `kill` or `kill_all` on an active auditor or cancelling background test runs prematurely. Auditing subagents MUST be allowed to run their full test matrix and deliver their comprehensive report uninterrupted.

---

## 🌳 Hierarchical Multi-Tier Subagent Architecture

```mermaid
flowchart TD
    Leader["Leader Agent (Tier 0: General Director / PM)"]
    
    subgraph ExecutionTree["Phase 1: Implementation Phase (Full Parent Permissions)"]
        LeadExec["Subagent 1: Lead Executor (Tier 1)"]
        SubDB["Child Subagent 1A: DB & Migrations (Tier 2)"]
        SubState["Child Subagent 1B: State & Logic (Tier 2)"]
        SubUI["Child Subagent 1C: UI & Compose Layer (Tier 2)"]
        
        LeadExec --> SubDB
        LeadExec --> SubState
        LeadExec --> SubUI
    end

    subgraph LeaderReviewGate["Phase 1.5: Leader Pre-Audit Sanity Gate"]
        LeaderDiff["Leader Agent inspects `git diff` & `git status`"]
        ScopeCheck["Verify: Scope Creep? Panic Edits? Partial Lifecycle?"]
        LeaderDiff --> ScopeCheck
    end

    subgraph AuditTree["Phase 2: Neutral Audit & Verification (Auditor Sanctity)"]
        LeadAudit["Subagent 2: Lead Neutral Auditor (Tier 1)"]
        AudSec["Child Subagent 2A: Security & DB Safety (Tier 2)"]
        AudRace["Child Subagent 2B: Concurrency & State Sync (Tier 2)"]
        AudParity["Child Subagent 2C: API & Design System Parity (Tier 2)"]
        
        LeadAudit --> AudSec
        LeadAudit --> AudRace
        LeadAudit --> AudParity
    end

    subgraph FixTree["Phase 3: Refinement Phase"]
        SubFixer["Subagent 3: Surgical Fixer (Tier 1)"]
    end

    Leader -->|Exhaustive Blueprint Prompt| LeadExec
    LeadExec -->|Reports Code & File List| LeaderReviewGate
    LeaderReviewGate -->|Approved Diff| LeadAudit
    LeaderReviewGate -->|Rejected Scope/Litter| LeadExec
    LeadAudit -->|Pass 100% / Reject (Uninterrupted)| Leader
    Leader -->|If Rejected| SubFixer
    SubFixer -->|Re-Audit Loop| LeadAudit
```

---

## ⚙️ Mandatory Permission Configuration for All Subagents

Whenever defining or invoking ANY subagent in this workflow, the following configuration is **MANDATORY**:

```json
{
  "enable_write_tools": true,
  "enable_subagent_tools": true,
  "enable_mcp_tools": true
}
```
*(Or use `self` subagents when cloning full parent capabilities).*

---

## 📋 Strict Execution Workflow

### Phase 0: Environment & Complete Blueprint Construction (Zero-Code Phase)
Before invoking any subagent, the Leader Agent MUST inspect:
1. Workspace constitution and rules (`.specify/memory/constitution.md`, `.agents/rules/GEMINI.md`).
2. Active MCP servers (`code-index-mcp`, `graphify`, `duckduckgo`).
3. Installed skills and CLI tools (`./gradlew test`, `coderabbit-guard`).
4. **Construct the Exhaustive Blueprint for the Lead Executor:**
   - **Explicit Target File List**: State exactly which files are permitted to be modified.
   - **Full Lifecycle Matrix**: If altering state/providers, the prompt MUST dictate changes across all lifecycle states:
     - `build()` / initial setup
     - `refresh()` / manual or automated re-fetch
     - `onError` / loading / fallback states
   - **Sibling / Peer Entities Scan**: Identify all neighboring repository methods, DAOs, or view-model state flows that share the same data domain (e.g., ensuring related query methods and UI states stay synchronized).
   - **Explicit Anti-Improvisation Clause**: State clearly: *"You are strictly forbidden from modifying any file outside the specified list. Do not insert print statements or exploratory queries into production DAOs if tests fail. Use read-only diagnostics first."*

---

### Phase 1: Subagent 1 (Lead Executor) - Implementation & Anti-Thrashing
- **Turn 1 (Zero-Code Rule)**: The Lead Executor is strictly forbidden from modifying files in its first turn. It MUST use `code-index-mcp` / `ast-grep`, `graphify`, and `view_file` to map the codebase.
- **Strict Adherence to Blueprint**: The Executor MUST execute the exact plan without improvising unrequested architectural patterns.
- **Anti-Thrashing Protocol (حظر دوامة التجربة والخطأ)**:
  - If a unit or integration test fails, the Executor MUST NOT start modifying core DAOs, deleting files, or injecting trial statements (`print()`, `Log.d()`, exploratory joins) into production files.
  - The Executor must diagnose the test failure using test logs and read-only inspection.
  - If a file was modified during debugging, the Executor MUST run `git checkout <file>` to revert it before reporting completion.
- **Autonomous Tree Decomposition (Tier 2 Subagents)**:
  - If the task is multi-faceted, the Lead Executor uses `define_subagent` / `invoke_subagent` (with full parent permissions) to spawn specialized child subagents:
    - `Child 1A (DB Specialist)`: For Room SQLite, migrations, and local tables.
    - `Child 1B (State/Logic Specialist)`: For ViewModels, Repositories, and Extractor.
    - `Child 1C (UI Specialist)`: For Jetpack Compose composables, RTL layout, and theme compliance.
- **Executor Pre-Delivery Self-Check**:
  - Run `git status` and `git diff` to ensure 0 unintended files modified and 0 debug prints left behind.
  - Report exact modifications made to the Leader Agent.

---

### Phase 1.5: Leader Pre-Audit Sanity & Git Diff Gate (بوابة الفحص الصارم للوكيل الرئيسي)
Before spawning or handing work over to the Lead Auditor, the **Leader Agent MUST personally inspect the working tree**:
1. **Run `git status -s` and `git diff` directly in the terminal.**
2. **Three-Point Gate Audit:**
   - **A. Scope Creep Check**: Did the executor touch files outside the agreed list (e.g., editing `.gitignore`, DAOs, or config files without authorization)? If yes, immediately revert them via `git checkout`.
   - **B. Code Litter Check**: Are there any debug `print()`, temporary test queries, unformatted snippets, or leftover TODOs? If yes, reject or clean immediately.
   - **C. Lifecycle Completeness Check**: Did the executor only implement half the solution (e.g., guarding `build()` but forgetting `refresh()`, or guarding one provider but forgetting its sibling providers in the same file)? If incomplete, instruct the executor to finish the missing pieces before audit.
3. Only when the Leader Agent confirms the diff is clean, surgical, and complete, proceed to Phase 2.

---

### Phase 2: Subagent 2 (Lead Neutral CodeRabbit Guard Auditor) & Auditor Sanctity
The Lead Auditor MUST be spawned with a **strictly neutral, zero-author-bias prompt**. It treats all modified code as untrusted 3rd-party code.

- **Auditor Sanctity Rule (حظر مقاطعة أو قتل الوكيل المدقق):**
  - The Leader Agent MUST NOT terminate (`kill` / `kill_all`) the Lead Auditor or abort its background test tasks while it is running.
  - Even if basic tests pass or the Leader feels confident, the Auditor MUST be allowed to finish running full regression checks (e.g. `test/notifiers/`, database suites, or static analysis) and deliver its final evaluation.
- **Recursive Audit Delegation (Tier 2 Subagents)**:
  The Lead Auditor defines and invokes parallel specialized auditor subagents:
  1. **Auditor 2A (Security & DB Safety)**: Parameterized queries, schema alignment, data leaks, swallowed errors, SQLite runtime DDL safety.
  2. **Auditor 2B (Concurrency, State & Persistence Sync)**: Reactive Flow invalidation, Coroutine lifecycle safety (`viewModelScope` / `LaunchedEffect`), state vs DB desync, and **Mandatory Red-Teaming** (rapid double-taps, unhandled I/O failures).
  3. **Auditor 2C (API Contracts & PureTheme Parity)**: Signature changes, caller breakage, and strict adherence to `PureTheme.colors.*` (zero hardcoded colors).
- **Linter & Static Analysis**: Runs `./gradlew test` or `./gradlew lintDebug` directly to capture all compiler & linter warnings.
- **Synthesis & Severity Rating**:
  - Aggregates findings from child auditors into `[CRITICAL]`, `[MAJOR]`, `[MINOR]`.
  - Provides exact line references `[file_basename](file:///path/to/file#L10-L25)` and concrete diff suggestions.
  - Declares whether the code is **"100% VERIFIED CLEAN & READY FOR COMMIT"** or **"REJECTED - REQUIRES FIXES"**.

---

### Phase 3: Subagent 3 (Surgical Fixer) & Iterative Refinement Loop
If the Lead Auditor reports ANY `[CRITICAL]` or `[MAJOR]` issues (or score < 100%):
1. **Subagent 3 (Surgical Fixer)** is launched with full write permissions and the exact Auditor diff report to apply surgical fixes.
2. **Mandatory Re-Audit Rule**: ANY modification made by the Fixer MUST trigger a fresh re-audit cycle by **Lead Neutral Auditor (Subagent 2)** and its specialized auditing tree.
3. The loop (`Fixer` ➔ `Lead Auditor`) MUST continue until the Lead Auditor explicitly declares **0 CRITICAL, 0 MAJOR issues, 0 analyze errors, and 100% READY FOR COMMIT**.

---

### Phase 4: Leader Verification Gate & Circuit Breaker
The Leader Agent continuously oversees the tree:
- **Mandatory Leader Checkpoint**: BEFORE presenting the final answer or updating the PM artifact, the Leader Agent MUST independently run `./gradlew test` and primary verification commands directly in the terminal to empirically verify **0 syntax errors, 0 linter errors, and exit code 0**. NEVER rely solely on subagent claims without executing the verification command directly.
- **Loop End (Success)**: Leader Agent verifies 100% CodeRabbit pass, 0 compile errors, 100% clean `git status`, and zero extraneous code.
- **Circuit Breaker**: If oscillation or repetition occurs (> 3 iterations), Leader Agent halts child subagents and directly resolves the bottleneck.

---

## Deliverable: PM Executive Summary Artifact

Upon completion, the Leader Agent MUST create an **Executive Summary Artifact** for the non-programmer PM formatted as follows:

```markdown
# 📊 Executive Summary: [Feature / Task Name]

## 💡 Business Value & Key Updates
- [Clear non-technical description of what was built or fixed]
- [User experience impact]

## 🌳 Subagent Tree Execution
- **Lead Executor**: Decomposed into [X] child specialists (adhered to Zero-Improvisation Blueprint).
- **Leader Sanity Gate**: Verified `git diff` for zero scope creep and zero debug litter.
- **Lead Neutral Auditor**: Multi-tier audit across Security, Concurrency, and Platform Parity completed without premature interruption.
- **Refinement Iterations**: [X] Rounds

## 🔍 Audit & Quality Assurance Loop
- **CodeRabbit Guard Pass Status**: 100% Verified Clean & Ready for Commit
- **Bugs Prevented**: [List of bugs/flaws caught during auditing and fixed]

## 🧾 Proof Matrix (Empirical Evidence)
```text
[Exact terminal output of ./gradlew test - 0 errors, 0 warnings]
```

## 🎯 PM Verification Guide
1. Open the application.
2. [Step-by-step simple instructions for the PM to test the feature]
```

