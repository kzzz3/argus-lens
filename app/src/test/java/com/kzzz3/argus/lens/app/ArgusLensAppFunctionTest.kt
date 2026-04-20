package com.kzzz3.argus.lens.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusLensAppFunctionTest {

    @Test
    fun incrementCallDurationLabel_incrementsSecondsAndMinutes() {
        assertEquals("00:01", incrementCallDurationLabel("00:00"))
        assertEquals("01:00", incrementCallDurationLabel("00:59"))
    }

    @Test
    fun incrementCallDurationLabel_fallsBackForInvalidInput() {
        assertEquals("00:01", incrementCallDurationLabel("invalid"))
    }

    @Test
    fun isSseAuthFailure_detectsNestedUnauthorizedErrors() {
        val throwable = IllegalStateException(
            "stream failed",
            RuntimeException("HTTP 401 from upstream"),
        )

        assertTrue(isSseAuthFailure(throwable))
    }

    @Test
    fun isSseAuthFailure_ignoresOtherFailures() {
        assertFalse(isSseAuthFailure(IllegalStateException("socket closed")))
    }
}
