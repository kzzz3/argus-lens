package com.kzzz3.argus.lens.data.auth

sealed interface AuthRepositoryResult {
    data class Success(val session: AuthSession) : AuthRepositoryResult
    data class Failure(
        val code: String?,
        val message: String,
        val kind: AuthFailureKind,
    ) : AuthRepositoryResult
}

enum class AuthFailureKind {
    NETWORK,
    UNAUTHORIZED,
    SERVER,
}

interface AuthRepository {
    suspend fun restoreSession(accessToken: String): AuthRepositoryResult

    suspend fun login(account: String, password: String): AuthRepositoryResult

    suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult
}
