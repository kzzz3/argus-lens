package com.kzzz3.argus.lens.data.conversation

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ConversationApiService {
    @GET("api/v1/conversations")
    suspend fun listConversations(
        @Query("recentWindowDays") recentWindowDays: Int,
        @Header("Authorization") authorizationHeader: String,
    ): Response<List<RemoteConversationSummary>>

    @POST("api/v1/conversations")
    suspend fun createConversation(
        @Header("Authorization") authorizationHeader: String,
        @Body request: CreateConversationRequest,
    ): Response<RemoteConversationSummary>

    @GET("api/v1/conversations/{conversationId}")
    suspend fun getConversationDetail(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @Header("Authorization") authorizationHeader: String,
    ): Response<RemoteConversationDetail>

    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun listMessages(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @Query("recentWindowDays") recentWindowDays: Int,
        @Query("limit") limit: Int,
        @Query("sinceCursor") sinceCursor: String?,
        @Header("Authorization") authorizationHeader: String,
    ): Response<RemoteConversationMessagePage>

    @POST("api/v1/conversations/{conversationId}/messages")
    suspend fun sendMessage(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: SendRemoteMessageRequest,
    ): Response<RemoteConversationMessage>

    @POST("api/v1/conversations/{conversationId}/messages/{messageId}/receipt")
    suspend fun applyReceipt(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @retrofit2.http.Path("messageId") messageId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: MessageReceiptRequest,
    ): Response<RemoteConversationMessage>

    @POST("api/v1/conversations/{conversationId}/messages/{messageId}/recall")
    suspend fun recallMessage(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @retrofit2.http.Path("messageId") messageId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<RemoteConversationMessage>

    @POST("api/v1/conversations/{conversationId}/read")
    suspend fun markConversationRead(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<RemoteConversationSummary>
}
