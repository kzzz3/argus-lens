package com.kzzz3.argus.lens.feature.wallet

sealed interface WalletAction {
    data object NavigateBack : WalletAction
    data object RefreshWalletSummary : WalletAction
    data object OpenPayScanner : WalletAction
    data object OpenCollectQr : WalletAction
    data object OpenTransactionHistory : WalletAction
    data object RequestCameraPermission : WalletAction
    data class CameraPermissionResult(val granted: Boolean) : WalletAction
    data class UpdateManualPayload(val value: String) : WalletAction
    data object SubmitManualPayload : WalletAction
    data class DetectedPayload(val value: String) : WalletAction
    data class UpdatePayAmountDraft(val value: String) : WalletAction
    data class UpdatePayNoteDraft(val value: String) : WalletAction
    data object ConfirmPayment : WalletAction
    data class UpdateCollectAmountDraft(val value: String) : WalletAction
    data class UpdateCollectNoteDraft(val value: String) : WalletAction
    data object ClearCollectRequest : WalletAction
    data class OpenReceiptDetail(val paymentId: String) : WalletAction
    data object ResetForRescan : WalletAction
}
