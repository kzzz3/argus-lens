package com.kzzz3.argus.lens.data.auth

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @GET("api/v1/auth/session/me")
    suspend fun restoreSession(
        @Header("Authorization") authorizationHeader: String,
    ): Response<AuthSuccessResponse>

    @POST("api/v1/auth/refresh")
    suspend fun refresh(
        @Body request: RefreshTokenRequestBody,
    ): Response<AuthSuccessResponse>

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestBody,
    ): Response<AuthSuccessResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestBody,
    ): Response<AuthSuccessResponse>
}
