package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletReducerResult
import com.kzzz3.argus.lens.feature.wallet.WalletState

internal data class WalletActionRouteRequest(
    val currentState: WalletState,
)

internal data class WalletActionRouteCallbacks(
    val onWalletStateChanged: (WalletState) -> Unit,
)

internal class WalletActionRouteRuntime(
    private val reduceAction: (WalletState, WalletAction) -> WalletReducerResult,
    private val handleEffect: (WalletEffect?, WalletState) -> Unit,
) {
    fun handleAction(
        action: WalletAction,
        request: WalletActionRouteRequest,
        callbacks: WalletActionRouteCallbacks,
    ) {
        val result = reduceAction(request.currentState, action)
        callbacks.onWalletStateChanged(result.state)
        handleEffect(result.effect, result.state)
    }
}
