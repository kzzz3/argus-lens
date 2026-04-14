package com.kzzz3.argus.lens.feature.scan

sealed interface ScanEffect {
    data object NavigateBack : ScanEffect
    data class ResolvePayload(val payload: String) : ScanEffect
    data class ConfirmPayment(
        val sessionId: String,
        val amountInput: String?,
        val note: String,
    ) : ScanEffect
    data class OpenConversation(val conversationId: String) : ScanEffect
}
