package com.kzzz3.argus.lens.core.network

import com.google.gson.Gson
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createAppGson(): Gson = Gson()

fun createAppHttpClient(
    enableVerboseHttpLogs: Boolean = BuildConfig.DEBUG,
): OkHttpClient {
    val builder = OkHttpClient.Builder()
    if (enableVerboseHttpLogs) {
        builder.addInterceptor(
            HttpLoggingInterceptor().apply {
                redactHeader("Authorization")
                redactHeader("Cookie")
                redactHeader("Set-Cookie")
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
    }
    return builder.build()
}

fun createAppRetrofit(
    baseUrl: String = BuildConfig.AUTH_BASE_URL,
    gson: Gson = createAppGson(),
    okHttpClient: OkHttpClient = createAppHttpClient(),
): Retrofit {
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
}

fun createAppBaseUrl(
    baseUrl: String = BuildConfig.AUTH_BASE_URL,
): HttpUrl = baseUrl.toHttpUrl()
