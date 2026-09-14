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
- Zero CPU/memory overhead when filters are disabled (NoOp by default).

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

### V. Feature-First Modern Android Architecture
- Clean modular structure: `core` (database, designsystem, extractor, aibridge), `player`, and `features` (feed, subscriptions, watchlater, history, settings).
- Jetpack Compose with anti-dopamine, calm palette (Sage Green, Slate Gray, Midnight Black) and RTL-first support.
- Fully production-ready code with no incomplete implementations or stubbed placeholders.

## Governance
This Constitution defines the architectural boundaries and product ethos for PureTube. All feature specifications (`speckit-specify`), implementation plans (`speckit-plan`), and tasks (`speckit-tasks`) must adhere strictly to these principles. Any proposed feature introducing addictive UX patterns or unverified external tracking must be rejected.

**Version**: 1.0.0 | **Ratified**: 2026-09-14 | **Last Amended**: 2026-09-14
