package com.kzzz3.argus.lens.feature.call

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallSessionStateFactoryTest {

    @Test
    fun createCallSessionUiState_doesNotUseStageOrShellSubtitle() {
        val state = createCallSessionUiState(
            CallSessionState(contactDisplayName = "Argus Tester"),
        )

        assertFalse(state.subtitle.contains("stage", ignoreCase = true))
        assertFalse(state.subtitle.contains("shell", ignoreCase = true))
    }

    @Test
    fun createCallSessionUiState_describesCallAvailability() {
        val state = createCallSessionUiState(
            CallSessionState(contactDisplayName = "Argus Tester"),
        )

        assertTrue(state.subtitle.contains("call", ignoreCase = true))
    }
}
