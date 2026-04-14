package com.kzzz3.argus.lens.data.conversation

import android.content.Context
import com.kzzz3.argus.lens.BuildConfig
import com.kzzz3.argus.lens.data.local.createLocalConversationRepository
import com.kzzz3.argus.lens.data.network.createAppRetrofit
import com.kzzz3.argus.lens.data.session.SessionRepository

enum class ConversationMode {
    LOCAL,
    REMOTE,
}

fun createConversationRepository(
    context: Context,
    sessionRepository: SessionRepository,
    mode: ConversationMode = resolveConversationMode(),
): ConversationRepository {
    val localRepository = createLocalConversationRepository(context)
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
    val retrofit = createAppRetrofit()

    return RemoteConversationRepository(
        localRepository = localRepository,
        sessionRepository = sessionRepository,
        conversationApiService = retrofit.create(ConversationApiService::class.java),
    )
}
