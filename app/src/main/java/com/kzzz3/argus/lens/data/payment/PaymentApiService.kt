package com.kzzz3.argus.lens.data.payment

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PaymentApiService {
    @GET("api/v1/payments")
    suspend fun listPayments(
        @Header("Authorization") authorizationHeader: String,
    ): Response<List<PaymentHistoryItemResponseBody>>

    @GET("api/v1/payments/{paymentId}")
    suspend fun getPaymentReceipt(
        @Header("Authorization") authorizationHeader: String,
        @Path("paymentId") paymentId: String,
    ): Response<ConfirmPaymentResponseBody>

    @POST("api/v1/payments/scan-sessions/resolve")
    suspend fun resolveScanPayload(
        @Header("Authorization") authorizationHeader: String,
        @Body request: ResolvePaymentScanRequestBody,
    ): Response<ResolvePaymentScanResponseBody>

    @POST("api/v1/payments/scan-sessions/{sessionId}/confirm")
    suspend fun confirmPayment(
        @Header("Authorization") authorizationHeader: String,
        @Path("sessionId") sessionId: String,
        @Body request: ConfirmPaymentRequestBody,
    ): Response<ConfirmPaymentResponseBody>
}
