package com.kzzz3.argus.lens.data.conversation

import android.content.Context
import com.kzzz3.argus.lens.BuildConfig
import com.kzzz3.argus.lens.data.local.createLocalConversationCoordinator

enum class ConversationMode {
    LOCAL,
    REMOTE,
}

fun createConversationRepository(
    context: Context,
    mode: ConversationMode = resolveConversationMode(),
): ConversationRepository {
    val localRepository = createLocalConversationCoordinator(context)
    return when (mode) {
        ConversationMode.LOCAL -> localRepository
        ConversationMode.REMOTE -> RemoteConversationCoordinator(localRepository)
    }
}

fun resolveConversationMode(): ConversationMode {
    return runCatching {
        ConversationMode.valueOf(BuildConfig.CONVERSATION_MODE)
    }.getOrElse {
        ConversationMode.LOCAL
    }
}

private class RemoteConversationCoordinator(
    private val localRepository: ConversationRepository,
) : ConversationRepository by localRepository
