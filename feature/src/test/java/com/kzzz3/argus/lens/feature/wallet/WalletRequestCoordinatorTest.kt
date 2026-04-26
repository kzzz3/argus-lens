package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.data.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.data.payment.PaymentReceipt
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.payment.PaymentTransferResolution
import com.kzzz3.argus.lens.data.payment.WalletSummary
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRequestCoordinatorTest {
    @Test
    fun loadWalletSummary_mapsSuccessIntoUiState() = runBlocking {
        val coordinator = WalletRequestCoordinator(
            FakePaymentRepository(
                walletSummaryResult = PaymentRepositoryResult.WalletSummarySuccess(
                    WalletSummary("tester", "Tester", 42.5, "CNY")
                )
            )
        )

        val state = coordinator.loadWalletSummary(WalletState())

        assertEquals("tester", state.currentAccountId)
        assertEquals("42.50", state.summary!!.balance)
        assertEquals(false, state.isStatusError)
    }

    @Test
    fun confirmPayment_rejectsInvalidAmountBeforeRepositoryCall() = runBlocking {
        val repository = FakePaymentRepository()
        val coordinator = WalletRequestCoordinator(repository)

        val state = coordinator.confirmPayment(WalletState(), "scan-1", "not-a-number", "note")

        assertEquals("Amount must be a valid decimal value.", state.statusMessage)
        assertTrue(state.isStatusError)
        assertEquals(0, repository.confirmRequests)
    }

    @Test
    fun loadPaymentHistory_mapsEmptyHistoryToStatusMessage() = runBlocking {
        val coordinator = WalletRequestCoordinator(
            FakePaymentRepository(
                historyResult = PaymentRepositoryResult.HistorySuccess(emptyList())
            )
        )

        val state = coordinator.loadPaymentHistory(WalletState(currentAccountId = "tester"))

        assertEquals(WalletPage.History, state.page)
        assertEquals("No wallet transfers yet.", state.statusMessage)
        assertEquals(false, state.isStatusError)
    }

    @Test
    fun loadPaymentReceipt_mapsFailureWithoutClearingExistingReceipt() = runBlocking {
        val existingReceipt = PaymentReceipt(
            paymentId = "payment-1",
            scanSessionId = "scan-1",
            status = "COMPLETED",
            payerAccountId = "tester",
            payerDisplayName = "Tester",
            payerBalanceAfter = 90.0,
            recipientAccountId = "lisi",
            recipientDisplayName = "Li Si",
            recipientBalanceAfter = 110.0,
            amount = 10.0,
            currency = "CNY",
            note = "Lunch",
            paidAt = "2026-04-24T00:00:00Z",
        )
        val coordinator = WalletRequestCoordinator(
            FakePaymentRepository(
                receiptResult = PaymentRepositoryResult.Failure("NOT_FOUND", "Receipt missing")
            )
        )

        val state = coordinator.loadPaymentReceipt(
            WalletState(currentAccountId = "tester").withReceiptLoaded(existingReceipt),
            "missing",
        )

        assertEquals("payment-1", state.selectedReceipt!!.paymentId)
        assertEquals("Receipt missing", state.statusMessage)
        assertTrue(state.isStatusError)
    }

    private class FakePaymentRepository(
        private val walletSummaryResult: PaymentRepositoryResult = PaymentRepositoryResult.Failure("UNUSED", "unused"),
        private val resolutionResult: PaymentRepositoryResult = PaymentRepositoryResult.ResolutionSuccess(
            PaymentTransferResolution("scan-1", "lisi", "Li Si", "CNY", 10.0, false, "Lunch")
        ),
        private val confirmationResult: PaymentRepositoryResult = PaymentRepositoryResult.ConfirmationSuccess(
            PaymentReceipt("payment-1", "scan-1", "COMPLETED", "tester", "Tester", 90.0, "lisi", "Li Si", 110.0, 10.0, "CNY", "Lunch", "2026-04-24T00:00:00Z")
        ),
        private val historyResult: PaymentRepositoryResult = PaymentRepositoryResult.HistorySuccess(
            listOf(PaymentHistoryEntry("payment-1", "tester", "Tester", "lisi", "Li Si", 10.0, "CNY", "COMPLETED", "2026-04-24T00:00:00Z"))
        ),
        private val receiptResult: PaymentRepositoryResult = PaymentRepositoryResult.ReceiptSuccess(
            PaymentReceipt("payment-1", "scan-1", "COMPLETED", "tester", "Tester", 90.0, "lisi", "Li Si", 110.0, 10.0, "CNY", "Lunch", "2026-04-24T00:00:00Z")
        ),
    ) : PaymentRepository {
        var confirmRequests = 0

        override suspend fun getWalletSummary(): PaymentRepositoryResult = walletSummaryResult
        override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult = resolutionResult
        override suspend fun confirmPayment(sessionId: String, amount: Double?, note: String): PaymentRepositoryResult {
            confirmRequests += 1
            return confirmationResult
        }
        override suspend fun listPayments(): PaymentRepositoryResult = historyResult
        override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult = receiptResult
    }
}
