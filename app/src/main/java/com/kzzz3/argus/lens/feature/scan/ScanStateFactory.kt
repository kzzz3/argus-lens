package com.kzzz3.argus.lens.feature.scan

fun createScanUiState(state: ScanState): ScanUiState {
    val isScannerActive = state.cameraPermissionGranted &&
        !state.isResolving &&
        !state.isConfirming &&
        state.resolution == null &&
        state.completedPayment == null

    return ScanUiState(
        title = "Scan Pay",
        subtitle = "Scan an Argus merchant QR code, confirm the amount, then finish payment with a linked chat receipt.",
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
        canConfirm = state.resolution != null && !state.isConfirming,
        completedPayment = state.completedPayment,
        openConversationActionLabel = "Open merchant chat",
        backActionLabel = "Back to inbox",
    )
}
