package com.kzzz3.argus.lens.data.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun clearLocalData_clearsSummaryAndWalletDetailsCaches() {
        val summaryCache = FakeWalletSummaryCache()
        val detailsCache = FakeWalletDetailsCache()
        val repository = CachedPaymentRepository(
            remoteRepository = FakePaymentRepository(),
            sessionRepository = FakeSessionRepository(),
            walletSummaryStore = summaryCache,
            walletDetailsStore = detailsCache,
        )

        repository.clearLocalData("tester")

        assertEquals(listOf("tester"), summaryCache.clearedAccounts)
        assertEquals(listOf("tester"), detailsCache.clearedAccounts)
        assertTrue(detailsCache.clearedAll)
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

    private class FakeWalletSummaryCache : WalletSummaryCache {
        val clearedAccounts = mutableListOf<String>()

        override fun load(accountId: String): WalletSummary? = null

        override fun save(summary: WalletSummary) = Unit

        override fun clear(accountId: String) {
            clearedAccounts += accountId
        }
    }

    private class FakeWalletDetailsCache : WalletDetailsCache {
        val clearedAccounts = mutableListOf<String>()
        var clearedAll: Boolean = false

        override fun loadHistory(accountId: String): List<PaymentHistoryEntry>? = null

        override fun saveHistory(accountId: String, history: List<PaymentHistoryEntry>) = Unit

        override fun loadReceipt(paymentId: String): PaymentReceipt? = null

        override fun saveReceipt(receipt: PaymentReceipt) = Unit

        override fun upsertHistoryEntry(accountId: String, entry: PaymentHistoryEntry) = Unit

        override fun clearAccount(accountId: String) {
            clearedAccounts += accountId
        }

        override fun clearAll() {
            clearedAll = true
        }
    }

    private class FakePaymentRepository : PaymentRepository {
        override suspend fun getWalletSummary(): PaymentRepositoryResult = PaymentRepositoryResult.Failure(null, "unused")

        override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult = PaymentRepositoryResult.Failure(null, "unused")

        override suspend fun confirmPayment(sessionId: String, amount: Double?, note: String): PaymentRepositoryResult = PaymentRepositoryResult.Failure(null, "unused")

        override suspend fun listPayments(): PaymentRepositoryResult = PaymentRepositoryResult.Failure(null, "unused")

        override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult = PaymentRepositoryResult.Failure(null, "unused")
    }

    private class FakeSessionRepository : com.kzzz3.argus.lens.data.session.SessionRepository {
        override suspend fun loadSession(): com.kzzz3.argus.lens.model.session.AppSessionState = com.kzzz3.argus.lens.model.session.AppSessionState()

        override suspend fun loadCredentials(): com.kzzz3.argus.lens.data.session.SessionCredentials = com.kzzz3.argus.lens.data.session.SessionCredentials()

        override suspend fun saveSession(
            state: com.kzzz3.argus.lens.model.session.AppSessionState,
            credentials: com.kzzz3.argus.lens.data.session.SessionCredentials,
        ) = Unit

        override suspend fun clearSession() = Unit
    }
}
