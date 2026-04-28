package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.withConfirmFailure
import com.kzzz3.argus.lens.feature.wallet.withConfirmedPayment
import com.kzzz3.argus.lens.feature.wallet.withHistoryFailure
import com.kzzz3.argus.lens.feature.wallet.withHistoryLoaded
import com.kzzz3.argus.lens.feature.wallet.withReceiptFailure
import com.kzzz3.argus.lens.feature.wallet.withReceiptLoaded
import com.kzzz3.argus.lens.feature.wallet.withResolveFailure
import com.kzzz3.argus.lens.feature.wallet.withResolvedPayment
import com.kzzz3.argus.lens.feature.wallet.withWalletSummaryFailure
import com.kzzz3.argus.lens.feature.wallet.withWalletSummaryLoaded

class WalletUseCases(
    private val paymentRepository: PaymentRepository,
) {
    suspend fun loadWalletSummary(currentState: WalletState): WalletState {
        return when (val paymentResult = paymentRepository.getWalletSummary()) {
            is PaymentRepositoryResult.WalletSummarySuccess -> currentState.withWalletSummaryLoaded(paymentResult.summary)
            is PaymentRepositoryResult.Failure -> currentState.withWalletSummaryFailure(paymentResult.message)
            else -> currentState
        }
    }

    suspend fun resolvePayload(currentState: WalletState, payload: String): WalletState {
        return when (val paymentResult = paymentRepository.resolveScanPayload(payload)) {
            is PaymentRepositoryResult.ResolutionSuccess -> currentState.withResolvedPayment(paymentResult.resolution)
            is PaymentRepositoryResult.Failure -> currentState.withResolveFailure(paymentResult.message)
            else -> currentState
        }
    }

    suspend fun confirmPayment(
        currentState: WalletState,
        sessionId: String,
        amountInput: String?,
        note: String,
    ): WalletState {
        val amount = amountInput?.toDoubleOrNull()
        if (amountInput != null && amount == null) {
            return currentState.withConfirmFailure("Amount must be a valid decimal value.")
        }
        return when (val paymentResult = paymentRepository.confirmPayment(sessionId, amount, note)) {
            is PaymentRepositoryResult.ConfirmationSuccess -> currentState.withConfirmedPayment(paymentResult.receipt)
            is PaymentRepositoryResult.Failure -> currentState.withConfirmFailure(paymentResult.message)
            else -> currentState
        }
    }

    suspend fun loadPaymentHistory(currentState: WalletState): WalletState {
        return when (val paymentResult = paymentRepository.listPayments()) {
            is PaymentRepositoryResult.HistorySuccess -> currentState.withHistoryLoaded(paymentResult.history)
            is PaymentRepositoryResult.Failure -> currentState.withHistoryFailure(paymentResult.message)
            else -> currentState
        }
    }

    suspend fun loadPaymentReceipt(currentState: WalletState, paymentId: String): WalletState {
        return when (val paymentResult = paymentRepository.getPaymentReceipt(paymentId)) {
            is PaymentRepositoryResult.ReceiptSuccess -> currentState.withReceiptLoaded(paymentResult.receipt)
            is PaymentRepositoryResult.Failure -> currentState.withReceiptFailure(paymentResult.message)
            else -> currentState
        }
    }
}
