package com.kzzz3.argus.lens.data.conversation

import android.content.Context
import com.google.gson.Gson
import com.kzzz3.argus.lens.BuildConfig
import com.kzzz3.argus.lens.data.local.createLocalConversationCoordinator
import com.kzzz3.argus.lens.data.session.SessionRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class ConversationMode {
    LOCAL,
    REMOTE,
}

fun createConversationRepository(
    context: Context,
    sessionRepository: SessionRepository,
    mode: ConversationMode = resolveConversationMode(),
): ConversationRepository {
    val localRepository = createLocalConversationCoordinator(context)
    return when (mode) {
        ConversationMode.LOCAL -> localRepository
        ConversationMode.REMOTE -> createRemoteConversationRepository(localRepository, sessionRepository)
    }
}

fun resolveConversationMode(): ConversationMode {
    return runCatching {
        ConversationMode.valueOf(BuildConfig.CONVERSATION_MODE)
    }.getOrElse {
        ConversationMode.LOCAL
    }
}

fun createRemoteConversationRepository(
    localRepository: ConversationRepository,
    sessionRepository: SessionRepository,
): ConversationRepository {
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

    return RemoteConversationRepository(
        localRepository = localRepository,
        sessionRepository = sessionRepository,
        conversationApiService = retrofit.create(ConversationApiService::class.java),
    )
}
