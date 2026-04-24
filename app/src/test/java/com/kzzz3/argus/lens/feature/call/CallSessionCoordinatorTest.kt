package com.kzzz3.argus.lens.feature.call

import org.junit.Assert.assertEquals
import org.junit.Test

class CallSessionCoordinatorTest {
    @Test
    fun incrementDurationLabel_incrementsSecondsAndMinutes() {
        assertEquals("00:01", incrementDurationLabel("00:00"))
        assertEquals("01:00", incrementDurationLabel("00:59"))
    }

    @Test
    fun incrementDurationLabel_fallsBackForInvalidInput() {
        assertEquals("00:01", incrementDurationLabel("invalid"))
    }

    @Test
    fun activateConnectingCallSession_transitionsOnlyConnectingState() {
        assertEquals(CallSessionStatus.Active, activateConnectingCallSession(CallSessionState(status = CallSessionStatus.Connecting)).status)
        assertEquals(CallSessionStatus.Ended, activateConnectingCallSession(CallSessionState(status = CallSessionStatus.Ended)).status)
    }
}
