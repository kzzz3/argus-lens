package com.kzzz3.argus.lens.feature.scan

data class ScanReducerResult(
    val state: ScanState,
    val effect: ScanEffect?,
)

fun reduceScanState(
    currentState: ScanState,
    action: ScanAction,
): ScanReducerResult {
    return when (action) {
        ScanAction.NavigateBack -> handleNavigateBack(currentState)

        ScanAction.RequestCameraPermission -> ScanReducerResult(
            state = currentState.copy(
                shouldRequestCameraPermission = true,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        is ScanAction.CameraPermissionResult -> ScanReducerResult(
            state = currentState.copy(
                cameraPermissionGranted = action.granted,
                shouldRequestCameraPermission = false,
                statusMessage = if (!action.granted) {
                    "Camera permission is required to scan merchant QR codes."
                } else {
                    currentState.statusMessage
                },
                isStatusError = !action.granted,
            ),
            effect = null,
        )

        is ScanAction.UpdateManualPayload -> ScanReducerResult(
            state = currentState.copy(manualPayload = action.value),
            effect = null,
        )

        ScanAction.SubmitManualPayload -> submitPayload(
            currentState = currentState,
            payload = currentState.manualPayload,
        )

        is ScanAction.DetectedPayload -> submitPayload(
            currentState = currentState,
            payload = action.value,
        )

        is ScanAction.UpdateAmountDraft -> ScanReducerResult(
            state = currentState.copy(amountDraft = action.value),
            effect = null,
        )

        is ScanAction.UpdateNoteDraft -> ScanReducerResult(
            state = currentState.copy(noteDraft = action.value),
            effect = null,
        )

        ScanAction.ConfirmPayment -> {
            val resolution = currentState.resolution
            if (resolution == null || currentState.isConfirming) {
                ScanReducerResult(currentState, null)
            } else {
                val normalizedAmount = currentState.amountDraft.trim().takeIf { it.isNotEmpty() }
                if (resolution.amountEditable && normalizedAmount == null) {
                    ScanReducerResult(
                        state = currentState.copy(
                            statusMessage = "Please enter an amount before confirming the payment.",
                            isStatusError = true,
                        ),
                        effect = null,
                    )
                } else {
                    ScanReducerResult(
                        state = currentState.copy(
                            isConfirming = true,
                            statusMessage = null,
                            isStatusError = false,
                        ),
                        effect = ScanEffect.ConfirmPayment(
                            sessionId = resolution.scanSessionId,
                            amountInput = normalizedAmount,
                            note = currentState.noteDraft.trim(),
                        ),
                    )
                }
            }
        }

        ScanAction.OpenTransactionHistory -> ScanReducerResult(
            state = currentState.withHistoryLoading(),
            effect = ScanEffect.LoadPaymentHistory,
        )

        is ScanAction.OpenReceiptDetail -> {
            if (currentState.isReceiptLoading) {
                ScanReducerResult(currentState, null)
            } else {
                ScanReducerResult(
                    state = currentState.withReceiptLoading(),
                    effect = ScanEffect.LoadPaymentReceipt(action.paymentId),
                )
            }
        }

        ScanAction.ResetForRescan -> ScanReducerResult(
            state = ScanState(
                cameraPermissionGranted = currentState.cameraPermissionGranted,
            ),
            effect = null,
        )
    }
}

private fun handleNavigateBack(currentState: ScanState): ScanReducerResult {
    return when (currentState.page) {
        ScanPage.ReceiptDetail -> ScanReducerResult(
            state = currentState.copy(
                page = ScanPage.History,
                selectedReceipt = null,
                isReceiptLoading = false,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        ScanPage.History -> ScanReducerResult(
            state = currentState.copy(
                page = if (currentState.completedPayment != null) ScanPage.Result else ScanPage.Scanner,
                selectedReceipt = null,
                isReceiptLoading = false,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        ScanPage.Merchant -> ScanReducerResult(
            state = currentState.copy(
                page = ScanPage.Scanner,
                resolution = null,
                amountDraft = "",
                noteDraft = "",
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        ScanPage.Scanner,
        ScanPage.Result -> ScanReducerResult(
            state = currentState,
            effect = ScanEffect.NavigateBack,
        )
    }
}

private fun submitPayload(
    currentState: ScanState,
    payload: String,
): ScanReducerResult {
    val trimmedPayload = payload.trim()
    if (trimmedPayload.isEmpty() || currentState.isResolving || currentState.isConfirming) {
        return ScanReducerResult(currentState, null)
    }

    if (trimmedPayload == currentState.activePayload) {
        return ScanReducerResult(currentState, null)
    }

    return ScanReducerResult(
        state = currentState.copy(
            page = ScanPage.Scanner,
            activePayload = trimmedPayload,
            isResolving = true,
            resolution = null,
            completedPayment = null,
            selectedReceipt = null,
            statusMessage = null,
            isStatusError = false,
        ),
        effect = ScanEffect.ResolvePayload(trimmedPayload),
    )
}
