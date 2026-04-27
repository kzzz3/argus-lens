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

class WalletStateHolderTest {
    @Test
    fun handleAction_updatesStateBeforeDispatchingEffect() {
        val reducedState = WalletState(currentAccountId = "tester", statusMessage = "leaving")
        val holder = walletStateHolder(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
            reduceAction = { _, action ->
                assertEquals(WalletAction.NavigateBack, action)
                WalletReducerResult(reducedState, WalletEffect.NavigateBackToInbox)
            },
        )
        val events = mutableListOf<String>()

        holder.handleAction(
            action = WalletAction.NavigateBack,
            session = authenticatedSession("tester"),
            getCurrentSession = { authenticatedSession("tester") },
            onNavigateBackToInbox = { events += "navigate:${holder.state.value.statusMessage}" },
        )

        assertEquals(reducedState, holder.state.value)
        assertEquals(listOf("navigate:leaving"), events)
    }

    @Test
    fun handleAction_usesLatestStateForSubsequentActions() {
        val holder = walletStateHolder(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
            reduceAction = ::reduceWalletState,
        )

        holder.handleAction(
            action = WalletAction.UpdateManualPayload("payload-1"),
            session = authenticatedSession("tester"),
            getCurrentSession = { authenticatedSession("tester") },
            onNavigateBackToInbox = {},
        )
        holder.handleAction(
            action = WalletAction.SubmitManualPayload,
            session = authenticatedSession("tester"),
            getCurrentSession = { authenticatedSession("tester") },
            onNavigateBackToInbox = {},
        )

        assertEquals("payload-1", holder.state.value.manualPayload)
        assertEquals("payload-1", holder.state.value.activePayload)
        assertEquals(true, holder.state.value.isResolving)
    }

    @Test
    fun openForAccount_setsCurrentAccountAndAllowsSummaryReload() {
        val holder = walletStateHolder(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
            reduceAction = ::reduceWalletState,
            initialState = WalletState(
                currentAccountId = "account-a",
                hasAttemptedSummaryLoad = true,
            ),
        )

        holder.openForAccount("account-b")

        assertEquals("account-b", holder.state.value.currentAccountId)
        assertEquals(false, holder.state.value.hasAttemptedSummaryLoad)
    }

    @Test
    fun invalidatePreventsStaleWalletRequestFromReplacingCurrentState() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val releaseRequest = CompletableDeferred<Unit>()
        val holder = walletStateHolder(
            requestRunner = WalletRequestRunner(scope),
            reduceAction = { state, _ -> WalletReducerResult(state.withWalletSummaryLoading(), WalletEffect.LoadWalletSummary) },
            loadWalletSummary = { state ->
                releaseRequest.await()
                state.copy(statusMessage = "stale")
            },
            initialState = WalletState(currentAccountId = "tester", statusMessage = "current"),
        )

        holder.handleAction(
            action = WalletAction.RefreshWalletSummary,
            session = authenticatedSession("tester"),
            getCurrentSession = { authenticatedSession("tester") },
            onNavigateBackToInbox = {},
        )
        holder.invalidate()
        holder.replaceState(holder.state.value.copy(statusMessage = "current"))
        releaseRequest.complete(Unit)
        delay(100)

        assertEquals("current", holder.state.value.statusMessage)
        scope.cancel()
    }

    private fun walletStateHolder(
        requestRunner: WalletRequestRunner,
        reduceAction: (WalletState, WalletAction) -> WalletReducerResult,
        initialState: WalletState = WalletState(),
        loadWalletSummary: suspend (WalletState) -> WalletState = { state -> state },
        resolvePayload: suspend (WalletState, String) -> WalletState = { state, _ -> state },
        confirmPayment: suspend (WalletState, String, String?, String) -> WalletState = { state, _, _, _ -> state },
        loadPaymentHistory: suspend (WalletState) -> WalletState = { state -> state },
        loadPaymentReceipt: suspend (WalletState, String) -> WalletState = { state, _ -> state },
    ): WalletStateHolder {
        val effectHandler = WalletEffectHandler(
            requestRunner = requestRunner,
            loadWalletSummary = loadWalletSummary,
            resolvePayload = resolvePayload,
            confirmPayment = confirmPayment,
            loadPaymentHistory = loadPaymentHistory,
            loadPaymentReceipt = loadPaymentReceipt,
        )
        return WalletStateHolder(
            initialState = initialState,
            controller = WalletFeatureController(
                reduceAction = reduceAction,
                effectHandler = effectHandler,
            ),
            invalidateRequests = requestRunner::invalidate,
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
