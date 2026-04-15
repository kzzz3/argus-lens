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

data class PaymentReceipt(
    val paymentId: String,
    val scanSessionId: String,
    val status: String,
    val payerAccountId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val amount: Double,
    val currency: String,
    val note: String,
    val paidAt: String,
)

data class PaymentHistoryEntry(
    val paymentId: String,
    val merchantDisplayName: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paidAt: String,
)

sealed interface PaymentRepositoryResult {
    data class ResolutionSuccess(val resolution: PaymentScanResolution) : PaymentRepositoryResult
    data class ConfirmationSuccess(val receipt: PaymentReceipt) : PaymentRepositoryResult
    data class HistorySuccess(val history: List<PaymentHistoryEntry>) : PaymentRepositoryResult
    data class ReceiptSuccess(val receipt: PaymentReceipt) : PaymentRepositoryResult
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

    suspend fun listPayments(): PaymentRepositoryResult

    suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult
}
