package com.kzzz3.argus.lens.core.data.payment

import com.kzzz3.argus.lens.model.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.model.payment.PaymentReceipt
import com.kzzz3.argus.lens.model.payment.PaymentTransferResolution
import com.kzzz3.argus.lens.model.payment.WalletSummary

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

    fun clearLocalData(accountId: String) {}
}
