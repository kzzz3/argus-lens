package com.kzzz3.argus.lens.feature.wallet

data class WalletActionRequest(
    val currentState: WalletState,
)

data class WalletActionCallbacks(
    val onWalletStateChanged: (WalletState) -> Unit,
)

class WalletActionHandler(
    private val reduceAction: (WalletState, WalletAction) -> WalletReducerResult,
    private val handleEffect: (WalletEffect?, WalletState) -> Unit,
) {
    fun handleAction(
        action: WalletAction,
        request: WalletActionRequest,
        callbacks: WalletActionCallbacks,
    ) {
        val result = reduceAction(request.currentState, action)
        callbacks.onWalletStateChanged(result.state)
        handleEffect(result.effect, result.state)
    }
}
