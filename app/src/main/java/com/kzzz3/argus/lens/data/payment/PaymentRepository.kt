package com.kzzz3.argus.lens.data.payment

data class PaymentScanResolution(
    val scanSessionId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val currency: String,
    val suggestedAmount: Double?,
    val amountEditable: Boolean,
    val suggestedNote: String,
)

data class PaymentConfirmation(
    val paymentId: String,
    val status: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val conversationId: String,
    val amount: Double,
    val currency: String,
    val note: String,
    val paidAt: String,
)

sealed interface PaymentRepositoryResult {
    data class ResolutionSuccess(val resolution: PaymentScanResolution) : PaymentRepositoryResult
    data class ConfirmationSuccess(val confirmation: PaymentConfirmation) : PaymentRepositoryResult
    data class Failure(
        val code: String?,
        val message: String,
    ) : PaymentRepositoryResult
}

interface PaymentRepository {
    suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult

    suspend fun confirmPayment(
        sessionId: String,
        amount: Double?,
        note: String,
    ): PaymentRepositoryResult
}
