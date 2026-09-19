package com.dr.tech.puretube.features.player

import com.dr.tech.puretube.features.player.util.TimestampParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimestampParserTest {

    @Test
    fun testParseStandardTimestamp() {
        val millis1 = TimestampParser.parseTimestampToMillis("01:30")
        assertEquals(90_000L, millis1)

        val millis2 = TimestampParser.parseTimestampToMillis("01:15:20")
        assertEquals((1 * 3600 + 15 * 60 + 20) * 1000L, millis2)
    }

    @Test
    fun testParseArabicIndicNumerals() {
        val millis = TimestampParser.parseTimestampToMillis("٠٣:٤٥")
        assertEquals((3 * 60 + 45) * 1000L, millis)

        val millisHour = TimestampParser.parseTimestampToMillis("٠١:١٠:٠٥")
        assertEquals((1 * 3600 + 10 * 60 + 5) * 1000L, millisHour)
    }

    @Test
    fun testInvalidTimestamps() {
        assertNull(TimestampParser.parseTimestampToMillis("invalid"))
        assertNull(TimestampParser.parseTimestampToMillis("12:99"))
        assertNull(TimestampParser.parseTimestampToMillis("01:75:30"))
    }

    @Test
    fun testExtractChaptersFromDescription() {
        val sampleDescription = """
            مرحباً بكم في هذا الدرس الشامل:
            00:00 المقدمة والترحيب
            02:30 شرح الفكرة الأساسية
            15:45 التطبيق العملي
            ٠١:٠٥:٠٠ خاتمة وتوصيات
        """.trimIndent()

        val chapters = TimestampParser.extractChapters(sampleDescription, durationMs = 4_000_000L)
        assertEquals(4, chapters.size)

        assertEquals("المقدمة والترحيب", chapters[0].title)
        assertEquals(0L, chapters[0].timestampMs)

        assertEquals("شرح الفكرة الأساسية", chapters[1].title)
        assertEquals(150_000L, chapters[1].timestampMs)

        assertEquals("التطبيق العملي", chapters[2].title)
        assertEquals(945_000L, chapters[2].timestampMs)

        assertEquals("خاتمة وتوصيات", chapters[3].title)
        assertEquals(3_900_000L, chapters[3].timestampMs)
    }

    @Test
    fun testChaptersFilteredByDuration() {
        val description = "05:00 نقطة متأخرة\n50:00 بعد نهاية الفيديو"
        val chapters = TimestampParser.extractChapters(description, durationMs = 600_000L) // 10 mins

        assertEquals(1, chapters.size)
        assertEquals(300_000L, chapters[0].timestampMs)
    }
}
