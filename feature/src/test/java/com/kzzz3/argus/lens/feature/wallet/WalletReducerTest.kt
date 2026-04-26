package com.kzzz3.argus.lens.feature.wallet

import com.kzzz3.argus.lens.data.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.data.payment.PaymentTransferResolution
import com.kzzz3.argus.lens.data.payment.WalletSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletReducerTest {

    @Test
    fun refreshWalletSummary_emitsLoadEffect() {
        val result = reduceWalletState(
            currentState = WalletState(),
            action = WalletAction.RefreshWalletSummary,
        )

        assertTrue(result.state.isLoadingSummary)
        assertEquals(WalletEffect.LoadWalletSummary, result.effect)
    }

    @Test
    fun submitManualPayload_trimsInputAndEmitsResolveEffect() {
        val result = reduceWalletState(
            currentState = WalletState(page = WalletPage.PayScanner, manualPayload = "  argus://pay?recipientAccountId=lisi  "),
            action = WalletAction.SubmitManualPayload,
        )

        assertTrue(result.state.isResolving)
        assertEquals("argus://pay?recipientAccountId=lisi", result.state.activePayload)
        assertEquals(
            WalletEffect.ResolvePayload("argus://pay?recipientAccountId=lisi"),
            result.effect,
        )
    }

    @Test
    fun confirmPayment_requiresAmountForEditableCode() {
        val result = reduceWalletState(
            currentState = WalletState(
                resolution = WalletResolutionUi(
                    scanSessionId = "scan-1",
                    recipientAccountId = "lisi",
                    recipientDisplayName = "Li Si",
                    currency = "CNY",
                    requestedAmount = null,
                    amountEditable = true,
                    requestedNote = "",
                ),
            ),
            action = WalletAction.ConfirmPayment,
        )

        assertNull(result.effect)
        assertFalse(result.state.isConfirming)
        assertTrue(result.state.isStatusError)
    }

    @Test
    fun walletSummaryLoaded_updatesOverviewCard() {
        val updated = WalletState().withWalletSummaryLoaded(
            WalletSummary(
                accountId = "tester",
                displayName = "Argus Tester",
                balance = 1000.0,
                currency = "CNY",
            )
        )

        assertEquals("tester", updated.currentAccountId)
        assertEquals("1000.00", updated.summary?.balance)
        assertFalse(updated.isStatusError)
    }

    @Test
    fun walletSummaryFailure_stopsAutomaticReloadUntilManualRetry() {
        val failedState = WalletState()
            .withWalletSummaryLoading()
            .withWalletSummaryFailure("offline")

        val uiState = createWalletUiState(failedState)

        assertFalse(uiState.shouldLoadSummary)
        assertTrue(failedState.hasAttemptedSummaryLoad)
    }

    @Test
    fun historyLoaded_mapsCounterpartyFromViewerPerspective() {
        val updated = WalletState(currentAccountId = "tester").withHistoryLoaded(
            listOf(
                PaymentHistoryEntry(
                    paymentId = "payment-1",
                    payerAccountId = "tester",
                    payerDisplayName = "Argus Tester",
                    recipientAccountId = "lisi",
                    recipientDisplayName = "Li Si",
                    amount = 18.88,
                    currency = "CNY",
                    status = "COMPLETED",
                    paidAt = "2026-04-15T10:00:00",
                )
            )
        )

        assertEquals(WalletPage.History, updated.page)
        assertEquals(WalletTransferDirection.Sent, updated.historyItems.first().direction)
        assertEquals("Li Si", updated.historyItems.first().counterpartyDisplayName)
    }
}
