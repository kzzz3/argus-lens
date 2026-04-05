package com.kzzz3.argus.lens.data.auth

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestBody,
    ): Response<AuthSuccessResponse>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestBody,
    ): Response<AuthSuccessResponse>
}
