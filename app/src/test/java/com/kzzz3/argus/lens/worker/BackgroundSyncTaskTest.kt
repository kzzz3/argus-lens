package com.kzzz3.argus.lens.worker

import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundSyncTaskTest {
    @Test
    fun run_skipsWhenSessionHasNoAccessToken() = runBlocking {
        val conversationRepository = FakeConversationRepository()
        val task = BackgroundSyncTask(
            sessionRepository = FakeSessionRepository(
                session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
                credentials = SessionCredentials(),
            ),
            conversationRepository = conversationRepository,
        )

        val result = task.run()

        assertEquals(BackgroundSyncResult.SkippedNoSession, result)
        assertEquals(0, conversationRepository.saveCount)
    }

    @Test
    fun run_loadsAndSavesConversationSnapshotForAuthenticatedSession() = runBlocking {
        val conversationRepository = FakeConversationRepository()
        val task = BackgroundSyncTask(
            sessionRepository = FakeSessionRepository(
                session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
                credentials = SessionCredentials(accessToken = "access-token"),
            ),
            conversationRepository = conversationRepository,
        )

        val result = task.run()

        assertEquals(BackgroundSyncResult.Synced, result)
        assertEquals(1, conversationRepository.saveCount)
        assertEquals("tester", conversationRepository.savedAccountId)
    }

    @Test
    fun run_retriesWhenConversationSyncFailsForAuthenticatedSession() = runBlocking {
        val conversationRepository = FakeConversationRepository(
            loadFailure = IllegalStateException("network unavailable"),
        )
        val task = BackgroundSyncTask(
            sessionRepository = FakeSessionRepository(
                session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
                credentials = SessionCredentials(accessToken = "access-token"),
            ),
            conversationRepository = conversationRepository,
        )

        val result = task.run()

        assertEquals(BackgroundSyncResult.Retry, result)
        assertEquals(0, conversationRepository.saveCount)
    }

    private class FakeSessionRepository(
        private val session: AppSessionState,
        private val credentials: SessionCredentials,
    ) : SessionRepository {
        override suspend fun loadSession(): AppSessionState = session
        override suspend fun loadCredentials(): SessionCredentials = credentials
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) = Unit
        override suspend fun clearSession() = Unit
    }

    private class FakeConversationRepository(
        private val loadFailure: RuntimeException? = null,
    ) : ConversationRepository {
        var saveCount: Int = 0
        var savedAccountId: String = ""

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()

        override suspend fun loadOrCreateConversationThreads(
            accountId: String,
            currentUserDisplayName: String,
        ): ConversationThreadsState {
            loadFailure?.let { throw it }
            return ConversationThreadsState()
        }

        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) {
            saveCount += 1
            savedAccountId = accountId
        }

        override suspend fun clearConversationThreads(accountId: String) = Unit
        override suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun sendMessage(state: ConversationThreadsState, conversationId: String, localMessageId: String, body: String, attachment: ChatMessageAttachment?): ConversationThreadsState = state
        override suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState = state
        override fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
        override fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
    }
}
