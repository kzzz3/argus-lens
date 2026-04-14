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
                    resolution = PaymentScanResolution(
                        scanSessionId = body.scanSessionId,
                        merchantAccountId = body.merchantAccountId,
                        merchantDisplayName = body.merchantDisplayName,
                        currency = body.currency,
                        suggestedAmount = body.suggestedAmount,
                        amountEditable = body.amountEditable,
                        suggestedNote = body.suggestedNote,
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
                    confirmation = PaymentConfirmation(
                        paymentId = body.paymentId,
                        status = body.status,
                        merchantAccountId = body.merchantAccountId,
                        merchantDisplayName = body.merchantDisplayName,
                        conversationId = body.conversationId,
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
