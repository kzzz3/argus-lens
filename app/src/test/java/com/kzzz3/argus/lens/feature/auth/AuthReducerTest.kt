package com.kzzz3.argus.lens.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthReducerTest {

    @Test
    fun submitPasswordLogin_withValidForm_requestsNetworkSubmission() {
        val state = AuthFormState(
            account = "tester",
            password = "secret123",
        )

        val result = reduceAuthFormState(state, AuthEntryAction.SubmitPasswordLogin)

        assertTrue(result.formState.isSubmitting)
        assertEquals(
            AuthEntryEffect.SubmitPasswordLogin(account = "tester", password = "secret123"),
            result.effect,
        )
    }

    @Test
    fun submitPasswordLogin_withInvalidForm_doesNotRequestNetworkSubmission() {
        val state = AuthFormState(
            account = "abc",
            password = "123",
        )

        val result = reduceAuthFormState(state, AuthEntryAction.SubmitPasswordLogin)

        assertFalse(result.formState.isSubmitting)
        assertEquals(null, result.effect)
    }
}
