package com.dr.tech.puretube.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ArabicTimeFormatterTest {

    @Test
    fun testRelativeTimeSeconds() {
        val now = System.currentTimeMillis()
        assertEquals("الآن", ArabicTimeFormatter.formatRelativeTime(now - 10_000L))
        assertEquals("الآن", ArabicTimeFormatter.formatRelativeTime(now - 55_000L))
    }

    @Test
    fun testRelativeTimeMinutes() {
        val now = System.currentTimeMillis()
        assertEquals("منذ دقيقة", ArabicTimeFormatter.formatRelativeTime(now - 60_000L))
        assertEquals("منذ دقيقتين", ArabicTimeFormatter.formatRelativeTime(now - 120_000L))
        assertEquals("منذ 3 دقائق", ArabicTimeFormatter.formatRelativeTime(now - 180_000L))
        assertEquals("منذ 10 دقائق", ArabicTimeFormatter.formatRelativeTime(now - 600_000L))
        assertEquals("منذ 11 دقيقة", ArabicTimeFormatter.formatRelativeTime(now - 660_000L))
        assertEquals("منذ 45 دقيقة", ArabicTimeFormatter.formatRelativeTime(now - 45 * 60_000L))
    }

    @Test
    fun testRelativeTimeHours() {
        val now = System.currentTimeMillis()
        val hourMs = 3600_000L
        assertEquals("منذ ساعة", ArabicTimeFormatter.formatRelativeTime(now - 1 * hourMs))
        assertEquals("منذ ساعتين", ArabicTimeFormatter.formatRelativeTime(now - 2 * hourMs))
        assertEquals("منذ 5 ساعات", ArabicTimeFormatter.formatRelativeTime(now - 5 * hourMs))
        assertEquals("منذ 10 ساعات", ArabicTimeFormatter.formatRelativeTime(now - 10 * hourMs))
        assertEquals("منذ 14 ساعة", ArabicTimeFormatter.formatRelativeTime(now - 14 * hourMs))
    }

    @Test
    fun testRelativeTimeDays() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600_000L
        assertEquals("منذ يوم", ArabicTimeFormatter.formatRelativeTime(now - 1 * dayMs))
        assertEquals("منذ يومين", ArabicTimeFormatter.formatRelativeTime(now - 2 * dayMs))
        assertEquals("منذ 4 أيام", ArabicTimeFormatter.formatRelativeTime(now - 4 * dayMs))
    }

    @Test
    fun testRelativeTimeWeeks() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600_000L
        assertEquals("منذ أسبوع", ArabicTimeFormatter.formatRelativeTime(now - 7 * dayMs))
        assertEquals("منذ أسبوعين", ArabicTimeFormatter.formatRelativeTime(now - 14 * dayMs))
        assertEquals("منذ 3 أسابيع", ArabicTimeFormatter.formatRelativeTime(now - 21 * dayMs))
    }

    @Test
    fun testRelativeTimeMonths() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600_000L
        assertEquals("منذ شهر", ArabicTimeFormatter.formatRelativeTime(now - 35 * dayMs))
        assertEquals("منذ شهرين", ArabicTimeFormatter.formatRelativeTime(now - 65 * dayMs))
        assertEquals("منذ 5 أشهر", ArabicTimeFormatter.formatRelativeTime(now - 150 * dayMs))
        assertEquals("منذ 11 شهراً", ArabicTimeFormatter.formatRelativeTime(now - 335 * dayMs))
    }

    @Test
    fun testRelativeTimeYears() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 3600_000L
        assertEquals("منذ سنة", ArabicTimeFormatter.formatRelativeTime(now - 370 * dayMs))
        assertEquals("منذ سنتين", ArabicTimeFormatter.formatRelativeTime(now - 750 * dayMs))
        assertEquals("منذ 4 سنوات", ArabicTimeFormatter.formatRelativeTime(now - 1500 * dayMs))
        assertEquals("منذ 12 سنة", ArabicTimeFormatter.formatRelativeTime(now - 4400 * dayMs))
    }

    @Test
    fun testEnglishRelativeStrings() {
        assertEquals("منذ دقيقة", ArabicTimeFormatter.formatRelativeTime(0L, "1 minute ago"))
        assertEquals("منذ دقيقتين", ArabicTimeFormatter.formatRelativeTime(0L, "2 minutes ago"))
        assertEquals("منذ 5 دقائق", ArabicTimeFormatter.formatRelativeTime(0L, "5 minutes ago"))
        assertEquals("منذ ساعة", ArabicTimeFormatter.formatRelativeTime(0L, "1 hour ago"))
        assertEquals("منذ ساعتين", ArabicTimeFormatter.formatRelativeTime(0L, "2 hours ago"))
        assertEquals("منذ 3 ساعات", ArabicTimeFormatter.formatRelativeTime(0L, "3 hours ago"))
        assertEquals("منذ 15 ساعة", ArabicTimeFormatter.formatRelativeTime(0L, "15 hours ago"))
        assertEquals("منذ يوم", ArabicTimeFormatter.formatRelativeTime(0L, "1 day ago"))
        assertEquals("منذ يومين", ArabicTimeFormatter.formatRelativeTime(0L, "2 days ago"))
        assertEquals("منذ 3 أيام", ArabicTimeFormatter.formatRelativeTime(0L, "3 days ago"))
        assertEquals("منذ 12 يوماً", ArabicTimeFormatter.formatRelativeTime(0L, "12 days ago"))
        assertEquals("منذ أسبوع", ArabicTimeFormatter.formatRelativeTime(0L, "1 week ago"))
        assertEquals("منذ أسبوعين", ArabicTimeFormatter.formatRelativeTime(0L, "2 weeks ago"))
        assertEquals("منذ 3 أسابيع", ArabicTimeFormatter.formatRelativeTime(0L, "3 weeks ago"))
        assertEquals("منذ 12 أسبوعاً", ArabicTimeFormatter.formatRelativeTime(0L, "12 weeks ago"))
        assertEquals("منذ شهر", ArabicTimeFormatter.formatRelativeTime(0L, "1 month ago"))
        assertEquals("منذ شهرين", ArabicTimeFormatter.formatRelativeTime(0L, "2 months ago"))
        assertEquals("منذ 6 أشهر", ArabicTimeFormatter.formatRelativeTime(0L, "6 months ago"))
        assertEquals("منذ 11 شهراً", ArabicTimeFormatter.formatRelativeTime(0L, "11 months ago"))
        assertEquals("منذ سنة", ArabicTimeFormatter.formatRelativeTime(0L, "1 year ago"))
        assertEquals("منذ سنتين", ArabicTimeFormatter.formatRelativeTime(0L, "2 years ago"))
        assertEquals("منذ 4 سنوات", ArabicTimeFormatter.formatRelativeTime(0L, "4 years ago"))
        assertEquals("منذ 15 سنة", ArabicTimeFormatter.formatRelativeTime(0L, "15 years ago"))
    }

    @Test
    fun testExistingArabicStringPreserved() {
        val arabicText = "منذ 3 أسابيع"
        assertEquals(arabicText, ArabicTimeFormatter.formatRelativeTime(0L, arabicText))
    }

    @Test
    fun testIso8601Parsing() {
        val twoDaysAgoIso = Instant.now().minusSeconds(2 * 24 * 3600L).toString()
        val result = ArabicTimeFormatter.formatRelativeTime(0L, twoDaysAgoIso)
        assertEquals("منذ يومين", result)
    }
}
