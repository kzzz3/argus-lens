package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState

data class WalletFeatureRequest(
    val session: AppSessionState,
    val currentState: WalletState,
)

data class WalletFeatureCallbacks(
    val getCurrentSession: () -> AppSessionState,
    val getCurrentState: () -> WalletState,
    val onNavigateBackToInbox: () -> Unit,
    val onStateChanged: (WalletState) -> Unit,
)

class WalletFeatureController(
    private val reduceAction: (WalletState, WalletAction) -> WalletReducerResult,
    private val effectHandler: WalletEffectHandler,
) {
    fun handleAction(
        action: WalletAction,
        request: WalletFeatureRequest,
        callbacks: WalletFeatureCallbacks,
    ) {
        val actionHandler = WalletActionHandler(
            reduceAction = reduceAction,
            handleEffect = { effect, currentState ->
                effectHandler.handleEffect(
                    effect = effect,
                    request = WalletEffectRequest(
                        session = request.session,
                        currentState = currentState,
                    ),
                    callbacks = WalletEffectCallbacks(
                        getCurrentSession = callbacks.getCurrentSession,
                        getCurrentState = callbacks.getCurrentState,
                        onNavigateBackToInbox = callbacks.onNavigateBackToInbox,
                        onStateChanged = callbacks.onStateChanged,
                    ),
                )
            },
        )
        actionHandler.handleAction(
            action = action,
            request = WalletActionRequest(currentState = request.currentState),
            callbacks = WalletActionCallbacks(onWalletStateChanged = callbacks.onStateChanged),
        )
    }
}
