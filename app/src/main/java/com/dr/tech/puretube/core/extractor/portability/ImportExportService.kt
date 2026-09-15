package com.dr.tech.puretube.core.extractor.portability

import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Service for importing and exporting subscriptions.
 * Supports NewPipe JSON format and Google Takeout CSV/JSON formats
 * upholding Constitution Principle III (Privacy & Full Data Ownership).
 */
class ImportExportService {

    /**
     * Parses a NewPipe subscription export JSON string into a list of SubscriptionEntity.
     * Supports both modern object format { "subscriptions": [...] } and legacy array format [...].
     */
    fun parseNewPipeJson(jsonContent: String): Result<List<SubscriptionEntity>> {
        return runCatching {
            val entities = mutableListOf<SubscriptionEntity>()
            val trimmed = jsonContent.trim()
            if (trimmed.isEmpty()) return@runCatching emptyList()

            val array = if (trimmed.startsWith("{")) {
                val jsonObject = JSONObject(trimmed)
                if (jsonObject.has("subscriptions")) {
                    jsonObject.getJSONArray("subscriptions")
                } else {
                    JSONArray()
                }
            } else if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                throw IllegalArgumentException("Invalid NewPipe JSON format")
            }

            val currentTime = System.currentTimeMillis()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val url = item.optString("url", "").trim()
                val name = item.optString("name", "").trim()
                val channelId = extractChannelIdFromUrl(url)

                if (channelId.isNotEmpty()) {
                    val displayName = name.ifEmpty { channelId }
                    val handle = if (url.contains("/@")) "@" + url.substringAfter("/@").substringBefore("/").substringBefore("?").substringBefore("#") else null

                    entities.add(
                        SubscriptionEntity(
                            channelId = channelId,
                            channelName = displayName,
                            channelHandle = handle,
                            avatarUrl = null,
                            subscriberCountText = null,
                            isNotificationsEnabled = true,
                            subscribedAtMs = currentTime + (array.length() - i)
                        )
                    )
                }
            }

            // Deduplicate by channelId keeping the first occurrence
            entities.distinctBy { it.channelId }
        }
    }

    /**
     * Parses a Google Takeout subscriptions CSV string into a list of SubscriptionEntity.
     * Typical format:
     * Channel Id,Channel Url,Channel Title
     * UC...,https://www.youtube.com/channel/UC...,Channel Name
     */
    fun parseGoogleTakeoutCsv(csvContent: String): Result<List<SubscriptionEntity>> {
        return runCatching {
            val entities = mutableListOf<SubscriptionEntity>()
            val lines = csvContent.lines()
            if (lines.isEmpty()) return@runCatching emptyList()

            var idColIdx = 0
            var nameColIdx = 2
            var isHeaderSkipped = false
            val currentTime = System.currentTimeMillis()

            for (lineIndex in lines.indices) {
                val rawLine = lines[lineIndex].trim()
                if (rawLine.isBlank()) continue

                val columns = parseCsvLine(rawLine)
                if (!isHeaderSkipped) {
                    val lowerHeader = columns.map { it.lowercase() }
                    val foundIdIdx = lowerHeader.indexOfFirst { it.contains("channel id") }
                    val foundNameIdx = lowerHeader.indexOfFirst { it.contains("channel title") || it.contains("channel name") }

                    if (foundIdIdx != -1) {
                        idColIdx = foundIdIdx
                        nameColIdx = if (foundNameIdx != -1) foundNameIdx else 2
                        isHeaderSkipped = true
                        continue
                    }
                    isHeaderSkipped = true
                }

                if (columns.size > idColIdx) {
                    val rawId = columns[idColIdx].trim()
                    val channelId = extractChannelIdFromUrl(rawId)
                    val channelName = if (columns.size > nameColIdx && columns[nameColIdx].isNotBlank()) {
                        columns[nameColIdx].trim()
                    } else {
                        channelId
                    }

                    if (channelId.isNotEmpty()) {
                        entities.add(
                            SubscriptionEntity(
                                channelId = channelId,
                                channelName = channelName,
                                channelHandle = null,
                                avatarUrl = null,
                                subscriberCountText = null,
                                isNotificationsEnabled = true,
                                subscribedAtMs = currentTime + (lines.size - lineIndex)
                            )
                        )
                    }
                }
            }

            entities.distinctBy { it.channelId }
        }
    }

    /**
     * Exports current subscriptions to a standard NewPipe-compatible JSON string.
     */
    fun exportToNewPipeJson(subscriptions: List<SubscriptionEntity>): String {
        val root = JSONObject()
        root.put("app_version", "PureTube-1.0")
        root.put("app_version_int", 100)

        val array = JSONArray()
        for (sub in subscriptions) {
            val item = JSONObject()
            item.put("service_id", 0) // YouTube service ID in NewPipe
            val url = if (sub.channelId.startsWith("@")) {
                "https://www.youtube.com/${sub.channelId}"
            } else {
                "https://www.youtube.com/channel/${sub.channelId}"
            }
            item.put("url", url)
            item.put("name", sub.channelName)
            array.put(item)
        }

        root.put("subscriptions", array)
        return root.toString(2)
    }

    /**
     * Helper to extract a YouTube channel ID from a URL or raw ID string.
     */
    fun extractChannelIdFromUrl(urlOrId: String): String {
        val cleaned = urlOrId.trim()
        return when {
            cleaned.startsWith("UC") && cleaned.length >= 20 -> cleaned.substringBefore("?").substringBefore("/")
            cleaned.contains("/channel/") -> cleaned.substringAfter("/channel/").substringBefore("/").substringBefore("?")
            cleaned.contains("/@") -> "@" + cleaned.substringAfter("/@").substringBefore("/").substringBefore("?")
            cleaned.contains("/user/") -> cleaned.substringAfter("/user/").substringBefore("/").substringBefore("?")
            cleaned.contains("/c/") -> cleaned.substringAfter("/c/").substringBefore("/").substringBefore("?")
            cleaned.startsWith("@") -> cleaned.substringBefore("?").substringBefore("/").substringBefore("#")
            else -> cleaned
        }
    }

    /**
     * CSV line splitter supporting quoted strings and RFC-4180 escaped quotes ("").
     */
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // Skip the second quote of the escaped pair
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
