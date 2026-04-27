package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState

data class WalletEffectRequest(
    val session: AppSessionState,
    val currentState: WalletState,
)

data class WalletEffectCallbacks(
    val getCurrentSession: () -> AppSessionState,
    val getCurrentState: () -> WalletState,
    val onNavigateBackToInbox: () -> Unit,
    val onStateChanged: (WalletState) -> Unit,
)

class WalletEffectHandler(
    private val requestRunner: WalletRequestRunner,
    private val loadWalletSummary: suspend (WalletState) -> WalletState,
    private val resolvePayload: suspend (WalletState, String) -> WalletState,
    private val confirmPayment: suspend (WalletState, String, String?, String) -> WalletState,
    private val loadPaymentHistory: suspend (WalletState) -> WalletState,
    private val loadPaymentReceipt: suspend (WalletState, String) -> WalletState,
) {
    fun handleEffect(
        effect: WalletEffect?,
        request: WalletEffectRequest,
        callbacks: WalletEffectCallbacks,
    ) {
        when (effect) {
            WalletEffect.NavigateBackToInbox -> callbacks.onNavigateBackToInbox()
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
        request: WalletEffectRequest,
        callbacks: WalletEffectCallbacks,
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
