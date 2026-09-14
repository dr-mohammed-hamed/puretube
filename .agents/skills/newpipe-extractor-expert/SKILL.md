---
name: newpipe-extractor-expert
description: Expert in NewPipeExtractor integration, resilient stream parsing, Coroutine Semaphore concurrency throttling, OkHttp caching, and graceful failure degradation.
version: 1.0.0
tags: [newpipe, extractor, youtube, parsing, network, semaphore, caching]
---

# NewPipe Extractor Expert

## 1. Overview & Architectural Role

This skill governs data extraction, YouTube stream URL resolution, network resilience, and upstream politeness using **NewPipeExtractor** within modern Android architecture.

### Core Principles
1. **Network Politeness & Concurrency Throttling**: Fetching data across multiple channels or streams must strictly be rate-limited using a Coroutine `Semaphore(limit = 4)` to avoid IP throttling, memory spikes, and device battery drain.
2. **IO Isolation**: All extraction and HTTP parsing operations must execute strictly on `Dispatchers.IO`.
3. **Smart HTTP Caching**: Back the downloader implementation with an `OkHttpClient` equipped with a local disk cache for thumbnails, channel avatars, and metadata.
4. **Graceful Degradation**: Upstream changes in streaming platforms (e.g., signature deciphering updates) must be caught cleanly and surfaced with user-friendly explanations rather than crashing the app.
5. **Anti-Addiction Intentional Fetching**: Never execute queries for automated recommendation engines, trending sections, or endless short-form loops. Fetch content strictly for user-selected channels and intentional watch lists.

---

## 2. Implementation Patterns

### 2.1 Concurrency Limiting via Coroutine Semaphore
When orchestrating concurrent extraction tasks (e.g. syncing multiple feeds):

```kotlin
class ExtractorThrottler(
    private val maxConcurrentCalls: Int = 4
) {
    private val semaphore = kotlinx.coroutines.sync.Semaphore(maxConcurrentCalls)

    suspend fun <T> throttledCall(action: suspend () -> T): T {
        return semaphore.withPermit {
            withContext(Dispatchers.IO) {
                action()
            }
        }
    }
}
```

### 2.2 Resilient Result Wrapping
Wrap extractor calls inside a robust `Result` boundary to handle network errors and upstream parsing exceptions gracefully:

```kotlin
suspend fun <T> safeExtract(extractCall: () -> T): Result<T> {
    return withContext(Dispatchers.IO) {
        try {
            Result.success(extractCall())
        } catch (e: Exception) {
            // Log diagnostic info without exposing raw stack traces to the user interface
            Result.failure(e)
        }
    }
}
```

---

## 3. Data Portability & Storage Interoperability

- Support parsing and importing standard subscription backup formats (NewPipe JSON and Google Takeout CSV/JSON).
- Maintain local caching in Room to ensure the app remains fully functional and browsable offline for already-fetched metadata.
