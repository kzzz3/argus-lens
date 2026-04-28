package com.kzzz3.argus.lens.core.network.payment

data class WalletSummaryResponseBody(
    val accountId: String,
    val displayName: String,
    val balance: Double,
    val currency: String,
)

data class ResolvePaymentScanRequestBody(
    val scanPayload: String,
)

data class ResolvePaymentScanResponseBody(
    val scanSessionId: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val currency: String,
    val requestedAmount: Double?,
    val amountEditable: Boolean,
    val requestedNote: String,
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
    val payerDisplayName: String,
    val payerBalanceAfter: Double,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val recipientBalanceAfter: Double,
    val amount: Double,
    val currency: String,
    val note: String,
    val paidAt: String,
)

data class PaymentHistoryItemResponseBody(
    val paymentId: String,
    val payerAccountId: String,
    val payerDisplayName: String,
    val recipientAccountId: String,
    val recipientDisplayName: String,
    val amount: Double,
    val currency: String,
    val status: String,
    val paidAt: String,
)
