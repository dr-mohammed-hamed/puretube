package com.dr.tech.puretube.core.extractor.portability

import com.dr.tech.puretube.core.database.entity.SubscriptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ImportExportService verifying parsing accuracy for NewPipe JSON,
 * Google Takeout CSV, and JSON export.
 * Conforms to Constitution Principle III (Data Portability).
 */
class ImportExportServiceTest {

    private lateinit var service: ImportExportService

    @Before
    fun setUp() {
        service = ImportExportService()
    }

    @Test
    fun parseNewPipeJson_standardObjectFormat_parsesCorrectly() {
        val json = """
            {
              "app_version": "0.27.0",
              "app_version_int": 1000,
              "subscriptions": [
                {
                  "service_id": 0,
                  "url": "https://www.youtube.com/channel/UCq4qX1L1N1yS1rL8B4L0j8g",
                  "name": "القرآن الكريم"
                },
                {
                  "service_id": 0,
                  "url": "https://www.youtube.com/@DroosOnline4u",
                  "name": "دروس أونلاين"
                }
              ]
            }
        """.trimIndent()

        val result = service.parseNewPipeJson(json)
        assertTrue(result.isSuccess)

        val channels = result.getOrNull()
        assertNotNull(channels)
        assertEquals(2, requireNotNull(channels).size)

        assertEquals("UCq4qX1L1N1yS1rL8B4L0j8g", channels[0].channelId)
        assertEquals("القرآن الكريم", channels[0].channelName)

        assertEquals("@DroosOnline4u", channels[1].channelId)
        assertEquals("دروس أونلاين", channels[1].channelName)
    }

    @Test
    fun parseNewPipeJson_arrayFormat_parsesCorrectly() {
        val json = """
            [
              {
                "service_id": 0,
                "url": "https://www.youtube.com/channel/UCfiwzLy-8yKzIbsmZTzxDgw",
                "name": "الجزيرة الوثائقية"
              }
            ]
        """.trimIndent()

        val result = service.parseNewPipeJson(json)
        assertTrue(result.isSuccess)

        val channels = result.getOrNull()
        assertNotNull(channels)
        assertEquals(1, requireNotNull(channels).size)
        assertEquals("UCfiwzLy-8yKzIbsmZTzxDgw", channels[0].channelId)
        assertEquals("الجزيرة الوثائقية", channels[0].channelName)
    }

    @Test
    fun parseGoogleTakeoutCsv_validCsv_parsesCorrectly() {
        val csv = """
            Channel Id,Channel Url,Channel Title
            UCq4qX1L1N1yS1rL8B4L0j8g,http://www.youtube.com/channel/UCq4qX1L1N1yS1rL8B4L0j8g,"القرآن الكريم"
            UCrZdmsZhnvSmsMZZzT89aKg,http://www.youtube.com/channel/UCrZdmsZhnvSmsMZZzT89aKg,دروس أونلاين
        """.trimIndent()

        val result = service.parseGoogleTakeoutCsv(csv)
        assertTrue(result.isSuccess)

        val channels = result.getOrNull()
        assertNotNull(channels)
        assertEquals(2, requireNotNull(channels).size)

        assertEquals("UCq4qX1L1N1yS1rL8B4L0j8g", channels[0].channelId)
        assertEquals("القرآن الكريم", channels[0].channelName)
        assertEquals("UCrZdmsZhnvSmsMZZzT89aKg", channels[1].channelId)
        assertEquals("دروس أونلاين", channels[1].channelName)
    }

    @Test
    fun exportToNewPipeJson_generatesValidJson() {
        val subs = listOf(
            SubscriptionEntity(
                channelId = "UC1234567890",
                channelName = "قناة هادفة",
                channelHandle = "@Hadef",
                subscribedAtMs = 1700000000000L
            )
        )

        val exportedJson = service.exportToNewPipeJson(subs)
        assertTrue(exportedJson.contains("UC1234567890"))
        assertTrue(exportedJson.contains("قناة هادفة"))
        assertTrue(exportedJson.contains("PureTube-1.0"))

        // Re-parse exported JSON to verify round-trip integrity
        val reparsed = service.parseNewPipeJson(exportedJson)
        assertTrue(reparsed.isSuccess)
        assertEquals(1, reparsed.getOrNull()?.size)
        assertEquals("UC1234567890", reparsed.getOrNull()?.first()?.channelId)
    }

    @Test
    fun extractChannelIdFromUrl_handlesVariousFormats() {
        assertEquals("UC12345678901234567890", service.extractChannelIdFromUrl("UC12345678901234567890"))
        assertEquals("UC12345678901234567890", service.extractChannelIdFromUrl("https://www.youtube.com/channel/UC12345678901234567890"))
        assertEquals("@AliMuhammadAli", service.extractChannelIdFromUrl("https://www.youtube.com/@AliMuhammadAli"))
        assertEquals("@AliMuhammadAli", service.extractChannelIdFromUrl("@AliMuhammadAli"))
    }

    @Test
    fun exportToNewPipeJson_withChannelHandle_exportsHandleUrl() {
        val subs = listOf(
            SubscriptionEntity(
                channelId = "@DroosOnline4u",
                channelName = "دروس أونلاين",
                channelHandle = "@DroosOnline4u",
                subscribedAtMs = 1700000000000L
            ),
            SubscriptionEntity(
                channelId = "UCq4qX1L1N1yS1rL8B4L0j8g",
                channelName = "القرآن الكريم",
                subscribedAtMs = 1700000000001L
            )
        )

        val exportedJson = service.exportToNewPipeJson(subs)
        assertTrue(exportedJson.contains("https://www.youtube.com/@DroosOnline4u"))
        assertTrue(exportedJson.contains("https://www.youtube.com/channel/UCq4qX1L1N1yS1rL8B4L0j8g"))

        // Verify round-trip parsing produces expected channel IDs
        val reparsed = service.parseNewPipeJson(exportedJson)
        assertTrue(reparsed.isSuccess)
        val channels = reparsed.getOrNull()
        assertEquals(2, channels?.size)
        assertEquals("@DroosOnline4u", channels?.get(0)?.channelId)
        assertEquals("UCq4qX1L1N1yS1rL8B4L0j8g", channels?.get(1)?.channelId)
    }

    @Test
    fun parseGoogleTakeoutCsv_withEscapedQuotes_parsesCorrectly() {
        val csv = """
            Channel Id,Channel Url,Channel Title
            UC123,http://www.youtube.com/channel/UC123,"قناة ""النور"" التعليمية"
        """.trimIndent()

        val result = service.parseGoogleTakeoutCsv(csv)
        assertTrue(result.isSuccess)

        val channels = result.getOrNull()
        assertEquals(1, channels?.size)
        assertEquals("قناة \"النور\" التعليمية", channels?.first()?.channelName)
    }
}
