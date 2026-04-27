package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WalletStateHolder(
    initialState: WalletState = WalletState(),
    private val controller: WalletFeatureController,
    private val invalidateRequests: () -> Unit,
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<WalletState> = mutableState.asStateFlow()

    fun handleAction(
        action: WalletAction,
        session: AppSessionState,
        getCurrentSession: () -> AppSessionState,
        onNavigateBackToInbox: () -> Unit,
    ) {
        controller.handleAction(
            action = action,
            request = WalletFeatureRequest(
                session = session,
                currentState = mutableState.value,
            ),
            callbacks = WalletFeatureCallbacks(
                getCurrentSession = getCurrentSession,
                getCurrentState = { mutableState.value },
                onNavigateBackToInbox = onNavigateBackToInbox,
                onStateChanged = ::replaceState,
            ),
        )
    }

    fun openForAccount(accountId: String) {
        mutableState.update { state -> state.withCurrentAccount(accountId) }
    }

    fun invalidate() {
        invalidateRequests()
    }

    fun replaceState(state: WalletState) {
        mutableState.value = state
    }
}
