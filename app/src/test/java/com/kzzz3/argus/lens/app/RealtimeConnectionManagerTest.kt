package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.ApplyRealtimeConversationEventUseCase
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionCallbacks
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionManager
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionRequest
import com.kzzz3.argus.lens.feature.realtime.RealtimeReconnectScheduler
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RealtimeConnectionManagerTest {
    @Test
    fun connect_setsConnectingAndSchedulesSessionRefreshWhenConnected() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val realtimeClient = FakeConversationRealtimeClient()
        val reconnectScheduler = RealtimeReconnectScheduler(
            scope = scope,
            delayMillisForAttempt = { 10_000L },
        )
        val manager = RealtimeConnectionManager(
            scope = scope,
            realtimeClient = realtimeClient,
            applyRealtimeConversationEvent = ApplyRealtimeConversationEventUseCase(FakeConversationRepository()),
            reconnectScheduler = reconnectScheduler,
        )
        val callbacks = RecordingCallbacks()

        manager.connect(
            request = RealtimeConnectionRequest(
                isAuthenticated = true,
                accountId = "tester",
                credentials = SessionCredentials("access-token", "refresh-token"),
                lastEventId = "event-1",
                reconnectGeneration = 0,
                getSession = { AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester") },
                getConversationThreadsState = { ConversationThreadsState() },
                getActiveChatConversationId = { "" },
                isChatRouteActive = { false },
            ),
            callbacks = callbacks.asCallbacks(),
        )
        realtimeClient.emitConnected()
        delay(50)

        assertEquals("access-token", realtimeClient.accessToken)
        assertEquals("event-1", realtimeClient.lastEventId)
        assertEquals(
            listOf(
                ConversationRealtimeConnectionState.CONNECTING,
                ConversationRealtimeConnectionState.LIVE,
            ),
            callbacks.connectionStates,
        )
        assertEquals(1, callbacks.sessionRefreshLoopCount)
        assertEquals(0, reconnectScheduler.currentAttempt)
        manager.dispose(callbacks.asCallbacks())
        scope.cancel()
    }

    private class RecordingCallbacks {
        val connectionStates = mutableListOf<ConversationRealtimeConnectionState>()
        var sessionRefreshLoopCount: Int = 0

        fun asCallbacks(): RealtimeConnectionCallbacks {
            return RealtimeConnectionCallbacks(
                onConnectionStateChanged = { connectionStates += it },
                onEventIdRecorded = {},
                onLastEventIdReset = {},
                onConversationThreadsChanged = {},
                onReconnectGenerationIncremented = {},
                onScheduleSessionRefreshLoop = { sessionRefreshLoopCount += 1 },
                onCancelSessionRefreshLoop = {},
                refreshSessionTokens = { SessionRefreshOutcome.Failure(isUnauthorized = false) },
                signOutToEntry = {},
            )
        }
    }

    private class FakeConversationRealtimeClient : ConversationRealtimeClient {
        var accessToken: String = ""
        var lastEventId: String? = null
        private var onConnected: () -> Unit = {}
        private var onClosed: () -> Unit = {}
        private var onEvent: (ConversationRealtimeEvent) -> Unit = {}
        private var onError: (Throwable) -> Unit = {}

        override fun connect(
            accessToken: String,
            lastEventId: String?,
            onConnected: () -> Unit,
            onClosed: () -> Unit,
            onEvent: (ConversationRealtimeEvent) -> Unit,
            onError: (Throwable) -> Unit,
        ): ConversationRealtimeSubscription {
            this.accessToken = accessToken
            this.lastEventId = lastEventId
            this.onConnected = onConnected
            this.onClosed = onClosed
            this.onEvent = onEvent
            this.onError = onError
            return FakeConversationRealtimeSubscription()
        }

        fun emitConnected() = onConnected()
    }

    private class FakeConversationRealtimeSubscription : ConversationRealtimeSubscription {
        override fun close() = Unit
    }

    private class FakeConversationRepository : ConversationRepository {
        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
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
