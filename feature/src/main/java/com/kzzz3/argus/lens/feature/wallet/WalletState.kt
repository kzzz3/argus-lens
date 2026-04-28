package com.kzzz3.argus.lens.feature.wallet

import android.os.Parcelable
import com.kzzz3.argus.lens.model.payment.PaymentHistoryEntry
import com.kzzz3.argus.lens.model.payment.PaymentReceipt
import com.kzzz3.argus.lens.model.payment.PaymentTransferResolution
import com.kzzz3.argus.lens.model.payment.WalletSummary
import java.util.Locale
import kotlinx.parcelize.Parcelize

enum class WalletPage {
    Overview,
    PayScanner,
    PayReview,
    PayResult,
    Collect,
    History,
    ReceiptDetail,
}

enum class WalletTransferDirection {
    Sent,
    Received,
}

@Parcelize
data class WalletState(
    val currentAccountId: String = "",
    val page: WalletPage = WalletPage.Overview,
    val cameraPermissionGranted: Boolean = false,
    val shouldRequestCameraPermission: Boolean = false,
    val hasAttemptedSummaryLoad: Boolean = false,
    val isLoadingSummary: Boolean = false,
    val summary: WalletSummaryUi? = null,
    val manualPayload: String = "",
    val activePayload: String = "",
    val isResolving: Boolean = false,
    val resolution: WalletResolutionUi? = null,
    val payAmountDraft: String = "",
    val payNoteDraft: String = "",
    val isConfirming: Boolean = false,
    val completedPayment: WalletReceiptUi? = null,
    val collectAmountDraft: String = "",
    val collectNoteDraft: String = "",
    val historyItems: List<WalletHistoryItemUi> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val selectedReceipt: WalletReceiptUi? = null,
    val isReceiptLoading: Boolean = false,
    val statusMessage: String? = null,
    val isStatusError: Boolean = false,
) : Parcelable

@Parcelize
data class WalletSummaryUi(
    val accountId: String,
    val displayName: String,
    val balance: String,
    val currency: String,
) : Parcelable

@Parcelize
data class WalletResolutionUi(
    val scanSessionId: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val currency: String,
    val requestedAmount: String?,
    val amountEditable: Boolean,
    val requestedNote: String,
) : Parcelable

@Parcelize
data class WalletReceiptUi(
    val paymentId: String,
    val scanSessionId: String,
    val status: String,
    val direction: WalletTransferDirection,
    val payerAccountId: String,
    val payerDisplayName: String,
    val payerBalanceAfter: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val recipientBalanceAfter: String,
    val amount: String,
    val currency: String,
    val note: String,
    val paidAt: String,
) : Parcelable

@Parcelize
data class WalletHistoryItemUi(
    val paymentId: String,
    val direction: WalletTransferDirection,
    val counterpartyDisplayName: String,
    val amount: String,
    val currency: String,
    val status: String,
    val paidAt: String,
) : Parcelable

data class WalletTransferMetadata(
    val direction: WalletTransferDirection,
    val counterpartyDisplayName: String,
)

fun WalletState.withCurrentAccount(accountId: String): WalletState {
    return if (accountId.isBlank() || accountId == currentAccountId) {
        this
    } else {
        copy(
            currentAccountId = accountId,
            hasAttemptedSummaryLoad = false,
        )
    }
}

fun WalletState.withWalletSummaryLoading(): WalletState {
    return copy(
        hasAttemptedSummaryLoad = true,
        isLoadingSummary = true,
        statusMessage = null,
        isStatusError = false,
    )
}

fun WalletState.withWalletSummaryLoaded(summary: WalletSummary): WalletState {
    return copy(
        currentAccountId = summary.accountId,
        hasAttemptedSummaryLoad = true,
        isLoadingSummary = false,
        summary = WalletSummaryUi(
            accountId = summary.accountId,
            displayName = summary.displayName,
            balance = summary.balance.toCurrencyText(),
            currency = summary.currency,
        ),
        statusMessage = null,
        isStatusError = false,
    )
}

fun WalletState.withWalletSummaryFailure(message: String): WalletState {
    return copy(
        isLoadingSummary = false,
        statusMessage = message,
        isStatusError = true,
    )
}

fun WalletState.withResolvedPayment(resolution: PaymentTransferResolution): WalletState {
    return copy(
        page = WalletPage.PayReview,
        isResolving = false,
        activePayload = "",
        resolution = WalletResolutionUi(
            scanSessionId = resolution.scanSessionId,
            recipientAccountId = resolution.recipientAccountId,
            recipientDisplayName = resolution.recipientDisplayName,
            currency = resolution.currency,
            requestedAmount = resolution.requestedAmount?.toCurrencyText(),
            amountEditable = resolution.amountEditable,
            requestedNote = resolution.requestedNote,
        ),
        payAmountDraft = resolution.requestedAmount?.toCurrencyText().orEmpty(),
        payNoteDraft = resolution.requestedNote,
        isConfirming = false,
        selectedReceipt = null,
        completedPayment = null,
        statusMessage = "Recipient code resolved. Review the wallet transfer before sending.",
        isStatusError = false,
    )
}

fun WalletState.withResolveFailure(message: String): WalletState {
    return copy(
        page = WalletPage.PayScanner,
        isResolving = false,
        activePayload = "",
        resolution = null,
        completedPayment = null,
        statusMessage = message,
        isStatusError = true,
    )
}

