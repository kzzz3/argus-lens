package com.kzzz3.argus.lens.data.payment

import com.google.gson.Gson
import com.kzzz3.argus.lens.data.auth.ApiErrorResponse
import com.kzzz3.argus.lens.data.session.SessionRepository
import java.io.IOException

class RemotePaymentRepository(
    private val sessionRepository: SessionRepository,
    private val paymentApiService: PaymentApiService,
    private val gson: Gson = Gson(),
) : PaymentRepository {
    override suspend fun getWalletSummary(): PaymentRepositoryResult {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return PaymentRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        return try {
            val response = paymentApiService.getWalletSummary("Bearer $accessToken")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return PaymentRepositoryResult.Failure(
                    code = "EMPTY_WALLET_SUMMARY",
                    message = "Payment service returned an empty wallet summary.",
                )
                PaymentRepositoryResult.WalletSummarySuccess(
                    summary = WalletSummary(
                        accountId = body.accountId,
                        displayName = body.displayName,
                        balance = body.balance,
                        currency = body.currency,
                    )
                )
            }
        } catch (_: IOException) {
            PaymentRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach payment service.",
            )
        }
    }

    override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return PaymentRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        return try {
            val response = paymentApiService.resolveScanPayload(
                authorizationHeader = "Bearer $accessToken",
                request = ResolvePaymentScanRequestBody(scanPayload.trim()),
            )
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return PaymentRepositoryResult.Failure(
                    code = "EMPTY_PAYMENT_SCAN_RESPONSE",
                    message = "Payment scan service returned an empty response.",
                )
                PaymentRepositoryResult.ResolutionSuccess(
                    resolution = PaymentTransferResolution(
                        scanSessionId = body.scanSessionId,
                        recipientAccountId = body.recipientAccountId,
                        recipientDisplayName = body.recipientDisplayName,
                        currency = body.currency,
                        requestedAmount = body.requestedAmount,
                        amountEditable = body.amountEditable,
                        requestedNote = body.requestedNote,
                    )
                )
            }
        } catch (_: IOException) {
            PaymentRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach payment service.",
            )
        }
    }

    override suspend fun confirmPayment(
        sessionId: String,
        amount: Double?,
        note: String,
    ): PaymentRepositoryResult {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return PaymentRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        return try {
            val response = paymentApiService.confirmPayment(
                authorizationHeader = "Bearer $accessToken",
                sessionId = sessionId,
                request = ConfirmPaymentRequestBody(
                    amount = amount,
                    note = note.trim(),
                ),
            )
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return PaymentRepositoryResult.Failure(
                    code = "EMPTY_PAYMENT_CONFIRMATION",
                    message = "Payment service returned an empty confirmation response.",
                )
                PaymentRepositoryResult.ConfirmationSuccess(
                    receipt = PaymentReceipt(
                        paymentId = body.paymentId,
                        scanSessionId = body.scanSessionId,
                        status = body.status,
                        payerAccountId = body.payerAccountId,
                        payerDisplayName = body.payerDisplayName,
                        payerBalanceAfter = body.payerBalanceAfter,
                        recipientAccountId = body.recipientAccountId,
                        recipientDisplayName = body.recipientDisplayName,
                        recipientBalanceAfter = body.recipientBalanceAfter,
                        amount = body.amount,
                        currency = body.currency,
                        note = body.note,
                        paidAt = body.paidAt,
                    )
                )
            }
        } catch (_: IOException) {
            PaymentRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach payment service.",
            )
        }
    }

    override suspend fun listPayments(): PaymentRepositoryResult {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return PaymentRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        return try {
            val response = paymentApiService.listPayments("Bearer $accessToken")
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                PaymentRepositoryResult.HistorySuccess(
                    history = response.body().orEmpty().map { item ->
                        PaymentHistoryEntry(
                            paymentId = item.paymentId,
                            payerAccountId = item.payerAccountId,
                            payerDisplayName = item.payerDisplayName,
                            recipientAccountId = item.recipientAccountId,
                            recipientDisplayName = item.recipientDisplayName,
                            amount = item.amount,
                            currency = item.currency,
                            status = item.status,
                            paidAt = item.paidAt,
                        )
                    }
                )
            }
        } catch (_: IOException) {
            PaymentRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach payment service.",
            )
        }
    }

    override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult {
        val accessToken = sessionRepository.loadSession().accessToken
        if (accessToken.isBlank()) {
            return PaymentRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        return try {
            val response = paymentApiService.getPaymentReceipt(
                authorizationHeader = "Bearer $accessToken",
                paymentId = paymentId,
            )
            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return PaymentRepositoryResult.Failure(
                    code = "EMPTY_PAYMENT_RECEIPT",
                    message = "Payment service returned an empty receipt response.",
                )
                PaymentRepositoryResult.ReceiptSuccess(
                    receipt = PaymentReceipt(
                        paymentId = body.paymentId,
                        scanSessionId = body.scanSessionId,
                        status = body.status,
                        payerAccountId = body.payerAccountId,
                        payerDisplayName = body.payerDisplayName,
                        payerBalanceAfter = body.payerBalanceAfter,
                        recipientAccountId = body.recipientAccountId,
                        recipientDisplayName = body.recipientDisplayName,
                        recipientBalanceAfter = body.recipientBalanceAfter,
                        amount = body.amount,
                        currency = body.currency,
                        note = body.note,
                        paidAt = body.paidAt,
                    )
                )
            }
        } catch (_: IOException) {
            PaymentRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach payment service.",
            )
        }
    }

    private fun parseFailure(httpCode: Int, rawBody: String): PaymentRepositoryResult.Failure {
        val parsed = runCatching { gson.fromJson(rawBody, ApiErrorResponse::class.java) }.getOrNull()
        return PaymentRepositoryResult.Failure(
            code = parsed?.code,
            message = parsed?.message ?: "Payment request failed with HTTP $httpCode.",
        )
    }
}
