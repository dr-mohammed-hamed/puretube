package com.dr.tech.puretube.features.player.util

/**
 * Data class representing a parsed chapter or timestamp link from video descriptions.
 * Timestamps and positions are stored strictly in Long milliseconds (Constitution Principle IV).
 */
data class VideoChapter(
    val title: String,
    val timestampMs: Long,
    val rawText: String
)

/**
 * Utility for detecting, normalizing, and parsing timestamp references within video descriptions.
 * Handles standard ASCII digits (0-9) as well as Arabic-Indic digits (٠-٩).
 */
object TimestampParser {

    private val TIMESTAMP_REGEX = Regex(
        "(?:(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2}))"
    )

    /**
     * Converts Eastern Arabic-Indic numerals (٠-٩) to standard ASCII numerals (0-9).
     */
    fun normalizeDigits(input: String): String {
        val builder = StringBuilder(input.length)
        for (char in input) {
            when (char) {
                '٠' -> builder.append('0')
                '١' -> builder.append('1')
                '٢' -> builder.append('2')
                '٣' -> builder.append('3')
                '٤' -> builder.append('4')
                '٥' -> builder.append('5')
                '٦' -> builder.append('6')
                '٧' -> builder.append('7')
                '٨' -> builder.append('8')
                '٩' -> builder.append('9')
                else -> builder.append(char)
            }
        }
        return builder.toString()
    }

    /**
     * Parses a single timestamp string (e.g. "03:45" or "01:15:30") into milliseconds.
     * Returns null if the format is invalid.
     */
    fun parseTimestampToMillis(rawTimestamp: String): Long? {
        val normalized = normalizeDigits(rawTimestamp.trim())
        val match = TIMESTAMP_REGEX.matchEntire(normalized) ?: return null
        val groups = match.groupValues

        val hours = groups[1].takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val minutes = groups[2].toLongOrNull() ?: return null
        val seconds = groups[3].toLongOrNull() ?: return null

        if (minutes >= 60 && hours > 0L) return null
        if (seconds >= 60) return null

        return (hours * 3600L + minutes * 60L + seconds) * 1000L
    }

    /**
     * Extracts all timestamped chapters found across lines in the video description text.
     * Respects optional durationMs bounds when provided (> 0).
     */
    fun extractChapters(description: String?, durationMs: Long = 0L): List<VideoChapter> {
        if (description.isNullOrBlank()) return emptyList()

        val chapters = mutableListOf<VideoChapter>()
        val lines = description.lines()

        for (line in lines) {
            val normalizedLine = normalizeDigits(line)
            val matches = TIMESTAMP_REGEX.findAll(normalizedLine).toList()
            if (matches.isEmpty()) continue

            for (match in matches) {
                val rawMatch = match.value
                val timestampMs = parseTimestampToMillis(rawMatch) ?: continue

                if (durationMs > 0L && timestampMs > durationMs) {
                    continue
                }

                // Extract surrounding title text from the line
                val cleanTitle = line
                    .removeRange(match.range)
                    .trim(' ', '-', '–', ':', '|', '•')
                    .takeIf { it.isNotBlank() } ?: "المقطع ($rawMatch)"

                chapters.add(
                    VideoChapter(
                        title = cleanTitle,
                        timestampMs = timestampMs,
                        rawText = rawMatch
                    )
                )
            }
        }

        return chapters.distinctBy { it.timestampMs }.sortedBy { it.timestampMs }
    }
}
