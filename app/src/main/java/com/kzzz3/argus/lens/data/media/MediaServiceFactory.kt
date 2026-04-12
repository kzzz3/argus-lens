package com.kzzz3.argus.lens.data.media

import android.content.Context
import com.google.gson.Gson
import com.kzzz3.argus.lens.BuildConfig
import com.kzzz3.argus.lens.data.session.SessionRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun createMediaRepository(
    sessionRepository: SessionRepository,
    context: Context,
): MediaRepository {
    return createRemoteMediaRepository(sessionRepository, context)
}

private fun createRemoteMediaRepository(
    sessionRepository: SessionRepository,
    context: Context,
): MediaRepository {
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

    return RemoteMediaRepository(
        context = context,
        sessionRepository = sessionRepository,
        mediaApiService = retrofit.create(MediaApiService::class.java),
        gson = gson,
    )
}
