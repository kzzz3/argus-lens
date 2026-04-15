package com.kzzz3.argus.lens.feature.scan

fun createScanUiState(state: ScanState): ScanUiState {
    val isScannerActive = state.cameraPermissionGranted &&
        state.page == ScanPage.Scanner &&
        !state.isResolving &&
        !state.isConfirming &&
        state.resolution == null

    return ScanUiState(
        page = state.page,
        title = "Scan Pay",
        subtitle = "Scan an Argus merchant QR code, review the merchant page, complete payment, then inspect the receipt in transaction history.",
        statusMessage = state.statusMessage,
        isStatusError = state.isStatusError,
        cameraPermissionGranted = state.cameraPermissionGranted,
        permissionActionLabel = "Grant camera access",
        isScannerActive = isScannerActive,
        scannerHint = if (isScannerActive) {
            "Align the merchant QR code inside the frame."
        } else {
            "Scanner pauses automatically after a code is resolved so duplicate frames do not trigger extra requests."
        },
        manualPayload = state.manualPayload,
        manualPayloadLabel = "Manual QR payload",
        manualPayloadPlaceholder = "argus://pay?merchantAccountId=lisi&amount=18.88&note=Lunch",
        resolveActionLabel = if (state.isResolving) "Resolving..." else "Resolve scanned code",
        resolution = state.resolution,
        amountDraft = state.amountDraft,
        noteDraft = state.noteDraft,
        amountLabel = "Amount (CNY)",
        noteLabel = "Payment note",
        confirmActionLabel = if (state.isConfirming) "Confirming..." else "Confirm payment",
        rescanActionLabel = "Scan another code",
        openHistoryActionLabel = "View transaction history",
        openReceiptActionLabel = "View receipt details",
        canConfirm = state.resolution != null && !state.isConfirming,
        completedPayment = state.completedPayment,
        historyItems = state.historyItems,
        isHistoryLoading = state.isHistoryLoading,
        selectedReceipt = state.selectedReceipt,
        isReceiptLoading = state.isReceiptLoading,
        backActionLabel = when (state.page) {
            ScanPage.Merchant -> "Back to scanner"
            ScanPage.History -> if (state.completedPayment != null) "Back to result" else "Back to scanner"
            ScanPage.ReceiptDetail -> "Back to history"
            else -> "Back to inbox"
        },
    )
}
