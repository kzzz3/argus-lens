package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRequestGuardTest {
    @Test
    fun acceptsMatchingAuthenticatedRequest() {
        val result = shouldApplyWalletRequestResult(
            currentSession = authenticatedSession("tester"),
            requestAccountId = "tester",
            requestGeneration = 3,
            activeGeneration = 3,
        )

        assertTrue(result)
    }

    @Test
    fun rejectsWhenGenerationHasMoved() {
        val result = shouldApplyWalletRequestResult(
            currentSession = authenticatedSession("tester"),
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
            currentSession = authenticatedSession("lisi"),
            requestAccountId = "tester",
            requestGeneration = 2,
            activeGeneration = 2,
        )

        assertFalse(signedOut)
        assertFalse(switchedAccount)
    }

    @Test
    fun applyWalletRequestResult_updatesStateOnlyWhenRequestIsActive() {
        val currentState = WalletState(statusMessage = "Current")
        val activeState = applyWalletRequestResult(
            currentState = currentState,
            isActive = true,
        ) {
            it.copy(statusMessage = "Updated")
        }
        val staleState = applyWalletRequestResult(
            currentState = currentState,
            isActive = false,
        ) {
            it.copy(statusMessage = "Updated")
        }

        assertEquals("Updated", activeState.statusMessage)
        assertEquals("Current", staleState.statusMessage)
    }

    private fun authenticatedSession(accountId: String): AppSessionState {
        return AppSessionState(
            isAuthenticated = true,
            accountId = accountId,
            displayName = accountId,
        )
    }
}
