package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusLensWalletRequestGuardTest {

    @Test
    fun acceptsMatchingAuthenticatedRequest() {
        val result = shouldApplyWalletRequestResult(
            currentSession = AppSessionState(
                isAuthenticated = true,
                accountId = "tester",
                displayName = "Argus Tester",
                accessToken = "access",
                refreshToken = "refresh",
            ),
            requestAccountId = "tester",
            requestGeneration = 3,
            activeGeneration = 3,
        )

        assertTrue(result)
    }

    @Test
    fun rejectsWhenGenerationHasMoved() {
        val result = shouldApplyWalletRequestResult(
            currentSession = AppSessionState(
                isAuthenticated = true,
                accountId = "tester",
                displayName = "Argus Tester",
                accessToken = "access",
                refreshToken = "refresh",
            ),
            requestAccountId = "tester",
            requestGeneration = 3,
            activeGeneration = 4,
        )

        assertFalse(result)
    }

    @Test
    fun rejectsWhenSessionHasChangedOrSignedOut() {
        val signedOut = shouldApplyWalletRequestResult(
            currentSession = AppSessionState(),
            requestAccountId = "tester",
            requestGeneration = 2,
            activeGeneration = 2,
        )
        val switchedAccount = shouldApplyWalletRequestResult(
            currentSession = AppSessionState(
                isAuthenticated = true,
                accountId = "lisi",
                displayName = "Li Si",
                accessToken = "access",
                refreshToken = "refresh",
            ),
            requestAccountId = "tester",
            requestGeneration = 2,
            activeGeneration = 2,
        )

        assertFalse(signedOut)
        assertFalse(switchedAccount)
    }
}
