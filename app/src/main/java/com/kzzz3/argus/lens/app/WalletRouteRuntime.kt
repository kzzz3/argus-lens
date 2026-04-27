package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletRequestRunner
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class WalletRouteRequest(
    val session: AppSessionState,
    val currentState: WalletState,
)

internal data class WalletRouteCallbacks(
    val getCurrentSession: () -> AppSessionState,
    val getCurrentState: () -> WalletState,
    val onRouteChanged: (AppRoute) -> Unit,
    val onStateChanged: (WalletState) -> Unit,
)

internal class WalletRouteRuntime(
    private val requestRunner: WalletRequestRunner,
    private val loadWalletSummary: suspend (WalletState) -> WalletState,
    private val resolvePayload: suspend (WalletState, String) -> WalletState,
    private val confirmPayment: suspend (WalletState, String, String?, String) -> WalletState,
    private val loadPaymentHistory: suspend (WalletState) -> WalletState,
    private val loadPaymentReceipt: suspend (WalletState, String) -> WalletState,
) {
    fun handleEffect(
        effect: WalletEffect?,
        request: WalletRouteRequest,
        callbacks: WalletRouteCallbacks,
    ) {
        when (effect) {
            WalletEffect.NavigateBackToInbox -> callbacks.onRouteChanged(AppRoute.Inbox)
            WalletEffect.LoadWalletSummary -> launchRequest(request, callbacks, loadWalletSummary)
            is WalletEffect.ResolvePayload -> launchRequest(request, callbacks) { state ->
                resolvePayload(state, effect.payload)
            }
            is WalletEffect.ConfirmPayment -> launchRequest(request, callbacks) { state ->
                confirmPayment(
                    state,
                    effect.sessionId,
                    effect.amountInput,
                    effect.note,
                )
            }
            WalletEffect.LoadPaymentHistory -> launchRequest(request, callbacks, loadPaymentHistory)
            is WalletEffect.LoadPaymentReceipt -> launchRequest(request, callbacks) { state ->
                loadPaymentReceipt(state, effect.paymentId)
            }
            null -> Unit
        }
    }

    private fun launchRequest(
        request: WalletRouteRequest,
        callbacks: WalletRouteCallbacks,
        block: suspend (WalletState) -> WalletState,
    ) {
        var shouldUseRequestState = true
        requestRunner.launchStateRequest(
            requestSession = request.session,
            getCurrentSession = callbacks.getCurrentSession,
            getCurrentState = {
                if (shouldUseRequestState) {
                    shouldUseRequestState = false
                    request.currentState
                } else {
                    callbacks.getCurrentState()
                }
            },
            setState = callbacks.onStateChanged,
            block = block,
        )
    }
}
