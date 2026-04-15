package com.kzzz3.argus.lens.feature.wallet

sealed interface WalletEffect {
    data object NavigateBackToInbox : WalletEffect
    data object LoadWalletSummary : WalletEffect
    data class ResolvePayload(val payload: String) : WalletEffect
    data class ConfirmPayment(
        val sessionId: String,
        val amountInput: String?,
        val note: String,
    ) : WalletEffect
    data object LoadPaymentHistory : WalletEffect
    data class LoadPaymentReceipt(val paymentId: String) : WalletEffect
}
