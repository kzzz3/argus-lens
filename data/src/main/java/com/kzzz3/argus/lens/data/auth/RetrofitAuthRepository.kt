package com.kzzz3.argus.lens.data.auth

import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

class RetrofitAuthRepository(
    private val authApiService: AuthApiService,
    private val gson: Gson,
) : AuthRepository {
    override suspend fun restoreSession(accessToken: String): AuthRepositoryResult {
        return executeRequest {
            authApiService.restoreSession(authorizationHeader = "Bearer $accessToken")
        }
    }

    override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult {
        return executeRequest {
            authApiService.refresh(RefreshTokenRequestBody(refreshToken = refreshToken))
        }
    }

    override suspend fun login(account: String, password: String): AuthRepositoryResult {
        return executeRequest {
            authApiService.login(LoginRequestBody(account = account, password = password))
        }
    }

    override suspend fun register(
        displayName: String,
        account: String,
        password: String,
    ): AuthRepositoryResult {
        return executeRequest {
            authApiService.register(
                RegisterRequestBody(
                    displayName = displayName,
                    account = account,
                    password = password,
                )
            )
        }
    }

    private suspend fun executeRequest(
        block: suspend () -> Response<AuthSuccessResponse>,
    ): AuthRepositoryResult {
        return try {
            val response = block()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AuthRepositoryResult.Success(
                        session = AuthSession(
                            accountId = body.accountId,
                            displayName = body.displayName,
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken.orEmpty(),
                            message = body.message,
                        )
                    )
                } else {
                    AuthRepositoryResult.Failure(
                        code = "EMPTY_AUTH_RESPONSE",
                        message = "Server returned an empty auth response.",
                        kind = AuthFailureKind.SERVER,
                    )
                }
            } else {
                parseErrorMessage(response)
            }
        } catch (_: IOException) {
            AuthRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach argus-cortex. Start the server locally and keep AUTH_BASE_URL pointed at 10.0.2.2:8080 for the Android emulator.",
                kind = AuthFailureKind.NETWORK,
            )
        }
    }

    private fun parseErrorMessage(response: Response<AuthSuccessResponse>): AuthRepositoryResult.Failure {
        val errorBody = response.errorBody()?.string().orEmpty()
        val parsedError = runCatching {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)
        }.getOrNull()
        val message = runCatching {
            parsedError?.message
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Auth request failed with HTTP ${response.code()}."
        val code = parsedError?.code?.takeIf { it.isNotBlank() }

        val kind = if (response.code() == 401) {
            AuthFailureKind.UNAUTHORIZED
        } else {
            AuthFailureKind.SERVER
        }

        return AuthRepositoryResult.Failure(
            code = code,
            message = message,
            kind = kind,
        )
    }
}
