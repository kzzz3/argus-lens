package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletEffectHandler
import com.kzzz3.argus.lens.feature.wallet.WalletRequestRunner
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WalletRouteRuntimeTest {
    @Test
    fun handleEffect_navigateBackToInboxRoutesToInbox() {
        val runtime = WalletRouteRuntime(
            effectHandler = walletEffectHandler(
                requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
            ),
        )
        var routedTo: AppRoute? = null

        runtime.handleEffect(
            effect = WalletEffect.NavigateBackToInbox,
            request = WalletRouteRequest(
                session = authenticatedSession("tester"),
                currentState = WalletState(currentAccountId = "tester"),
            ),
            callbacks = walletCallbacks(
                onRouteChanged = { routedTo = it },
            ),
        )

        assertEquals(AppRoute.Inbox, routedTo)
    }

    @Test
    fun handleEffect_loadWalletSummaryLaunchesRequest() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val runtime = WalletRouteRuntime(
            effectHandler = walletEffectHandler(
                requestRunner = WalletRequestRunner(scope),
                loadWalletSummary = { state -> state.copy(statusMessage = "summary loaded") },
            ),
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        runtime.handleEffect(
            effect = WalletEffect.LoadWalletSummary,
            request = WalletRouteRequest(
                session = session,
                currentState = walletState,
            ),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals("summary loaded", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun handleEffect_loadWalletSummaryUsesRequestStateAsRequestInput() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var requestInputStatus: String? = null
        val runtime = WalletRouteRuntime(
            effectHandler = walletEffectHandler(
                requestRunner = WalletRequestRunner(scope),
                loadWalletSummary = { state ->
                    requestInputStatus = state.statusMessage
                    state.copy(statusMessage = "summary loaded")
                },
            ),
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester", statusMessage = "old")

        runtime.handleEffect(
            effect = WalletEffect.LoadWalletSummary,
            request = WalletRouteRequest(
                session = session,
                currentState = walletState.copy(statusMessage = "loading"),
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

    private fun walletCallbacks(
        getCurrentSession: () -> AppSessionState = { authenticatedSession("tester") },
        getCurrentState: () -> WalletState = { WalletState(currentAccountId = "tester") },
        onRouteChanged: (AppRoute) -> Unit = {},
        onStateChanged: (WalletState) -> Unit = {},
    ): WalletRouteCallbacks {
        return WalletRouteCallbacks(
            getCurrentSession = getCurrentSession,
            getCurrentState = getCurrentState,
            onRouteChanged = onRouteChanged,
            onStateChanged = onStateChanged,
        )
    }

    private fun walletEffectHandler(
        requestRunner: WalletRequestRunner,
        loadWalletSummary: suspend (WalletState) -> WalletState = { state -> state },
        resolvePayload: suspend (WalletState, String) -> WalletState = { state, _ -> state },
        confirmPayment: suspend (WalletState, String, String?, String) -> WalletState = { state, _, _, _ -> state },
        loadPaymentHistory: suspend (WalletState) -> WalletState = { state -> state },
        loadPaymentReceipt: suspend (WalletState, String) -> WalletState = { state, _ -> state },
    ): WalletEffectHandler {
        return WalletEffectHandler(
            requestRunner = requestRunner,
            loadWalletSummary = loadWalletSummary,
            resolvePayload = resolvePayload,
            confirmPayment = confirmPayment,
            loadPaymentHistory = loadPaymentHistory,
            loadPaymentReceipt = loadPaymentReceipt,
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
