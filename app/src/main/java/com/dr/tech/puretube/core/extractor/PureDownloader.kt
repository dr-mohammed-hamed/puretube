package com.dr.tech.puretube.core.extractor

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Custom Downloader implementation backing NewPipeExtractor with OkHttp.
 * Handles HTTP requests, cookie caching, and timeouts cleanly.
 */
class PureDownloader(
    cacheDir: File? = null,
    private val client: OkHttpClient = createDefaultClient(cacheDir)
) : Downloader() {

    companion object {
        const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        private fun createDefaultClient(cacheDir: File?): OkHttpClient {
            val builder = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)

            if (cacheDir != null) {
                val httpCacheDirectory = File(cacheDir, "extractor_http_cache")
                builder.cache(Cache(httpCacheDirectory, 20L * 1024 * 1024))
            }
            return builder.build()
        }
    }

    @Throws(IOException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = try {
            okhttp3.Request.Builder().url(url)
        } catch (e: IllegalArgumentException) {
            throw IOException("Malformed URL: $url", e)
        }

        // Add standard browser User-Agent to prevent HTTP 403 bot blocks
        requestBuilder.header("User-Agent", USER_AGENT)

        headers?.forEach { (headerName, headerValueList) ->
            if (headerName.isNullOrBlank() || headerValueList == null) return@forEach
            val nonNullValues = headerValueList.filterNotNull()
            if (nonNullValues.size > 1) {
                requestBuilder.removeHeader(headerName)
                for (headerValue in nonNullValues) {
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (nonNullValues.size == 1) {
                requestBuilder.header(headerName, nonNullValues[0])
            }
        }

        val requestBody = when {
            httpMethod.equals("POST", ignoreCase = true) ||
            httpMethod.equals("PUT", ignoreCase = true) ||
            httpMethod.equals("PATCH", ignoreCase = true) -> {
                dataToSend?.toRequestBody() ?: ByteArray(0).toRequestBody()
            }
            else -> null
        }
        requestBuilder.method(httpMethod, requestBody)

        return client.newCall(requestBuilder.build()).execute().use { okHttpResponse ->
            if (okHttpResponse.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            val responseBody = okHttpResponse.body?.string() ?: ""
            val responseHeaders = mutableMapOf<String, List<String>>()
            for (headerName in okHttpResponse.headers.names()) {
                responseHeaders[headerName] = okHttpResponse.headers.values(headerName)
            }

            Response(
                okHttpResponse.code,
                okHttpResponse.message,
                responseHeaders,
                responseBody,
                okHttpResponse.request.url.toString()
            )
        }
    }
}
