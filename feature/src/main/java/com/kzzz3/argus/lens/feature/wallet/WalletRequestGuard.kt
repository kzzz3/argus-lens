package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.model.session.AppSessionState

fun shouldApplyWalletRequestResult(
    currentSession: AppSessionState,
    requestAccountId: String,
    requestGeneration: Int,
    activeGeneration: Int,
): Boolean {
    return requestGeneration == activeGeneration &&
        currentSession.isAuthenticated &&
        requestAccountId.isNotBlank() &&
        currentSession.accountId == requestAccountId
}

inline fun applyWalletRequestResult(
    currentState: WalletState,
    isActive: Boolean,
    transform: (WalletState) -> WalletState,
): WalletState {
    return if (isActive) transform(currentState) else currentState
}
