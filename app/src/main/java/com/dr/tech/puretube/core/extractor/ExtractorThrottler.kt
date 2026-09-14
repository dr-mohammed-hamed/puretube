package com.dr.tech.puretube.core.extractor

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.IOException

/**
 * Concurrency throttler for extraction calls according to Constitution Principle IV.
 * Strictly limits concurrent YouTube extractions to 4 parallel coroutines to prevent
 * IP throttling, device battery exhaustion, and memory spikes.
 */
class ExtractorThrottler(
    maxConcurrentCalls: Int = 4
) {
    private val semaphore = Semaphore(maxConcurrentCalls)

    /**
     * Executes a suspending action within the rate-limited semaphore on Dispatchers.IO.
     */
    suspend fun <T> throttledCall(action: suspend () -> T): T {
        return semaphore.withPermit {
            withContext(Dispatchers.IO) {
                action()
            }
        }
    }

    /**
     * Executes an extraction block safely wrapped in Result<T> with IO isolation and timeout boundary.
     */
    suspend fun <T> safeExtract(
        timeoutMillis: Long = 20_000L,
        extractCall: suspend () -> T
    ): Result<T> {
        return semaphore.withPermit {
            withContext(Dispatchers.IO) {
                try {
                    withTimeout(timeoutMillis) {
                        Result.success(extractCall())
                    }
                } catch (e: TimeoutCancellationException) {
                    Result.failure(IOException("Extraction timed out after ${timeoutMillis}ms", e))
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
        }
    }
}
