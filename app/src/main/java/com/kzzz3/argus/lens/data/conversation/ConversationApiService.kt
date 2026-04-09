package com.kzzz3.argus.lens.data.conversation

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface ConversationApiService {
    @GET("api/v1/conversations")
    suspend fun listConversations(
        @Header("Authorization") authorizationHeader: String,
    ): Response<List<RemoteConversationSummary>>

    @GET("api/v1/conversations/{conversationId}/messages")
    suspend fun listMessages(
        @retrofit2.http.Path("conversationId") conversationId: String,
        @Header("Authorization") authorizationHeader: String,
    ): Response<List<RemoteConversationMessage>>
}
