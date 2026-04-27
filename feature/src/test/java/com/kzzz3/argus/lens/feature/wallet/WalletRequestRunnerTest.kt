package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletRequestRunnerTest {
    @Test
    fun launchStateRequest_appliesResultForStillActiveSession() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runner = WalletRequestRunner(scope)
        val session = authenticatedSession("tester")
        var currentSession = session
        var walletState = WalletState(currentAccountId = "tester")

        runner.launchStateRequest(
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
        val runner = WalletRequestRunner(scope)
        val session = authenticatedSession("tester")
        var currentSession = session
        var walletState = WalletState(currentAccountId = "tester", statusMessage = "current")
        val releaseRequest = CompletableDeferred<Unit>()

        runner.launchStateRequest(
            requestSession = session,
            getCurrentSession = { currentSession },
            getCurrentState = { walletState },
            setState = { walletState = it },
        ) { state ->
            releaseRequest.await()
            state.copy(statusMessage = "stale")
        }
        runner.invalidate()
        releaseRequest.complete(Unit)

        delay(100)

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
