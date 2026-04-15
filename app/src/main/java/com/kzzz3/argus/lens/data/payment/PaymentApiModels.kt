package com.kzzz3.argus.lens.data.payment

data class ResolvePaymentScanRequestBody(
    val scanPayload: String,
)

data class ResolvePaymentScanResponseBody(
    val scanSessionId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val currency: String,
    val suggestedAmount: Double?,
    val amountEditable: Boolean,
    val suggestedNote: String,
)

data class ConfirmPaymentRequestBody(
    val amount: Double?,
    val note: String,
)

data class ConfirmPaymentResponseBody(
    val paymentId: String,
    val scanSessionId: String,
    val status: String,
    val payerAccountId: String,
    val merchantAccountId: String,
    val merchantDisplayName: String,
    val amount: Double,
    val currency: String,
    val note: String,
    val paidAt: String,
)

data class PaymentHistoryItemResponseBody(
    val paymentId: String,
    val merchantDisplayName: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paidAt: String,
)
