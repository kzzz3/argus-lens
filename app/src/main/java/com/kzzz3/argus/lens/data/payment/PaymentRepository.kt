package com.kzzz3.argus.lens.data.payment

data class WalletSummary(
    val accountId: String,
    val displayName: String,
    val balance: Double,
    val currency: String,
)

data class PaymentTransferResolution(
    val scanSessionId: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val currency: String,
    val requestedAmount: Double?,
    val amountEditable: Boolean,
    val requestedNote: String,
)

data class PaymentReceipt(
    val paymentId: String,
    val scanSessionId: String,
    val status: String,
    val payerAccountId: String,
    val payerDisplayName: String,
    val payerBalanceAfter: Double,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val recipientBalanceAfter: Double,
    val amount: Double,
    val currency: String,
    val note: String,
    val paidAt: String,
)

data class PaymentHistoryEntry(
    val paymentId: String,
    val payerAccountId: String,
    val payerDisplayName: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paidAt: String,
)

sealed interface PaymentRepositoryResult {
    data class WalletSummarySuccess(val summary: WalletSummary) : PaymentRepositoryResult
    data class ResolutionSuccess(val resolution: PaymentTransferResolution) : PaymentRepositoryResult
    data class ConfirmationSuccess(val receipt: PaymentReceipt) : PaymentRepositoryResult
    data class HistorySuccess(val history: List<PaymentHistoryEntry>) : PaymentRepositoryResult
    data class ReceiptSuccess(val receipt: PaymentReceipt) : PaymentRepositoryResult
    data class Failure(
        val code: String?,
        val message: String,
    ) : PaymentRepositoryResult
}

interface PaymentRepository {
    suspend fun getWalletSummary(): PaymentRepositoryResult

    suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult

    suspend fun confirmPayment(
        sessionId: String,
        amount: Double?,
        note: String,
    ): PaymentRepositoryResult

    suspend fun listPayments(): PaymentRepositoryResult

    suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult
}
