package com.dr.tech.puretube.core.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Utility for formatting video upload timestamps into natural Arabic relative time.
 * Conforms to Constitution Principles V, VI, VII:
 * Zero emojis, type-safe, < 150 LOC.
 */
object ArabicTimeFormatter {

    private val englishRelativeRegex =
        """(\d+)\s+(second|minute|hour|day|week|month|year)s?\s+ago""".toRegex(RegexOption.IGNORE_CASE)

    fun formatRelativeTime(uploadDateMs: Long, uploadDateText: String? = null): String {
        if (uploadDateMs > 0L) {
            return formatFromEpoch(uploadDateMs)
        }

        val text = uploadDateText?.trim()
        if (text.isNullOrBlank()) {
            return ""
        }

        // 1. Check if the string already contains Arabic characters
        if (text.any { it in '\u0600'..'\u06FF' }) {
            return text
        }

        // 2. Check for English relative time (e.g., "3 days ago", "1 month ago")
        val match = englishRelativeRegex.find(text)
        if (match != null) {
            val count = match.groupValues[1].toIntOrNull() ?: 1
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            return formatCountUnit(count, unit)
        }

        // 3. Try parsing ISO-8601 timestamps
        val parsedEpoch = parseIsoTimestamp(text)
        if (parsedEpoch != null && parsedEpoch > 0L) {
            return formatFromEpoch(parsedEpoch)
        }

        // 4. Fallback: try parsing generic dates or format as yyyy/MM/dd
        val parsedFallbackEpoch = parseGenericDate(text)
        if (parsedFallbackEpoch != null && parsedFallbackEpoch > 0L) {
            return formatAbsoluteDate(parsedFallbackEpoch)
        }

        return text
    }

    private fun parseIsoTimestamp(text: String): Long? {
        return try {
            OffsetDateTime.parse(text).toInstant().toEpochMilli()
        } catch (_: Throwable) {
            try {
                Instant.parse(text).toEpochMilli()
            } catch (_: Throwable) {
                try {
                    LocalDate.parse(text).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }

    private fun parseGenericDate(text: String): Long? {
        val formats = listOf("yyyy-MM-dd", "yyyy/MM/dd", "MMM d, yyyy", "MMMM d, yyyy")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(text)
                if (date != null) return date.time
            } catch (_: Throwable) {
                // Try next pattern
            }
        }
        return null
    }

    private fun formatAbsoluteDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.ROOT)
        return sdf.format(Date(epochMs))
    }

    private fun formatFromEpoch(uploadDateMs: Long): String {
        val nowMs = System.currentTimeMillis()
        val diffMs = (nowMs - uploadDateMs).coerceAtLeast(0L)
        val diffSec = diffMs / 1000L

        if (diffSec < 60L) {
            return "الآن"
        }

        val diffMin = diffSec / 60L
        if (diffMin < 60L) {
            return formatMinutes(diffMin.toInt())
        }

        val diffHour = diffMin / 60L
        if (diffHour < 24L) {
            return formatHours(diffHour.toInt())
        }

        val diffDay = diffHour / 24L
        if (diffDay < 7L) {
            return formatDays(diffDay.toInt())
        }

        if (diffDay < 30L) {
            val weeks = (diffDay / 7L).toInt().coerceAtLeast(1)
            return formatWeeks(weeks)
        }

        if (diffDay < 365L) {
            val months = (diffDay / 30L).toInt().coerceAtLeast(1)
            return formatMonths(months)
        }

        val years = (diffDay / 365L).toInt().coerceAtLeast(1)
        return formatYears(years)
    }

    private fun formatCountUnit(count: Int, unit: String): String {
        return when (unit) {
            "second" -> "الآن"
            "minute" -> formatMinutes(count)
            "hour" -> formatHours(count)
            "day" -> formatDays(count)
            "week" -> formatWeeks(count)
            "month" -> formatMonths(count)
            "year" -> formatYears(count)
            else -> "الآن"
        }
    }

    private fun formatMinutes(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ دقيقة"
        c == 2 -> "منذ دقيقتين"
        c in 3..10 -> "منذ $c دقائق"
        else -> "منذ $c دقيقة"
    }

    private fun formatHours(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ ساعة"
        c == 2 -> "منذ ساعتين"
        c in 3..10 -> "منذ $c ساعات"
        else -> "منذ $c ساعة"
    }

    private fun formatDays(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ يوم"
        c == 2 -> "منذ يومين"
        c in 3..10 -> "منذ $c أيام"
        else -> "منذ $c يوماً"
    }

    private fun formatWeeks(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ أسبوع"
        c == 2 -> "منذ أسبوعين"
        c in 3..10 -> "منذ $c أسابيع"
        else -> "منذ $c أسبوعاً"
    }

    private fun formatMonths(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ شهر"
        c == 2 -> "منذ شهرين"
        c in 3..10 -> "منذ $c أشهر"
        else -> "منذ $c شهراً"
    }

    private fun formatYears(c: Int): String = when {
        c <= 0 -> "الآن"
        c == 1 -> "منذ سنة"
        c == 2 -> "منذ سنتين"
        c in 3..10 -> "منذ $c سنوات"
        else -> "منذ $c سنة"
    }
}