fun WalletState.withConfirmedPayment(receipt: PaymentReceipt): WalletState {
    val receiptUi = receipt.toReceiptUi(currentAccountId)
    val viewerBalanceAfter = receipt.viewerBalanceAfter(currentAccountId)
    return copy(
        page = WalletPage.PayResult,
        isResolving = false,
        resolution = null,
        payAmountDraft = receipt.amount.toCurrencyText(),
        payNoteDraft = receipt.note,
        isConfirming = false,
        completedPayment = receiptUi,
        selectedReceipt = null,
        summary = summary?.copy(balance = viewerBalanceAfter.toCurrencyText()),
        statusMessage = "Wallet transfer completed successfully.",
        isStatusError = false,
    )
}

fun WalletState.withConfirmFailure(message: String): WalletState {
    return copy(
        isConfirming = false,
        statusMessage = message,
        isStatusError = true,
    )
}

fun WalletState.withHistoryLoading(): WalletState {
    return copy(
        page = WalletPage.History,
        isHistoryLoading = true,
        isReceiptLoading = false,
        selectedReceipt = null,
        statusMessage = null,
        isStatusError = false,
    )
}

fun WalletState.withHistoryLoaded(history: List<PaymentHistoryEntry>): WalletState {
    return copy(
        page = WalletPage.History,
        historyItems = history.map { item ->
            WalletHistoryItemUi(
                paymentId = item.paymentId,
                direction = item.toDirection(currentAccountId),
                counterpartyDisplayName = item.counterpartyDisplayName(currentAccountId),
                amount = item.amount.toCurrencyText(),
                currency = item.currency,
                status = item.status,
                paidAt = item.paidAt,
            )
        },
        isHistoryLoading = false,
        statusMessage = if (history.isEmpty()) "No wallet transfers yet." else null,
        isStatusError = false,
    )
}

fun WalletState.withHistoryFailure(message: String): WalletState {
    return copy(
        page = WalletPage.History,
        isHistoryLoading = false,
        statusMessage = message,
        isStatusError = true,
    )
}

fun WalletState.withReceiptLoading(): WalletState {
    return copy(
        isReceiptLoading = true,
        statusMessage = null,
        isStatusError = false,
    )
}

fun WalletState.withReceiptLoaded(receipt: PaymentReceipt): WalletState {
    return copy(
        page = WalletPage.ReceiptDetail,
        selectedReceipt = receipt.toReceiptUi(currentAccountId),
        isReceiptLoading = false,
        statusMessage = null,
        isStatusError = false,
    )
}

fun WalletState.withReceiptFailure(message: String): WalletState {
    return copy(
        isReceiptLoading = false,
        statusMessage = message,
        isStatusError = true,
    )
}

private fun PaymentReceipt.toReceiptUi(currentAccountId: String): WalletReceiptUi {
    val metadata = resolveWalletTransferMetadata(
        currentAccountId = currentAccountId,
        payerAccountId = payerAccountId,
        payerDisplayName = payerDisplayName,
        recipientDisplayName = recipientDisplayName,
    )
    return WalletReceiptUi(
        paymentId = paymentId,
        scanSessionId = scanSessionId,
        status = status,
        direction = metadata.direction,
        payerAccountId = payerAccountId,
        payerDisplayName = payerDisplayName,
        payerBalanceAfter = payerBalanceAfter.toCurrencyText(),
        recipientAccountId = recipientAccountId,
        recipientDisplayName = recipientDisplayName,
        recipientBalanceAfter = recipientBalanceAfter.toCurrencyText(),
        amount = amount.toCurrencyText(),
        currency = currency,
        note = note,
        paidAt = paidAt,
    )
}

fun PaymentReceipt.viewerBalanceAfter(currentAccountId: String): Double {
    return if (payerAccountId == currentAccountId) {
        payerBalanceAfter
    } else {
        recipientBalanceAfter
    }
}

fun PaymentReceipt.toDirection(currentAccountId: String): WalletTransferDirection {
    return resolveWalletTransferMetadata(
        currentAccountId = currentAccountId,
        payerAccountId = payerAccountId,
        payerDisplayName = payerDisplayName,
        recipientDisplayName = recipientDisplayName,
    ).direction
}

fun PaymentHistoryEntry.toDirection(currentAccountId: String): WalletTransferDirection {
    return resolveWalletTransferMetadata(
        currentAccountId = currentAccountId,
        payerAccountId = payerAccountId,
        payerDisplayName = payerDisplayName,
        recipientDisplayName = recipientDisplayName,
    ).direction
}

fun PaymentHistoryEntry.counterpartyDisplayName(currentAccountId: String): String {
    return resolveWalletTransferMetadata(
        currentAccountId = currentAccountId,
        payerAccountId = payerAccountId,
        payerDisplayName = payerDisplayName,
        recipientDisplayName = recipientDisplayName,
    ).counterpartyDisplayName
}

fun resolveWalletTransferMetadata(
    currentAccountId: String,
    payerAccountId: String,
    payerDisplayName: String,
    recipientDisplayName: String,
): WalletTransferMetadata {
    return if (payerAccountId == currentAccountId) {
        WalletTransferMetadata(
            direction = WalletTransferDirection.Sent,
            counterpartyDisplayName = recipientDisplayName,
        )
    } else {
        WalletTransferMetadata(
            direction = WalletTransferDirection.Received,
            counterpartyDisplayName = payerDisplayName,
        )
    }
}

private fun Double.toCurrencyText(): String = String.format(Locale.US, "%.2f", this)
