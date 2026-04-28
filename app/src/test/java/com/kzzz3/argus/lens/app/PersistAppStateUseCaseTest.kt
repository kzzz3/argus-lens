package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.app.runtime.PersistAppStateUseCase
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.model.session.createAuthenticatedSession
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PersistAppStateUseCaseTest {
    @Test
    fun persistSessionAndConversationThreadsDelegatesThroughShellUseCases() = runBlocking {
        val sessionRepository = FakeSessionRepository()
        val conversationRepository = FakeConversationRepository()
        val useCase = PersistAppStateUseCase(
            appShellUseCases = AppShellUseCases(
                sessionRepository = sessionRepository,
                conversationRepository = conversationRepository,
                paymentRepository = FakePaymentRepository(),
                backgroundSyncScheduler = FakeBackgroundSyncScheduler(),
            )
        )
        val session = createAuthenticatedSession("tester", "Tester")
        val credentials = SessionCredentials("access-token", "refresh-token")
        val threads = ConversationThreadsState()

        useCase.persistSession(session, credentials)
        useCase.persistConversationThreads(
            session = session,
            hydratedConversationAccountId = "tester",
            state = threads,
        )

        assertEquals(session, sessionRepository.savedSession)
        assertEquals(credentials, sessionRepository.savedCredentials)
        assertEquals("tester", conversationRepository.savedAccountId)
        assertEquals(threads, conversationRepository.savedState)
    }

    private class FakeSessionRepository : SessionRepository {
        var savedSession: AppSessionState = AppSessionState()
        var savedCredentials: SessionCredentials = SessionCredentials()

        override suspend fun loadSession(): AppSessionState = AppSessionState()
        override suspend fun loadCredentials(): SessionCredentials = SessionCredentials()
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) {
            savedSession = state
            savedCredentials = credentials
        }
        override suspend fun clearSession() {
            savedSession = AppSessionState()
            savedCredentials = SessionCredentials()
        }
    }

    private class FakeConversationRepository : ConversationRepository {
        var savedAccountId: String = ""
        var savedState: ConversationThreadsState = ConversationThreadsState()

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) {
            savedAccountId = accountId
            savedState = state
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
        override fun enqueue() = Unit
    }
}
