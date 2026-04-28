package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEventKind
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ApplyRealtimeConversationEventUseCaseTest {
    @Test
    fun classifyEvent_recognizesStreamReadyHeartbeatAndDomainEvents() {
        val useCase = ApplyRealtimeConversationEventUseCase(FakeConversationRepository())

        assertEquals(RealtimeEventKind.StreamReady, useCase.classifyEvent(ConversationRealtimeEvent(kind = ConversationRealtimeEventKind.StreamReady)))
        assertEquals(RealtimeEventKind.Heartbeat, useCase.classifyEvent(ConversationRealtimeEvent(kind = ConversationRealtimeEventKind.Heartbeat)))
        assertEquals(RealtimeEventKind.DomainEvent, useCase.classifyEvent(ConversationRealtimeEvent(kind = ConversationRealtimeEventKind.MessageCreated)))
    }

    @Test
    fun applyEvent_leavesStateUntouchedForHeartbeat() = runBlocking {
        val state = ConversationThreadsState()
        val useCase = ApplyRealtimeConversationEventUseCase(FakeConversationRepository())

        val result = useCase.applyEvent(
            event = ConversationRealtimeEvent(kind = ConversationRealtimeEventKind.Heartbeat),
            session = AppSessionState(isAuthenticated = true, accountId = "tester"),
            currentState = state,
            activeChatConversationId = "conv-1",
            isChatRouteActive = true,
        )

        assertEquals(state, result)
    }

    @Test
    fun eventKind_mapsRawBackendStrings() {
        assertEquals(ConversationRealtimeEventKind.StreamReady, ConversationRealtimeEventKind.fromBackendValue("STREAM_READY"))
        assertEquals(ConversationRealtimeEventKind.MessageStatusUpdated, ConversationRealtimeEventKind.fromBackendValue("MESSAGE_STATUS_UPDATED"))
        assertEquals(ConversationRealtimeEventKind.Unknown, ConversationRealtimeEventKind.fromBackendValue("UNEXPECTED"))
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
