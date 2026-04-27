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

class WalletEffectHandlerTest {
    @Test
    fun handleEffect_navigateBackToInboxInvokesCallback() {
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
        )
        var navigateBackCount = 0

        handler.handleEffect(
            effect = WalletEffect.NavigateBackToInbox,
            request = WalletEffectRequest(
                session = authenticatedSession("tester"),
                currentState = WalletState(currentAccountId = "tester"),
            ),
            callbacks = walletCallbacks(
                onNavigateBackToInbox = { navigateBackCount += 1 },
            ),
        )

        assertEquals(1, navigateBackCount)
    }

    @Test
    fun handleEffect_nullEffectDoesNothing() {
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(CoroutineScope(Dispatchers.Unconfined)),
        )
        var navigateBackCount = 0
        var stateChangeCount = 0

        handler.handleEffect(
            effect = null,
            request = WalletEffectRequest(
                session = authenticatedSession("tester"),
                currentState = WalletState(currentAccountId = "tester"),
            ),
            callbacks = walletCallbacks(
                onNavigateBackToInbox = { navigateBackCount += 1 },
                onStateChanged = { stateChangeCount += 1 },
            ),
        )

        assertEquals(0, navigateBackCount)
        assertEquals(0, stateChangeCount)
    }

    @Test
    fun handleEffect_loadWalletSummaryLaunchesRequest() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            loadWalletSummary = { state -> state.copy(statusMessage = "summary loaded") },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        handler.handleEffect(
            effect = WalletEffect.LoadWalletSummary,
            request = WalletEffectRequest(
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
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            loadWalletSummary = { state ->
                requestInputStatus = state.statusMessage
                state.copy(statusMessage = "summary loaded")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester", statusMessage = "old")

        handler.handleEffect(
            effect = WalletEffect.LoadWalletSummary,
            request = WalletEffectRequest(
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

    @Test
    fun handleEffect_resolvePayloadDispatchesRequestWithPayload() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var resolvedPayload: String? = null
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            resolvePayload = { state, payload ->
                resolvedPayload = payload
                state.copy(manualPayload = payload, statusMessage = "payload resolved")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        handler.handleEffect(
            effect = WalletEffect.ResolvePayload("qr-payload"),
            request = WalletEffectRequest(session = session, currentState = walletState),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals("qr-payload", resolvedPayload)
        assertEquals("qr-payload", walletState.manualPayload)
        assertEquals("payload resolved", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun handleEffect_confirmPaymentDispatchesRequestWithPaymentDetails() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var paymentDetails: PaymentDetails? = null
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            confirmPayment = { state, sessionId, amountInput, note ->
                paymentDetails = PaymentDetails(sessionId, amountInput, note)
                state.copy(statusMessage = "payment confirmed")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        handler.handleEffect(
            effect = WalletEffect.ConfirmPayment(
                sessionId = "scan-1",
                amountInput = "12.50",
                note = "Lunch",
            ),
            request = WalletEffectRequest(session = session, currentState = walletState),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals(PaymentDetails("scan-1", "12.50", "Lunch"), paymentDetails)
        assertEquals("payment confirmed", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun handleEffect_loadPaymentHistoryDispatchesRequest() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var historyRequestCount = 0
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            loadPaymentHistory = { state ->
                historyRequestCount += 1
                state.copy(statusMessage = "history loaded")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        handler.handleEffect(
            effect = WalletEffect.LoadPaymentHistory,
            request = WalletEffectRequest(session = session, currentState = walletState),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals(1, historyRequestCount)
        assertEquals("history loaded", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun handleEffect_loadPaymentReceiptDispatchesRequestWithPaymentId() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var requestedPaymentId: String? = null
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            loadPaymentReceipt = { state, paymentId ->
                requestedPaymentId = paymentId
                state.copy(statusMessage = "receipt loaded")
            },
        )
        val session = authenticatedSession("tester")
        var walletState = WalletState(currentAccountId = "tester")

        handler.handleEffect(
            effect = WalletEffect.LoadPaymentReceipt("payment-1"),
            request = WalletEffectRequest(session = session, currentState = walletState),
            callbacks = walletCallbacks(
                getCurrentSession = { session },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals("payment-1", requestedPaymentId)
        assertEquals("receipt loaded", walletState.statusMessage)
        scope.cancel()
    }

    @Test
    fun handleEffect_requestRunnerUsesRequestStateFirstAndCallbackStateAtCompletion() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val requestInput = CompletableDeferred<WalletState>()
        val releaseRequest = CompletableDeferred<Unit>()
        val handler = walletEffectHandler(
            requestRunner = WalletRequestRunner(scope),
            loadWalletSummary = { state ->
                requestInput.complete(state)
                releaseRequest.await()
                state.copy(statusMessage = "stale loaded")
            },
        )
        val requestSession = authenticatedSession("tester")
        var currentSession = requestSession
        var walletState = WalletState(currentAccountId = "tester", statusMessage = "old")

        handler.handleEffect(
            effect = WalletEffect.LoadWalletSummary,
            request = WalletEffectRequest(
                session = requestSession,
                currentState = walletState.copy(statusMessage = "loading"),
            ),
            callbacks = walletCallbacks(
                getCurrentSession = { currentSession },
                getCurrentState = { walletState },
                onStateChanged = { walletState = it },
            ),
        )

        assertEquals("loading", requestInput.await().statusMessage)
        walletState = walletState.copy(statusMessage = "current")
        currentSession = AppSessionState()
        releaseRequest.complete(Unit)
        delay(100)

        assertEquals("current", walletState.statusMessage)
        scope.cancel()
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

    private fun walletCallbacks(
        getCurrentSession: () -> AppSessionState = { authenticatedSession("tester") },
        getCurrentState: () -> WalletState = { WalletState(currentAccountId = "tester") },
        onNavigateBackToInbox: () -> Unit = {},
        onStateChanged: (WalletState) -> Unit = {},
    ): WalletEffectCallbacks {
        return WalletEffectCallbacks(
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

    private data class PaymentDetails(
        val sessionId: String,
        val amountInput: String?,
        val note: String,
    )
}
