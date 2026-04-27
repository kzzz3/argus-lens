package com.kzzz3.argus.lens.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaFileNameTest {
    @Test
    fun sanitizeMediaFileName_removesPathSegmentsAndUnsafeCharacters() {
        assertEquals("evil_invoice.png", sanitizeMediaFileName("../evil\\invoice.png", "attachment-1"))
    }

    @Test
    fun sanitizeMediaFileName_usesAttachmentFallbackForBlankNames() {
        assertEquals("attachment-attachment-1", sanitizeMediaFileName("   ", "attachment-1"))
    }

    @Test
    fun sanitizeMediaFileName_sanitizesAttachmentFallback() {
        assertEquals("attachment-evil_invoice.png", sanitizeMediaFileName("   ", "../evil/invoice.png"))
    }
}
