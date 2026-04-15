package com.kzzz3.argus.lens.feature.wallet

import android.net.Uri

fun createWalletUiState(state: WalletState): WalletUiState {
    val isScannerActive = state.cameraPermissionGranted &&
        state.page == WalletPage.PayScanner &&
        !state.isResolving &&
        !state.isConfirming &&
        state.resolution == null

    val (title, subtitle) = when (state.page) {
        WalletPage.Overview -> "Wallet" to "Check your balance, pay by scanning a friend's code, or show your own collect QR."
        WalletPage.PayScanner -> "Pay" to "Scan a friend's collect QR code, or paste the payload manually if the code is shared in chat."
        WalletPage.PayReview -> "Review transfer" to "Confirm the recipient, amount, and note before money leaves your wallet."
        WalletPage.PayResult -> "Transfer complete" to "The transfer finished successfully and the updated wallet balance is shown below."
        WalletPage.Collect -> "Collect" to "Show your wallet QR code so another user can scan it and send money to you."
        WalletPage.History -> "Transfer history" to "Every outgoing and incoming wallet transfer appears here with a receipt." 
        WalletPage.ReceiptDetail -> "Transfer receipt" to "This receipt records both participants and their post-transfer balances."
    }

    return WalletUiState(
        page = state.page,
        shouldLoadSummary = state.summary == null && !state.isLoadingSummary,
        title = title,
        subtitle = subtitle,
        statusMessage = state.statusMessage,
        isStatusError = state.isStatusError,
        isLoadingSummary = state.isLoadingSummary,
        summary = state.summary,
        refreshActionLabel = if (state.isLoadingSummary) "Refreshing..." else "Refresh wallet",
        payActionLabel = "Pay",
        collectActionLabel = "Collect",
        openHistoryActionLabel = "View transfer history",
        cameraPermissionGranted = state.cameraPermissionGranted,
        permissionActionLabel = "Grant camera access",
        isScannerActive = isScannerActive,
        scannerHint = if (isScannerActive) {
            "Align the recipient QR code inside the frame."
        } else if (state.isResolving) {
            "Resolving QR payload..."
        } else {
            "Scanner pauses after a code is resolved so duplicate frames do not trigger extra requests."
        },
        manualPayload = state.manualPayload,
        manualPayloadLabel = "Manual QR payload",
        manualPayloadPlaceholder = "argus://pay?recipientAccountId=lisi&amount=18.88&note=Lunch",
        resolveActionLabel = if (state.isResolving) "Resolving..." else "Resolve scanned code",
        resolution = state.resolution,
        payAmountDraft = state.payAmountDraft,
        payNoteDraft = state.payNoteDraft,
        amountLabel = "Amount (CNY)",
        noteLabel = "Transfer note",
        confirmActionLabel = if (state.isConfirming) "Sending..." else "Confirm transfer",
        rescanActionLabel = "Scan another code",
        canConfirm = state.resolution != null && !state.isConfirming,
        completedPayment = state.completedPayment,
        collectAmountDraft = state.collectAmountDraft,
        collectNoteDraft = state.collectNoteDraft,
        collectPayload = buildCollectPayload(
            accountId = state.summary?.accountId.orEmpty(),
            amountDraft = state.collectAmountDraft,
            noteDraft = state.collectNoteDraft,
        ),
        collectAmountLabel = "Requested amount (optional)",
        collectNoteLabel = "Collection note (optional)",
        clearCollectActionLabel = "Clear request",
        historyItems = state.historyItems,
        isHistoryLoading = state.isHistoryLoading,
        selectedReceipt = state.selectedReceipt,
        isReceiptLoading = state.isReceiptLoading,
        backActionLabel = when (state.page) {
            WalletPage.PayScanner,
            WalletPage.PayResult,
            WalletPage.Collect -> "Back to wallet"
            WalletPage.PayReview -> "Back to pay"
            WalletPage.History -> if (state.completedPayment != null) "Back to result" else "Back to wallet"
            WalletPage.ReceiptDetail -> "Back to history"
            WalletPage.Overview -> "Back to inbox"
        },
    )
}

private fun buildCollectPayload(
    accountId: String,
    amountDraft: String,
    noteDraft: String,
): String {
    if (accountId.isBlank()) return ""
    val builder = Uri.Builder()
        .scheme("argus")
        .authority("pay")
        .appendQueryParameter("recipientAccountId", accountId)
    amountDraft.trim().takeIf { it.isNotEmpty() }?.let { builder.appendQueryParameter("amount", it) }
    noteDraft.trim().takeIf { it.isNotEmpty() }?.let { builder.appendQueryParameter("note", it) }
    return builder.build().toString()
}
