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

fun resolveAuthMode(): AuthMode {
    return runCatching {
        AuthMode.valueOf(BuildConfig.AUTH_MODE)
    }.getOrElse {
        AuthMode.LOCAL
    }
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
