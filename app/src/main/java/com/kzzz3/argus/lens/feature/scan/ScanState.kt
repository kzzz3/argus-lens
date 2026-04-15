package com.kzzz3.argus.lens.feature.scan

import android.os.Parcelable
import com.kzzz3.argus.lens.data.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.data.payment.PaymentReceipt
import com.kzzz3.argus.lens.data.payment.PaymentScanResolution
import java.util.Locale
import kotlinx.parcelize.Parcelize

enum class ScanPage {
    Scanner,
    Merchant,
    Result,
    History,
    ReceiptDetail,
}

@Parcelize
data class ScanState(
    val page: ScanPage = ScanPage.Scanner,
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
    val historyItems: List<ScanPaymentHistoryItemUi> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val selectedReceipt: ScanPaymentReceiptUi? = null,
    val isReceiptLoading: Boolean = false,
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
    val scanSessionId: String,
    val status: String,
    val payerAccountId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val amount: String,
    val currency: String,
    val note: String,
    val paidAt: String,
) : Parcelable

@Parcelize
data class ScanPaymentHistoryItemUi(
    val paymentId: String,
    val merchantDisplayName: String,
    val amount: String,
    val currency: String,
    val status: String,
    val paidAt: String,
) : Parcelable

fun ScanState.withResolvedPayment(resolution: PaymentScanResolution): ScanState {
    return copy(
        page = ScanPage.Merchant,
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
        selectedReceipt = null,
        completedPayment = null,
        statusMessage = "Merchant code resolved. Review the merchant details before paying.",
        isStatusError = false,
    )
}

fun ScanState.withResolveFailure(message: String): ScanState {
    return copy(
        page = ScanPage.Scanner,
        isResolving = false,
        activePayload = "",
        resolution = null,
        completedPayment = null,
        statusMessage = message,
        isStatusError = true,
    )
}

fun ScanState.withConfirmedPayment(receipt: PaymentReceipt): ScanState {
    return copy(
        page = ScanPage.Result,
        isResolving = false,
        resolution = null,
        amountDraft = receipt.amount.toCurrencyText(),
        noteDraft = receipt.note,
        isConfirming = false,
        completedPayment = receipt.toReceiptUi(),
        selectedReceipt = null,
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

fun ScanState.withHistoryLoading(): ScanState {
    return copy(
        page = ScanPage.History,
        isHistoryLoading = true,
        isReceiptLoading = false,
        selectedReceipt = null,
        statusMessage = null,
        isStatusError = false,
    )
}

fun ScanState.withHistoryLoaded(history: List<PaymentHistoryEntry>): ScanState {
    return copy(
        page = ScanPage.History,
        historyItems = history.map { item ->
            ScanPaymentHistoryItemUi(
                paymentId = item.paymentId,
                merchantDisplayName = item.merchantDisplayName,
                amount = item.amount.toCurrencyText(),
                currency = item.currency,
                status = item.status,
                paidAt = item.paidAt,
            )
        },
        isHistoryLoading = false,
        statusMessage = if (history.isEmpty()) "No transaction records yet." else null,
        isStatusError = false,
    )
}

fun ScanState.withHistoryFailure(message: String): ScanState {
    return copy(
        page = ScanPage.History,
        isHistoryLoading = false,
        statusMessage = message,
        isStatusError = true,
    )
}

fun ScanState.withReceiptLoading(): ScanState {
    return copy(
        isReceiptLoading = true,
        statusMessage = null,
        isStatusError = false,
    )
}

fun ScanState.withReceiptLoaded(receipt: PaymentReceipt): ScanState {
    return copy(
        page = ScanPage.ReceiptDetail,
        selectedReceipt = receipt.toReceiptUi(),
        isReceiptLoading = false,
        statusMessage = null,
        isStatusError = false,
    )
}

fun ScanState.withReceiptFailure(message: String): ScanState {
    return copy(
        isReceiptLoading = false,
        statusMessage = message,
        isStatusError = true,
    )
}

private fun PaymentReceipt.toReceiptUi(): ScanPaymentReceiptUi {
    return ScanPaymentReceiptUi(
        paymentId = paymentId,
        scanSessionId = scanSessionId,
        status = status,
        payerAccountId = payerAccountId,
        merchantAccountId = merchantAccountId,
        merchantDisplayName = merchantDisplayName,
        amount = amount.toCurrencyText(),
        currency = currency,
        note = note,
        paidAt = paidAt,
    )
}

private fun Double.toCurrencyText(): String = String.format(Locale.US, "%.2f", this)
