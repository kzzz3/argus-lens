package com.kzzz3.argus.lens.feature.wallet

data class WalletReducerResult(
    val state: WalletState,
    val effect: WalletEffect?,
)

fun reduceWalletState(
    currentState: WalletState,
    action: WalletAction,
): WalletReducerResult {
    return when (action) {
        WalletAction.NavigateBack -> handleNavigateBack(currentState)

        WalletAction.RefreshWalletSummary -> {
            if (currentState.isLoadingSummary) {
                WalletReducerResult(currentState, null)
            } else {
                WalletReducerResult(
                    state = currentState.withWalletSummaryLoading(),
                    effect = WalletEffect.LoadWalletSummary,
                )
            }
        }

        WalletAction.OpenPayScanner -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.PayScanner,
                resolution = null,
                completedPayment = null,
                selectedReceipt = null,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletAction.OpenCollectQr -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.Collect,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletAction.OpenTransactionHistory -> WalletReducerResult(
            state = currentState.withHistoryLoading(),
            effect = WalletEffect.LoadPaymentHistory,
        )

        WalletAction.RequestCameraPermission -> WalletReducerResult(
            state = currentState.copy(
                shouldRequestCameraPermission = true,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        is WalletAction.CameraPermissionResult -> WalletReducerResult(
            state = currentState.copy(
                cameraPermissionGranted = action.granted,
                shouldRequestCameraPermission = false,
                statusMessage = if (!action.granted) {
                    "Camera permission is required to scan recipient QR codes."
                } else {
                    currentState.statusMessage
                },
                isStatusError = !action.granted,
            ),
            effect = null,
        )

        is WalletAction.UpdateManualPayload -> WalletReducerResult(
            state = currentState.copy(manualPayload = action.value),
            effect = null,
        )

        WalletAction.SubmitManualPayload -> submitPayload(
            currentState = currentState,
            payload = currentState.manualPayload,
        )

        is WalletAction.DetectedPayload -> submitPayload(
            currentState = currentState,
            payload = action.value,
        )

        is WalletAction.UpdatePayAmountDraft -> WalletReducerResult(
            state = currentState.copy(payAmountDraft = action.value),
            effect = null,
        )

        is WalletAction.UpdatePayNoteDraft -> WalletReducerResult(
            state = currentState.copy(payNoteDraft = action.value),
            effect = null,
        )

        WalletAction.ConfirmPayment -> {
            val resolution = currentState.resolution
            if (resolution == null || currentState.isConfirming) {
                WalletReducerResult(currentState, null)
            } else {
                val normalizedAmount = currentState.payAmountDraft.trim().takeIf { it.isNotEmpty() }
                if (resolution.amountEditable && normalizedAmount == null) {
                    WalletReducerResult(
                        state = currentState.copy(
                            statusMessage = "Please enter an amount before sending the transfer.",
                            isStatusError = true,
                        ),
                        effect = null,
                    )
                } else {
                    WalletReducerResult(
                        state = currentState.copy(
                            isConfirming = true,
                            statusMessage = null,
                            isStatusError = false,
                        ),
                        effect = WalletEffect.ConfirmPayment(
                            sessionId = resolution.scanSessionId,
                            amountInput = normalizedAmount,
                            note = currentState.payNoteDraft.trim(),
                        ),
                    )
                }
            }
        }

        is WalletAction.UpdateCollectAmountDraft -> WalletReducerResult(
            state = currentState.copy(collectAmountDraft = action.value),
            effect = null,
        )

        is WalletAction.UpdateCollectNoteDraft -> WalletReducerResult(
            state = currentState.copy(collectNoteDraft = action.value),
            effect = null,
        )

        WalletAction.ClearCollectRequest -> WalletReducerResult(
            state = currentState.copy(
                collectAmountDraft = "",
                collectNoteDraft = "",
            ),
            effect = null,
        )

        is WalletAction.OpenReceiptDetail -> {
            if (currentState.isReceiptLoading) {
                WalletReducerResult(currentState, null)
            } else {
                WalletReducerResult(
                    state = currentState.withReceiptLoading(),
                    effect = WalletEffect.LoadPaymentReceipt(action.paymentId),
                )
            }
        }

        WalletAction.ResetForRescan -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.PayScanner,
                manualPayload = "",
                activePayload = "",
                isResolving = false,
                resolution = null,
                payAmountDraft = "",
                payNoteDraft = "",
                isConfirming = false,
                completedPayment = null,
                selectedReceipt = null,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )
    }
}

private fun handleNavigateBack(currentState: WalletState): WalletReducerResult {
    return when (currentState.page) {
        WalletPage.ReceiptDetail -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.History,
                selectedReceipt = null,
                isReceiptLoading = false,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletPage.History -> WalletReducerResult(
            state = currentState.copy(
                page = if (currentState.completedPayment != null) WalletPage.PayResult else WalletPage.Overview,
                selectedReceipt = null,
                isReceiptLoading = false,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletPage.PayReview -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.PayScanner,
                resolution = null,
                payAmountDraft = "",
                payNoteDraft = "",
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletPage.PayScanner,
        WalletPage.PayResult,
        WalletPage.Collect -> WalletReducerResult(
            state = currentState.copy(
                page = WalletPage.Overview,
                statusMessage = null,
                isStatusError = false,
            ),
            effect = null,
        )

        WalletPage.Overview -> WalletReducerResult(
            state = currentState,
            effect = WalletEffect.NavigateBackToInbox,
        )
    }
}

private fun submitPayload(
    currentState: WalletState,
    payload: String,
): WalletReducerResult {
    val trimmedPayload = payload.trim()
    if (trimmedPayload.isEmpty() || currentState.isResolving || currentState.isConfirming) {
        return WalletReducerResult(currentState, null)
    }

    if (trimmedPayload == currentState.activePayload) {
        return WalletReducerResult(currentState, null)
    }

    return WalletReducerResult(
        state = currentState.copy(
            page = WalletPage.PayScanner,
            activePayload = trimmedPayload,
            isResolving = true,
            resolution = null,
            completedPayment = null,
            selectedReceipt = null,
            statusMessage = null,
            isStatusError = false,
        ),
        effect = WalletEffect.ResolvePayload(trimmedPayload),
    )
}
