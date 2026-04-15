package com.kzzz3.argus.lens.feature.scan

sealed interface ScanAction {
    data object NavigateBack : ScanAction
    data object RequestCameraPermission : ScanAction
    data class CameraPermissionResult(val granted: Boolean) : ScanAction
    data class UpdateManualPayload(val value: String) : ScanAction
    data object SubmitManualPayload : ScanAction
    data class DetectedPayload(val value: String) : ScanAction
    data class UpdateAmountDraft(val value: String) : ScanAction
    data class UpdateNoteDraft(val value: String) : ScanAction
    data object ConfirmPayment : ScanAction
    data object OpenTransactionHistory : ScanAction
    data class OpenReceiptDetail(val paymentId: String) : ScanAction
    data object ResetForRescan : ScanAction
}
