package com.kzzz3.argus.lens.data.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CachedPaymentRepositoryTest {

    @Test
    fun createCachedWalletSummary_usesPayerBalanceForCurrentSender() {
        val summary = createCachedWalletSummary(
            receipt = sampleReceipt(),
            currentAccountId = "tester",
            currentDisplayName = "Argus Tester",
        )

        requireNotNull(summary)
        assertEquals("tester", summary.accountId)
        assertEquals("Argus Tester", summary.displayName)
        assertEquals(981.12, summary.balance, 0.0)
        assertEquals("CNY", summary.currency)
    }

    @Test
    fun createCachedWalletSummary_usesRecipientBalanceAndFallbackName() {
        val summary = createCachedWalletSummary(
            receipt = sampleReceipt(),
            currentAccountId = "lisi",
            currentDisplayName = "",
        )

        requireNotNull(summary)
        assertEquals("lisi", summary.accountId)
        assertEquals("Li Si", summary.displayName)
        assertEquals(1018.88, summary.balance, 0.0)
        assertEquals("CNY", summary.currency)
    }

    @Test
    fun createCachedWalletSummary_returnsNullForBlankAccount() {
        val summary = createCachedWalletSummary(
            receipt = sampleReceipt(),
            currentAccountId = "   ",
            currentDisplayName = "Anyone",
        )

        assertNull(summary)
    }

    private fun sampleReceipt(): PaymentReceipt {
        return PaymentReceipt(
            paymentId = "payment-1",
            scanSessionId = "scan-1",
            status = "COMPLETED",
            payerAccountId = "tester",
            payerDisplayName = "Argus Tester",
            payerBalanceAfter = 981.12,
            recipientAccountId = "lisi",
            recipientDisplayName = "Li Si",
            recipientBalanceAfter = 1018.88,
            amount = 18.88,
            currency = "CNY",
            note = "Lunch",
            paidAt = "2026-04-18T10:00:00",
        )
    }
}
