package com.kzzz3.argus.lens.feature.register

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterStateFactoryTest {

    @Test
    fun createRegisterUiState_doesNotUseStageLabelInSubtitle() {
        val state = createRegisterUiState(RegisterFormState())

        assertFalse(state.subtitle.contains("stage-1", ignoreCase = true))
    }

    @Test
    fun createRegisterUiState_describesRealRegistrationFlow() {
        val state = createRegisterUiState(RegisterFormState())

        assertTrue(state.subtitle.contains("Create", ignoreCase = true))
        assertTrue(state.subtitle.contains("account", ignoreCase = true))
    }
}
