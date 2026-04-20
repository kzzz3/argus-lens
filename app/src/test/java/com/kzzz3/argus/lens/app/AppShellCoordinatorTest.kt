package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppShellCoordinatorTest {

    @Test
    fun hydrateAppState_withNetworkFailure_keepsPersistedSessionAndLoadsLocalThreads() = runBlocking {
        val persistedSession = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
            accessToken = "token",
        )
        val expectedThreads = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 1,
                    messages = emptyList(),
                )
            )
        )
        val sessionRepository = FakeSessionRepository(persistedSession)
        val coordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = FakeConversationRepository(expectedThreads),
        )

        val result = coordinator.hydrateAppState(previewThreadsState = ConversationThreadsState())

        assertEquals(persistedSession, result.session)
        assertEquals(expectedThreads, result.conversationThreadsState)
        assertEquals("argus_tester", result.hydratedConversationAccountId)
        assertTrue(!sessionRepository.cleared)
    }

    @Test
    fun hydrateAppState_withCachedSession_entersShellBeforeValidationCompletes() = runBlocking {
        val persistedSession = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
            accessToken = "token",
        )
        val previewThreads = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "preview",
                    title = "Preview",
                    subtitle = "seed",
                    unreadCount = 0,
                    messages = emptyList(),
                )
            )
        )
        val sessionRepository = FakeSessionRepository(persistedSession)
        val coordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = FakeConversationRepository(previewThreads),
        )

        val result = coordinator.hydrateAppState(previewThreadsState = previewThreads)

        assertEquals(persistedSession, result.session)
        assertEquals(previewThreads, result.conversationThreadsState)
        assertEquals("argus_tester", result.hydratedConversationAccountId)
        assertTrue(!sessionRepository.cleared)
    }

    private class FakeSessionRepository(
        private var state: AppSessionState,
    ) : SessionRepository {
        var cleared: Boolean = false

        override suspend fun loadSession(): AppSessionState = state

        override suspend fun saveSession(state: AppSessionState) {
            this.state = state
            this.cleared = false
        }

        override suspend fun clearSession() {
            this.state = AppSessionState()
            this.cleared = true
        }
    }

    private class FakeConversationRepository(
        private val threads: ConversationThreadsState,
    ) : ConversationRepository {
        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = threads
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = threads
        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) = Unit
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
