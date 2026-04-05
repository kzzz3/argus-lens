package com.kzzz3.argus.lens.data.auth

import com.google.gson.Gson
import java.io.IOException
import retrofit2.Response

class RetrofitAuthRepository(
    private val authApiService: AuthApiService,
    private val gson: Gson,
) : AuthRepository {
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
                            message = body.message,
                        )
                    )
                } else {
                    AuthRepositoryResult.Failure("Server returned an empty auth response.")
                }
            } else {
                AuthRepositoryResult.Failure(parseErrorMessage(response))
            }
        } catch (_: IOException) {
            AuthRepositoryResult.Failure(
                "Cannot reach argus-cortex. Start the server locally and keep AUTH_BASE_URL pointed at 10.0.2.2:8080 for the Android emulator."
            )
        }
    }

    private fun parseErrorMessage(response: Response<AuthSuccessResponse>): String {
        val errorBody = response.errorBody()?.string().orEmpty()
        return runCatching {
            gson.fromJson(errorBody, ApiErrorResponse::class.java)?.message
        }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "Auth request failed with HTTP ${response.code()}."
    }
}
