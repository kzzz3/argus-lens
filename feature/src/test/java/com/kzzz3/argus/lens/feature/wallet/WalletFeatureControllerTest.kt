package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletFeatureControllerTest {
    @Test
    fun handleAction_publishesReducedStateBeforeDispatchingEffect() {
        val currentState = WalletState(currentAccountId = "tester")
        val reducedState = currentState.copy(statusMessage = "leaving")
        val controller = walletFeatureController(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
            reduceAction = { state, action ->
                assertEquals(currentState, state)
                assertEquals(WalletAction.NavigateBack, action)
                WalletReducerResult(reducedState, WalletEffect.NavigateBackToInbox)
            },
        )
        val events = mutableListOf<String>()

        controller.handleAction(
            action = WalletAction.NavigateBack,
            request = WalletFeatureRequest(
                session = authenticatedSession("tester"),
                currentState = currentState,
            ),
            callbacks = walletCallbacks(
                getCurrentState = { reducedState },
                onStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
                onNavigateBackToInbox = { events += "navigate" },
            ),
        )

        assertEquals(listOf("state", "navigate"), events)
    }

    @Test
    fun handleAction_dispatchesRequestEffectWithReducedStateAsRequestInput() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val currentState = WalletState(currentAccountId = "tester", statusMessage = "old")
        val reducedState = currentState.copy(statusMessage = "loading")
        var requestInputStatus: String? = null
        val controller = walletFeatureController(
            requestRunner = WalletRequestRunner(scope),
            reduceAction = { _, _ -> WalletReducerResult(reducedState, WalletEffect.LoadWalletSummary) },
            loadWalletSummary = { state ->
                requestInputStatus = state.statusMessage
                state.copy(statusMessage = "summary loaded")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = currentState

        controller.handleAction(
            action = WalletAction.RefreshWalletSummary,
            request = WalletFeatureRequest(
                session = session,
                currentState = walletState,
            ),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals("loading", requestInputStatus)
        assertEquals("summary loaded", walletState.statusMessage)
        scope.cancel()
    }

    private fun walletFeatureController(
        requestRunner: WalletRequestRunner,
        reduceAction: (WalletState, WalletAction) -> WalletReducerResult,
        loadWalletSummary: suspend (WalletState) -> WalletState = { state -> state },
        resolvePayload: suspend (WalletState, String) -> WalletState = { state, _ -> state },
        confirmPayment: suspend (WalletState, String, String?, String) -> WalletState = { state, _, _, _ -> state },
        loadPaymentHistory: suspend (WalletState) -> WalletState = { state -> state },
        loadPaymentReceipt: suspend (WalletState, String) -> WalletState = { state, _ -> state },
    ): WalletFeatureController {
        return WalletFeatureController(
            reduceAction = reduceAction,
            effectHandler = WalletEffectHandler(
                requestRunner = requestRunner,
                loadWalletSummary = loadWalletSummary,
                resolvePayload = resolvePayload,
                confirmPayment = confirmPayment,
                loadPaymentHistory = loadPaymentHistory,
                loadPaymentReceipt = loadPaymentReceipt,
            ),
        )
    }

    private fun walletCallbacks(
        getCurrentSession: () -> AppSessionState = { authenticatedSession("tester") },
        getCurrentState: () -> WalletState = { WalletState(currentAccountId = "tester") },
        onNavigateBackToInbox: () -> Unit = {},
        onStateChanged: (WalletState) -> Unit = {},
    ): WalletFeatureCallbacks {
        return WalletFeatureCallbacks(
            getCurrentSession = getCurrentSession,
            getCurrentState = getCurrentState,
            onNavigateBackToInbox = onNavigateBackToInbox,
            onStateChanged = onStateChanged,
        )
    }

    private fun authenticatedSession(accountId: String): AppSessionState {
        return AppSessionState(
            isAuthenticated = true,
            accountId = accountId,
            displayName = accountId,
        )
    }
}
