package com.kzzz3.argus.lens.feature.scan

import android.os.Parcelable
import com.kzzz3.argus.lens.data.payment.PaymentConfirmation
import com.kzzz3.argus.lens.data.payment.PaymentScanResolution
import java.util.Locale
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScanState(
    val cameraPermissionGranted: Boolean = false,
    val shouldRequestCameraPermission: Boolean = false,
    val manualPayload: String = "",
    val activePayload: String = "",
    val isResolving: Boolean = false,
    val resolution: ScanResolutionUi? = null,
    val amountDraft: String = "",
    val noteDraft: String = "",
    val isConfirming: Boolean = false,
    val completedPayment: ScanPaymentReceiptUi? = null,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
) : Parcelable

@Parcelize
data class ScanResolutionUi(
    val scanSessionId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val currency: String,
    val suggestedAmount: String?,
    val amountEditable: Boolean,
    val suggestedNote: String,
) : Parcelable

@Parcelize
data class ScanPaymentReceiptUi(
    val paymentId: String,
    val status: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val conversationId: String,
    val amount: String,
    val currency: String,
    val note: String,
    val paidAt: String,
) : Parcelable

fun ScanState.withResolvedPayment(resolution: PaymentScanResolution): ScanState {
    return copy(
        isResolving = false,
        activePayload = "",
        resolution = ScanResolutionUi(
            scanSessionId = resolution.scanSessionId,
            merchantAccountId = resolution.merchantAccountId,
            merchantDisplayName = resolution.merchantDisplayName,
            currency = resolution.currency,
            suggestedAmount = resolution.suggestedAmount?.toCurrencyText(),
            amountEditable = resolution.amountEditable,
            suggestedNote = resolution.suggestedNote,
        ),
        amountDraft = resolution.suggestedAmount?.toCurrencyText().orEmpty(),
        noteDraft = resolution.suggestedNote,
        isConfirming = false,
        completedPayment = null,
        statusMessage = "Merchant code resolved. Confirm the payment details.",
        isStatusError = false,
    )
}

fun ScanState.withResolveFailure(message: String): ScanState {
    return copy(
        isResolving = false,
        activePayload = "",
        resolution = null,
        completedPayment = null,
        statusMessage = message,
        isStatusError = true,
    )
}

fun ScanState.withConfirmedPayment(confirmation: PaymentConfirmation): ScanState {
    return copy(
        isResolving = false,
        resolution = null,
        amountDraft = confirmation.amount.toCurrencyText(),
        noteDraft = confirmation.note,
        isConfirming = false,
        completedPayment = ScanPaymentReceiptUi(
            paymentId = confirmation.paymentId,
            status = confirmation.status,
            merchantAccountId = confirmation.merchantAccountId,
            merchantDisplayName = confirmation.merchantDisplayName,
            conversationId = confirmation.conversationId,
            amount = confirmation.amount.toCurrencyText(),
            currency = confirmation.currency,
            note = confirmation.note,
            paidAt = confirmation.paidAt,
        ),
        statusMessage = "Payment completed successfully.",
        isStatusError = false,
    )
}

fun ScanState.withConfirmFailure(message: String): ScanState {
    return copy(
        isConfirming = false,
        statusMessage = message,
        isStatusError = true,
    )
}

private fun Double.toCurrencyText(): String = String.format(Locale.US, "%.2f", this)
