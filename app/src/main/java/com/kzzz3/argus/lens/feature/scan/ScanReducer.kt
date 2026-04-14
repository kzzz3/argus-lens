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
        ScanAction.NavigateBack -> ScanReducerResult(
            state = currentState,
            effect = ScanEffect.NavigateBack,
        )

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

        ScanAction.ResetForRescan -> ScanReducerResult(
            state = currentState.copy(
                manualPayload = "",
                activePayload = "",
                isResolving = false,
                resolution = null,
                amountDraft = "",
                noteDraft = "",
                isConfirming = false,
                completedPayment = null,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        ScanAction.OpenReceiptConversation -> {
            val receipt = currentState.completedPayment
            if (receipt == null) {
                ScanReducerResult(currentState, null)
            } else {
                ScanReducerResult(
                    state = currentState,
                    effect = ScanEffect.OpenConversation(receipt.conversationId),
                )
            }
        }
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
            activePayload = trimmedPayload,
            isResolving = true,
            resolution = null,
            completedPayment = null,
            statusMessage = null,
            isStatusError = false,
        ),
        effect = ScanEffect.ResolvePayload(trimmedPayload),
    )
}
