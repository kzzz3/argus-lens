package com.kzzz3.argus.lens.data.auth

import com.google.gson.Gson
import com.kzzz3.argus.lens.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
    val gson = Gson()
    val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.AUTH_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    return RetrofitAuthRepository(
        authApiService = retrofit.create(AuthApiService::class.java),
        gson = gson,
    )
}
