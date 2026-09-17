package com.dr.tech.puretube.core.extractor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.schabi.newpipe.extractor.utils.Utils
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Regression test verifying that URLEncoder.encode and URLDecoder.decode
 * with StandardCharsets.UTF_8 can execute without NoSuchMethodError or LinkageError.
 */
class DesugarCompatibilityTest {

    @Test
    fun urlEncoder_withCharset_encodesCorrectly() {
        val input = "نقي PureTube 2026"
        val encoded = URLEncoder.encode(input, StandardCharsets.UTF_8)
        assertNotNull(encoded)

        val decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8)
        assertEquals(input, decoded)
    }

    @Test
    fun newPipeUtils_encodeAndDecodeUrlUtf8_executesWithoutError() {
        val input = "https://www.youtube.com/@قناة_نافعة"
        val encoded = Utils.encodeUrlUtf8(input)
        assertNotNull(encoded)

        val decoded = Utils.decodeUrlUtf8(encoded)
        assertEquals(input, decoded)
    }
}
