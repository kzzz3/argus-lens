package com.kzzz3.argus.lens.data.auth

sealed interface AuthRepositoryResult {
    data class Success(val session: AuthSession) : AuthRepositoryResult
    data class Failure(val message: String) : AuthRepositoryResult
}

interface AuthRepository {
    suspend fun login(account: String, password: String): AuthRepositoryResult

    suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult
}
