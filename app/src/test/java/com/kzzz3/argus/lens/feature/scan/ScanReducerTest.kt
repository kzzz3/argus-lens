package com.kzzz3.argus.lens.feature.scan

import com.kzzz3.argus.lens.data.payment.PaymentScanResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanReducerTest {

    @Test
    fun submitManualPayload_trimsInputAndEmitsResolveEffect() {
        val result = reduceScanState(
            currentState = ScanState(manualPayload = "  argus://pay?merchantAccountId=lisi  "),
            action = ScanAction.SubmitManualPayload,
        )

        assertTrue(result.state.isResolving)
        assertEquals("argus://pay?merchantAccountId=lisi", result.state.activePayload)
        assertEquals(
            ScanEffect.ResolvePayload("argus://pay?merchantAccountId=lisi"),
            result.effect,
        )
    }

    @Test
    fun confirmPayment_requiresAmountForEditableCode() {
        val result = reduceScanState(
            currentState = ScanState(
                resolution = ScanResolutionUi(
                    scanSessionId = "scan-1",
                    merchantAccountId = "lisi",
                    merchantDisplayName = "Li Si",
                    currency = "CNY",
                    suggestedAmount = null,
                    amountEditable = true,
                    suggestedNote = "",
                ),
            ),
            action = ScanAction.ConfirmPayment,
        )

        assertNull(result.effect)
        assertFalse(result.state.isConfirming)
        assertTrue(result.state.isStatusError)
    }

    @Test
    fun resolvedPayment_prefillsAmountAndNote() {
        val updated = ScanState().withResolvedPayment(
            PaymentScanResolution(
                scanSessionId = "scan-1",
                merchantAccountId = "lisi",
                merchantDisplayName = "Li Si",
                currency = "CNY",
                suggestedAmount = 18.88,
                amountEditable = false,
                suggestedNote = "Lunch set",
            )
        )

        assertEquals("18.88", updated.amountDraft)
        assertEquals("Lunch set", updated.noteDraft)
        assertFalse(updated.isStatusError)
        assertEquals("Li Si", updated.resolution?.merchantDisplayName)
    }
}
