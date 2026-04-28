package com.kzzz3.argus.lens.core.network.auth

data class LoginRequestBody(
    val account: String,
    val password: String,
)

data class RegisterRequestBody(
    val displayName: String,
    val account: String,
    val password: String,
)

data class AuthSuccessResponse(
    val accountId: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String?,
    val message: String,
)

data class RefreshTokenRequestBody(
    val refreshToken: String,
)
