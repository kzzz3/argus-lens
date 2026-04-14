package com.kzzz3.argus.lens.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.createAuthRepository
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.createConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.createFriendRepository
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.createMediaRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.realtime.createConversationRealtimeClient
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.data.session.createLocalSessionStore

data class AppDependencies(
    val authRepository: AuthRepository,
    val sessionRepository: SessionRepository,
    val conversationRepository: ConversationRepository,
    val friendRepository: FriendRepository,
    val mediaRepository: MediaRepository,
    val realtimeClient: ConversationRealtimeClient,
    val appShellCoordinator: AppShellCoordinator,
)

@Composable
fun rememberAppDependencies(
    context: Context,
): AppDependencies {
    val authRepository = remember { createAuthRepository() }
    val sessionRepository = remember(context) { createLocalSessionStore(context) }
    val conversationRepository = remember(context, sessionRepository) {
        createConversationRepository(context = context, sessionRepository = sessionRepository)
    }
    val friendRepository = remember(sessionRepository) {
        createFriendRepository(sessionRepository)
    }
    val mediaRepository = remember(sessionRepository, context) {
        createMediaRepository(sessionRepository, context)
    }
    val realtimeClient = remember { createConversationRealtimeClient() }
    val appShellCoordinator = remember(authRepository, sessionRepository, conversationRepository) {
        AppShellCoordinator(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
        )
    }
    return remember(
        authRepository,
        sessionRepository,
        conversationRepository,
        friendRepository,
        mediaRepository,
        realtimeClient,
        appShellCoordinator,
    ) {
        AppDependencies(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            friendRepository = friendRepository,
            mediaRepository = mediaRepository,
            realtimeClient = realtimeClient,
            appShellCoordinator = appShellCoordinator,
        )
    }
}
