package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEventKind
import com.kzzz3.argus.lens.feature.inbox.synchronizeActiveConversation
import com.kzzz3.argus.lens.model.session.AppSessionState

class ApplyRealtimeConversationEventUseCase(
    private val conversationRepository: ConversationRepository,
) {
    fun classifyEvent(event: ConversationRealtimeEvent): RealtimeEventKind {
        return when (event.kind) {
            ConversationRealtimeEventKind.StreamReady -> RealtimeEventKind.StreamReady
            ConversationRealtimeEventKind.Heartbeat -> RealtimeEventKind.Heartbeat
            else -> RealtimeEventKind.DomainEvent
        }
    }

    suspend fun applyEvent(
        event: ConversationRealtimeEvent,
        session: AppSessionState,
        currentState: ConversationThreadsState,
        activeChatConversationId: String,
        isChatRouteActive: Boolean,
    ): ConversationThreadsState {
        if (session.accountId.isBlank()) return currentState
        if (!event.kind.isDomainEvent) return currentState

        val refreshedState = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = session.displayName,
        )
        return if (event.conversationId.isNotBlank() && isChatRouteActive && activeChatConversationId == event.conversationId) {
            synchronizeActiveConversation(
                state = refreshedState,
                conversationId = event.conversationId,
                conversationRepository = conversationRepository,
            )
        } else {
            refreshedState
        }
    }
}

enum class RealtimeEventKind {
    StreamReady,
    Heartbeat,
    DomainEvent,
}
