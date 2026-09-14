# Universal Project Memory Template (قالب الذاكرة المعمارية العالمي)

> **Purpose:** Use this universal template to bootstrap or structure an authoritative architectural memory file (`.specify/memory/project_memory.md` or `project_memory.md`) for ANY software project.

---

```markdown
# Architectural Memory & Strategic Decision Log — [PROJECT_NAME]

> **System Version:** [VERSION] | **Last Architecture Audit:** [DATE_YYYY_MM_DD]  
> **Tech Stack:** [PRIMARY_FRAMEWORKS_AND_DATABASES] (e.g. Flutter + SQLite / Next.js + PostgreSQL / FastAPI + Redis)  
> **Project Context & Target Domain:** [BRIEF_1_2_SENTENCE_EXPLANATION_OF_THE_PRODUCT_AND_BUSINESS_GOAL]  
> **Leadership & Management Persona:** [PM_NAME_AND_BACKGROUND] (e.g. Non-programmer PM Lead / Tech Lead). [PREFERRED_COMMUNICATION_STYLE] (e.g. Explain functional value, provide explicit manual verification steps, avoid jargon).

---

## 🏛️ Section 1: Non-Negotiables & Strategic Choices (الثوابت المعمارية وخيارات المدير المحسومة)

These rules represent the supreme invariants of the codebase. Every AI agent and human engineer MUST strictly adhere to them:

1. **Core Domain Invariants (الثوابت المالية أو التقنية الأساسية):**
   - [e.g. For fintech/commerce: Integer piastres/cents rule; never use float/double for money calculations.]
   - [e.g. For security/privacy: Encrypt sensitive data at rest; zero plain-text tokens in logs.]

2. **Data Immutability & Ledger Discipline (سجلات قطعية غير قابلة للتعديل):**
   - [e.g. Finalized transaction records are append-only. Corrections via compensating reversals only. Soft deletes via is_deleted flag.]

3. **Code Style, Architecture & Typography (الهوية والمعمارية):**
   - [e.g. Clean Architecture (UI -> State -> Domain/Repo -> Data). File ceiling 1000 lines. Required UI fonts and design system tokens.]

4. **Agentic Governance Rules (حوكمة وإدارة الوكلاء):**
   - **Zero-Improvisation Blueprint:** Agents must be provided with complete lifecycle specifications before modifying code.
   - **Anti-Thrashing Protocol:** Prohibit trial-and-error edits in production databases or inserting debug print litter when tests fail.
   - **Leader Pre-Audit Git Diff Gate:** Inspect `git diff` for scope creep before handing code to auditors.
   - **Auditor Sanctity:** Auditing agents must run their test suite to completion without premature termination.

5. **Disaster Recovery & Full Backups (النسخ الاحتياطي الكامل):**
   - Standalone scripts to export project source and database states on demand.

---

## ⚠️ Section 2: Hard-Learned Lessons & Fatal Anti-Patterns (الأخطاء والدروس المستفادة المحظور تكرارها)

Document critical mistakes made in earlier phases to guarantee they are never repeated:

1. **[ANTI_PATTERN_NAME_1] (e.g. Startup Race Condition):**
   - **Root Cause:** [What went wrong in the code timing or state?]
   - **Enforced Rule:** [The strict rule or guard that must always be applied.]

2. **[ANTI_PATTERN_NAME_2] (e.g. Half-Lifecycle Fix Trap):**
   - **Root Cause:** [Fixing initialization but forgetting refresh or error states.]
   - **Enforced Rule:** [Full lifecycle consistency required across all sibling entities.]

3. **[ANTI_PATTERN_NAME_3] (e.g. Permissive Filter Leaks):**
   - **Root Cause:** [Lax conditions matching unintended rows.]
   - **Enforced Rule:** [Strict multi-tenant or entity scoping.]

4. **[ANTI_PATTERN_NAME_4] (e.g. Premature Victory Declaration):**
   - **Root Cause:** [Declaring task complete before running automated tests.]
   - **Enforced Rule:** [Empirical static analysis and test passing required.]

---

## 📅 Section 3: Milestone Decision Log (السجل الزمني المركز للقرارات الهندسية)

Chronological, verified record of architectural milestones extracted from git commits:

### [YYYY-MM-DD] [Milestone Feature or Fix Title]
* **Commit Hash:** `[SHORT_HASH]`
* **Affected Files:** `[KEY_FILES_CHANGED]`
* **Architectural Value:** [Precise explanation of what was built or resolved, how it works, and why it matters.]

### [YYYY-MM-DD] [Previous Milestone Title]
* **Commit Hash:** `[SHORT_HASH]`
* **Affected Files:** `[KEY_FILES_CHANGED]`
* **Architectural Value:** [Description]
```
