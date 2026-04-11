package com.kzzz3.argus.lens.data.friend

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

    @POST("api/v1/friends")
    suspend fun addFriend(
        @Header("Authorization") authorizationHeader: String,
        @Body request: AddFriendRequestBody,
    ): Response<FriendResponse>
}
