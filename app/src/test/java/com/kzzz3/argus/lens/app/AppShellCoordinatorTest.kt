package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
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
        val sessionRepository = FakeSessionRepository(persistedSession, SessionCredentials(accessToken = "token"))
        val backgroundSyncScheduler = FakeBackgroundSyncScheduler()
        val coordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = FakeConversationRepository(expectedThreads),
            paymentRepository = FakePaymentRepository(),
            backgroundSyncScheduler = backgroundSyncScheduler,
        )

        val result = coordinator.hydrateAppState(previewThreadsState = ConversationThreadsState())

        assertEquals(persistedSession, result.session)
        assertEquals(expectedThreads, result.conversationThreadsState)
        assertEquals("argus_tester", result.hydratedConversationAccountId)
        assertTrue(!sessionRepository.cleared)
        assertEquals(1, backgroundSyncScheduler.enqueueCount)
    }

    @Test
    fun hydrateAppState_withCachedSession_entersShellBeforeValidationCompletes() = runBlocking {
        val persistedSession = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
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
        val sessionRepository = FakeSessionRepository(persistedSession, SessionCredentials(accessToken = "token"))
        val backgroundSyncScheduler = FakeBackgroundSyncScheduler()
        val coordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = FakeConversationRepository(previewThreads),
            paymentRepository = FakePaymentRepository(),
            backgroundSyncScheduler = backgroundSyncScheduler,
        )

        val result = coordinator.hydrateAppState(previewThreadsState = previewThreads)

        assertEquals(persistedSession, result.session)
        assertEquals(previewThreads, result.conversationThreadsState)
        assertEquals("argus_tester", result.hydratedConversationAccountId)
        assertTrue(!sessionRepository.cleared)
        assertEquals(1, backgroundSyncScheduler.enqueueCount)
    }

    @Test
    fun hydrateAppState_withoutCredentials_doesNotScheduleBackgroundSync() = runBlocking {
        val sessionRepository = FakeSessionRepository(AppSessionState(), SessionCredentials())
        val backgroundSyncScheduler = FakeBackgroundSyncScheduler()
        val coordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = FakeConversationRepository(ConversationThreadsState()),
            paymentRepository = FakePaymentRepository(),
            backgroundSyncScheduler = backgroundSyncScheduler,
        )

        coordinator.hydrateAppState(previewThreadsState = ConversationThreadsState())

        assertEquals(0, backgroundSyncScheduler.enqueueCount)
    }

    private class FakeSessionRepository(
        private var state: AppSessionState,
        private var credentials: SessionCredentials,
    ) : SessionRepository {
        var cleared: Boolean = false

        override suspend fun loadSession(): AppSessionState = state

        override suspend fun loadCredentials(): SessionCredentials = credentials

        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) {
            this.state = state
            this.credentials = credentials
            this.cleared = false
        }

        override suspend fun clearSession() {
            this.state = AppSessionState()
            this.credentials = SessionCredentials()
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

    private class FakePaymentRepository : PaymentRepository {
        override suspend fun getWalletSummary(): PaymentRepositoryResult = failure()
        override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult = failure()
        override suspend fun confirmPayment(sessionId: String, amount: Double?, note: String): PaymentRepositoryResult = failure()
        override suspend fun listPayments(): PaymentRepositoryResult = failure()
        override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult = failure()

        private fun failure(): PaymentRepositoryResult = PaymentRepositoryResult.Failure(
            code = "UNUSED",
            message = "unused",
        )
    }

    private class FakeBackgroundSyncScheduler : BackgroundSyncScheduler {
        var enqueueCount: Int = 0

        override fun enqueue() {
            enqueueCount += 1
        }
    }
}
