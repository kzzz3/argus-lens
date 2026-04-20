package com.kzzz3.argus.lens.data.auth

import com.kzzz3.argus.lens.BuildConfig
import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppRetrofit

enum class AuthMode {
    LOCAL,
    REMOTE,
}

fun createAuthRepository(
    mode: AuthMode = resolveAuthMode(),
): AuthRepository {
    return when (mode) {
        AuthMode.LOCAL -> createLocalAuthRepository()
        AuthMode.REMOTE -> createRemoteAuthRepository()
    }
}

fun createAuthRepositoryOrUnavailable(
    mode: AuthMode?,
): AuthRepository {
    return when (mode) {
        AuthMode.LOCAL -> createLocalAuthRepository()
        AuthMode.REMOTE -> createRemoteAuthRepository()
        null -> UnavailableAuthRepository()
    }
}

fun resolveAuthMode(): AuthMode {
    return resolveAuthModeOrNull() ?: AuthMode.REMOTE
}

fun resolveAuthModeOrNull(
    rawMode: String = BuildConfig.AUTH_MODE,
): AuthMode? {
    return runCatching {
        AuthMode.valueOf(rawMode)
    }.getOrNull()
}

fun createLocalAuthRepository(): AuthRepository {
    return LocalAuthRepository()
}

fun createRemoteAuthRepository(): AuthRepository {
    val gson = createAppGson()
    val retrofit = createAppRetrofit(gson = gson)

    return RetrofitAuthRepository(
        authApiService = retrofit.create(AuthApiService::class.java),
        gson = gson,
    )
}

class UnavailableAuthRepository : AuthRepository {
    override suspend fun restoreSession(accessToken: String): AuthRepositoryResult {
        return unavailableAuthResult()
    }

    override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult {
        return unavailableAuthResult()
    }

    override suspend fun login(account: String, password: String): AuthRepositoryResult {
        return unavailableAuthResult()
    }

    override suspend fun register(
        displayName: String,
        account: String,
        password: String,
    ): AuthRepositoryResult {
        return unavailableAuthResult()
    }
}

private fun unavailableAuthResult(): AuthRepositoryResult.Failure {
    return AuthRepositoryResult.Failure(
        code = "AUTH_MODE_UNAVAILABLE",
        message = "Auth is unavailable because the app auth mode is misconfigured. Check BuildConfig.AUTH_MODE.",
        kind = AuthFailureKind.SERVER,
    )
}
