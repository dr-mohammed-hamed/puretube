package com.dr.tech.puretube.features.player

import com.dr.tech.puretube.features.player.sheets.SleepTimerOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SleepTimerTest {

    @Test
    fun testSleepTimerOptionValues() {
        assertEquals(null, SleepTimerOption.OFF.durationMinutes)
        assertEquals(15, SleepTimerOption.MIN_15.durationMinutes)
        assertEquals(30, SleepTimerOption.MIN_30.durationMinutes)
        assertEquals(45, SleepTimerOption.MIN_45.durationMinutes)
        assertEquals(60, SleepTimerOption.MIN_60.durationMinutes)
        assertEquals(0, SleepTimerOption.END_OF_VIDEO.durationMinutes)
    }

    @Test
    fun testSleepTimerOptionLabelsAreNonEmptyArabic() {
        SleepTimerOption.entries.forEach { option ->
            org.junit.Assert.assertTrue(option.labelArabic.isNotBlank())
        }
    }
}
