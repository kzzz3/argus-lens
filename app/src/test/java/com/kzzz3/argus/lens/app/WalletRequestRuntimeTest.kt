package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletRequestRuntimeTest {
    @Test
    fun launchStateRequest_appliesResultForStillActiveSession() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runtime = WalletRequestRuntime(scope)
        val session = authenticatedSession("tester")
        var currentSession = session
        var walletState = WalletState(currentAccountId = "tester")

        runtime.launchStateRequest(
            requestSession = session,
            getCurrentSession = { currentSession },
            getCurrentState = { walletState },
            setState = { walletState = it },
        ) { state ->
            state.copy(statusMessage = "loaded")
        }

        assertEquals("loaded", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun invalidatePreventsStaleResultFromReplacingCurrentState() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val runtime = WalletRequestRuntime(scope)
        val session = authenticatedSession("tester")
        var currentSession = session
        var walletState = WalletState(currentAccountId = "tester", statusMessage = "current")
        val releaseRequest = CompletableDeferred<Unit>()

        runtime.launchStateRequest(
            requestSession = session,
            getCurrentSession = { currentSession },
            getCurrentState = { walletState },
            setState = { walletState = it },
        ) { state ->
            releaseRequest.await()
            state.copy(statusMessage = "stale")
        }
        runtime.invalidate()
        releaseRequest.complete(Unit)

        kotlinx.coroutines.delay(100)

        assertEquals("current", walletState.statusMessage)
        currentSession = AppSessionState()
        scope.cancel()
    }

    private fun authenticatedSession(accountId: String): AppSessionState {
        return AppSessionState(
            isAuthenticated = true,
            accountId = accountId,
            displayName = accountId,
        )
    }
}
