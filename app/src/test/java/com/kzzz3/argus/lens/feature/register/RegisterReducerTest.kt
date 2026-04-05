package com.kzzz3.argus.lens.feature.register

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterReducerTest {

    @Test
    fun submitRegistration_withValidForm_requestsNetworkSubmission() {
        val state = RegisterFormState(
            displayName = "Argus Tester",
            account = "tester",
            password = "secret123",
            confirmPassword = "secret123",
        )

        val result = reduceRegisterFormState(state, RegisterAction.SubmitRegistration)

        assertTrue(result.formState.isSubmitting)
        assertEquals(
            RegisterEffect.SubmitRegistration(
                displayName = "Argus Tester",
                account = "tester",
                password = "secret123",
            ),
            result.effect,
        )
    }

    @Test
    fun submitRegistration_withInvalidForm_doesNotRequestNetworkSubmission() {
        val state = RegisterFormState(
            displayName = "",
            account = "abc",
            password = "123",
            confirmPassword = "321",
        )

        val result = reduceRegisterFormState(state, RegisterAction.SubmitRegistration)

        assertFalse(result.formState.isSubmitting)
        assertEquals(null, result.effect)
    }
}
