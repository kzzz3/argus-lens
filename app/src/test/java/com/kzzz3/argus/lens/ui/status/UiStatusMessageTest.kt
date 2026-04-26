package com.kzzz3.argus.lens.ui.status

import org.junit.Assert.assertEquals
import org.junit.Test

class UiStatusMessageTest {

    @Test
    fun successCreatesNonErrorMessage() {
        val message = UiStatusMessage.success("Synced")

        assertEquals("Synced", message.text)
        assertEquals(false, message.isError)
    }

    @Test
    fun errorCreatesErrorMessage() {
        val message = UiStatusMessage.error("Network unavailable")

        assertEquals("Network unavailable", message.text)
        assertEquals(true, message.isError)
    }
}
