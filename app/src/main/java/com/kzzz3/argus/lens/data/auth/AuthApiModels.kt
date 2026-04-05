package com.kzzz3.argus.lens.data.auth

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
    val message: String,
)

data class ApiErrorResponse(
    val code: String,
    val message: String,
)

data class AuthSession(
    val accountId: String,
    val displayName: String,
    val accessToken: String,
    val message: String,
)
