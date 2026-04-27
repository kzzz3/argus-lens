package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.feature.wallet.WalletEffectCallbacks
import com.kzzz3.argus.lens.feature.wallet.WalletEffectHandler
import com.kzzz3.argus.lens.feature.wallet.WalletEffectRequest
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
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
    private val effectHandler: WalletEffectHandler,
) {
    fun handleEffect(
        effect: WalletEffect?,
        request: WalletRouteRequest,
        callbacks: WalletRouteCallbacks,
    ) {
        effectHandler.handleEffect(
            effect = effect,
            request = WalletEffectRequest(
                session = request.session,
                currentState = request.currentState,
            ),
            callbacks = WalletEffectCallbacks(
            getCurrentSession = callbacks.getCurrentSession,
                getCurrentState = callbacks.getCurrentState,
                onNavigateBackToInbox = { callbacks.onRouteChanged(AppRoute.Inbox) },
                onStateChanged = callbacks.onStateChanged,
            ),
        )
    }
}
