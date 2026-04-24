package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.app.REALTIME_EVENT_HEARTBEAT
import com.kzzz3.argus.lens.app.REALTIME_EVENT_STREAM_READY
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RealtimeCoordinatorTest {
    @Test
    fun classifyEvent_recognizesStreamReadyHeartbeatAndDomainEvents() {
        val coordinator = RealtimeCoordinator(FakeConversationRepository())

        assertEquals(RealtimeEventKind.StreamReady, coordinator.classifyEvent(ConversationRealtimeEvent(eventType = REALTIME_EVENT_STREAM_READY)))
        assertEquals(RealtimeEventKind.Heartbeat, coordinator.classifyEvent(ConversationRealtimeEvent(eventType = REALTIME_EVENT_HEARTBEAT)))
        assertEquals(RealtimeEventKind.DomainEvent, coordinator.classifyEvent(ConversationRealtimeEvent(eventType = "MESSAGE_CREATED")))
    }

    @Test
    fun applyEvent_leavesStateUntouchedForHeartbeat() = runBlocking {
        val state = ConversationThreadsState()
        val coordinator = RealtimeCoordinator(FakeConversationRepository())

        val result = coordinator.applyEvent(
            event = ConversationRealtimeEvent(eventType = REALTIME_EVENT_HEARTBEAT),
            session = AppSessionState(isAuthenticated = true, accountId = "tester", accessToken = "token"),
            currentState = state,
            selectedConversationId = "conv-1",
            currentRoute = AppRoute.Chat,
        )

        assertEquals(state, result)
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
