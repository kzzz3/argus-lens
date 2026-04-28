package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.auth.AuthSession
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.app.runtime.AppSessionBoundaryCallbacks
import com.kzzz3.argus.lens.app.runtime.SessionBoundaryHandler
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionBoundaryHandlerTest {
    @Test
    fun applySuccessfulAuthResultResetsSessionScopedStateAndPublishesAuthenticatedSession() = runBlocking {
        val loadedThreads = ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = emptyList(),
                )
            )
        )
        val scheduler = FakeBackgroundSyncScheduler()
        val handler = SessionBoundaryHandler(
            appShellUseCases = AppShellUseCases(
                sessionRepository = FakeSessionRepository(),
                conversationRepository = FakeConversationRepository(loadedThreads),
                paymentRepository = FakePaymentRepository(),
                backgroundSyncScheduler = scheduler,
            ),
            refreshSessionOnce = { _, _ -> error("refresh not expected") },
            startSessionRefreshLoop = { error("loop not expected") },
            cancelSessionRefreshLoop = { error("cancel refresh not expected") },
            invalidateWalletRequests = { invalidatedWalletRequests += 1 },
            cancelCallSession = { cancelledCallSessions += 1 },
        )
        val callbacks = RecordingCallbacks()

        handler.applySuccessfulAuthResult(
            authResult = AuthRepositoryResult.Success(
                AuthSession(
                    accountId = "tester",
                    displayName = "Argus Tester",
                    accessToken = "access-token",
                    refreshToken = "refresh-token",
                    message = "welcome",
                )
            ),
            keepSubmitMessageOnAuthForm = true,
            callbacks = callbacks,
        )

        assertEquals(1, invalidatedWalletRequests)
        assertEquals(1, cancelledCallSessions)
        assertEquals(1, scheduler.enqueueCount)
        assertEquals(listOf(null), callbacks.hydratedConversationAccountIds)
        assertEquals(loadedThreads, callbacks.conversationThreadsState)
        assertEquals(WalletState(), callbacks.walletState)
        assertEquals(CallSessionState(), callbacks.callSessionState)
        assertEquals("welcome", callbacks.authFormState.submitResult)
        assertEquals("tester", callbacks.authFormState.account)
        assertEquals(AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Argus Tester"), callbacks.authenticatedSession)
        assertEquals(SessionCredentials("access-token", "refresh-token"), callbacks.authenticatedCredentials)
        assertEquals("tester", callbacks.authenticatedHydratedConversationAccountId)
        assertEquals(1, callbacks.authenticatedRealtimeReconnectIncrement)
    }

    private var invalidatedWalletRequests: Int = 0
    private var cancelledCallSessions: Int = 0

    private class RecordingCallbacks {
        val hydratedConversationAccountIds = mutableListOf<String?>()
        var callSessionState: CallSessionState = CallSessionState()
        var walletState: WalletState = WalletState()
        var conversationThreadsState: ConversationThreadsState = ConversationThreadsState()
        var authenticatedSession: AppSessionState = AppSessionState()
        var authenticatedCredentials: SessionCredentials = SessionCredentials()
        var authenticatedHydratedConversationAccountId: String = ""
        var authenticatedRealtimeReconnectIncrement: Int = 0
        var authFormState: AuthFormState = AuthFormState()

        fun asBoundaryCallbacks(): AppSessionBoundaryCallbacks {
            return AppSessionBoundaryCallbacks(
                onHydratedConversationAccountChanged = { hydratedConversationAccountIds += it },
                onCallSessionStateChanged = { callSessionState = it },
                onWalletStateChanged = { walletState = it },
                onConversationThreadsChanged = { conversationThreadsState = it },
                onAuthenticatedSessionApplied = { session, credentials, hydratedConversationAccountId, reconnectIncrement ->
                    authenticatedSession = session
                    authenticatedCredentials = credentials
                    authenticatedHydratedConversationAccountId = hydratedConversationAccountId
                    authenticatedRealtimeReconnectIncrement = reconnectIncrement
                },
                onAuthFormStateChanged = { authFormState = it },
                onSessionCleared = {},
                onRegisterFormStateChanged = {},
                onContactsStateChanged = {},
                onFriendsChanged = {},
                onFriendRequestStatusReset = {},
            )
        }
    }

    private suspend fun SessionBoundaryHandler.applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
        callbacks: RecordingCallbacks,
    ) {
        applySuccessfulAuthResult(
            authResult = authResult,
            keepSubmitMessageOnAuthForm = keepSubmitMessageOnAuthForm,
            callbacks = callbacks.asBoundaryCallbacks(),
        )
    }

    private class FakeSessionRepository : SessionRepository {
        override suspend fun loadSession(): AppSessionState = AppSessionState()
        override suspend fun loadCredentials(): SessionCredentials = SessionCredentials()
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) = Unit
        override suspend fun clearSession() = Unit
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
