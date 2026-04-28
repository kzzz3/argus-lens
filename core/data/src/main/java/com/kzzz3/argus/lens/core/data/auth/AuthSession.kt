package com.kzzz3.argus.lens.core.data.auth

data class AuthSession(
    val accountId: String,
    val displayName: String,
    val accessToken: String,
    val refreshToken: String,
    val message: String,
)
