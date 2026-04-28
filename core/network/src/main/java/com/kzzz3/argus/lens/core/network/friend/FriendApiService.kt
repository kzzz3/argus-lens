package com.kzzz3.argus.lens.core.network.friend

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface FriendApiService {
    @GET("api/v1/friends")
    suspend fun listFriends(
        @Header("Authorization") authorizationHeader: String,
    ): Response<List<FriendResponse>>

    @GET("api/v1/friends/requests")
    suspend fun listFriendRequests(
        @Header("Authorization") authorizationHeader: String,
    ): Response<PendingFriendRequestsResponse>

    @POST("api/v1/friends")
    suspend fun sendFriendRequest(
        @Header("Authorization") authorizationHeader: String,
        @Body request: AddFriendRequestBody,
    ): Response<FriendRequestResponse>

    @POST("api/v1/friends/requests/{requestId}/accept")
    suspend fun acceptFriendRequest(
        @retrofit2.http.Path("requestId") requestId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<FriendResponse>

    @POST("api/v1/friends/requests/{requestId}/reject")
    suspend fun rejectFriendRequest(
        @retrofit2.http.Path("requestId") requestId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<FriendRequestResponse>

    @POST("api/v1/friends/requests/{requestId}/ignore")
    suspend fun ignoreFriendRequest(
        @retrofit2.http.Path("requestId") requestId: String,
        @Header("Authorization") authorizationHeader: String,
        @Body request: Map<String, String> = emptyMap(),
    ): Response<FriendRequestResponse>
}
