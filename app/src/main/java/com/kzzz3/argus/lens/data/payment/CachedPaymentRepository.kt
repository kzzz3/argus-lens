package com.kzzz3.argus.lens.data.payment

import com.kzzz3.argus.lens.data.session.SessionRepository

class CachedPaymentRepository(
    private val remoteRepository: PaymentRepository,
    private val sessionRepository: SessionRepository,
    private val walletSummaryStore: LocalWalletSummaryStore,
    private val walletDetailsStore: LocalWalletDetailsStore,
) : PaymentRepository {
    override suspend fun getWalletSummary(): PaymentRepositoryResult {
        return when (val result = remoteRepository.getWalletSummary()) {
            is PaymentRepositoryResult.WalletSummarySuccess -> {
                walletSummaryStore.save(result.summary)
                result
            }
            is PaymentRepositoryResult.Failure -> {
                val currentAccountId = sessionRepository.loadSession().accountId
                val cached = walletSummaryStore.load(currentAccountId)
                if (cached != null && result.code == "NETWORK_UNAVAILABLE") {
                    PaymentRepositoryResult.WalletSummarySuccess(cached)
                } else {
                    result
                }
            }
            else -> result
        }
    }

    override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult {
        return remoteRepository.resolveScanPayload(scanPayload)
    }

    override suspend fun confirmPayment(sessionId: String, amount: Double?, note: String): PaymentRepositoryResult {
        return when (val result = remoteRepository.confirmPayment(sessionId, amount, note)) {
            is PaymentRepositoryResult.ConfirmationSuccess -> {
                cacheReceiptAndHistory(result.receipt, sessionRepository.loadSession().accountId)
                result
            }
            else -> result
        }
    }

    override suspend fun listPayments(): PaymentRepositoryResult {
        return when (val result = remoteRepository.listPayments()) {
            is PaymentRepositoryResult.HistorySuccess -> {
                val currentAccountId = sessionRepository.loadSession().accountId
                walletDetailsStore.saveHistory(currentAccountId, result.history)
                result
            }
            is PaymentRepositoryResult.Failure -> {
                val currentAccountId = sessionRepository.loadSession().accountId
                val cached = walletDetailsStore.loadHistory(currentAccountId)
                if (cached != null && result.code == "NETWORK_UNAVAILABLE") {
                    PaymentRepositoryResult.HistorySuccess(cached)
                } else {
                    result
                }
            }
            else -> result
        }
    }

    override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult {
        return when (val result = remoteRepository.getPaymentReceipt(paymentId)) {
            is PaymentRepositoryResult.ReceiptSuccess -> {
                walletDetailsStore.saveReceipt(result.receipt)
                result
            }
            is PaymentRepositoryResult.Failure -> {
                val cached = walletDetailsStore.loadReceipt(paymentId)
                if (cached != null && result.code == "NETWORK_UNAVAILABLE") {
                    PaymentRepositoryResult.ReceiptSuccess(cached)
                } else {
                    result
                }
            }
            else -> result
        }
    }

    private fun cacheReceiptAndHistory(receipt: PaymentReceipt, currentAccountId: String) {
        walletDetailsStore.saveReceipt(receipt)
        if (currentAccountId.isBlank()) return
        walletDetailsStore.upsertHistoryEntry(
            currentAccountId,
            PaymentHistoryEntry(
                paymentId = receipt.paymentId,
                payerAccountId = receipt.payerAccountId,
                payerDisplayName = receipt.payerDisplayName,
                recipientAccountId = receipt.recipientAccountId,
                recipientDisplayName = receipt.recipientDisplayName,
                amount = receipt.amount,
                currency = receipt.currency,
                status = receipt.status,
                paidAt = receipt.paidAt,
            ),
        )
    }
}
