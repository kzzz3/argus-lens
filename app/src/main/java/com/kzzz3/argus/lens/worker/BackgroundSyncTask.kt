package com.kzzz3.argus.lens.worker

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.session.SessionRepository
import javax.inject.Inject

class BackgroundSyncTask @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val conversationRepository: ConversationRepository,
) {
    suspend fun run(): BackgroundSyncResult {
        val session = sessionRepository.loadSession()
        val credentials = sessionRepository.loadCredentials()
        if (!session.isAuthenticated || session.accountId.isBlank() || !credentials.hasAccessToken) {
            return BackgroundSyncResult.SkippedNoSession
        }

        val currentState = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = session.displayName,
        )
        conversationRepository.saveConversationThreads(
            accountId = session.accountId,
            state = currentState,
        )
        return BackgroundSyncResult.Synced
    }
}

enum class BackgroundSyncResult {
    Synced,
    SkippedNoSession,
}
