package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.app.REALTIME_EVENT_HEARTBEAT
import com.kzzz3.argus.lens.app.REALTIME_EVENT_STREAM_READY
import com.kzzz3.argus.lens.app.handleConversationRealtimeEvent
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState

class RealtimeCoordinator(
    private val conversationRepository: ConversationRepository,
) {
    fun classifyEvent(event: ConversationRealtimeEvent): RealtimeEventKind {
        return when (event.eventType) {
            REALTIME_EVENT_STREAM_READY -> RealtimeEventKind.StreamReady
            REALTIME_EVENT_HEARTBEAT -> RealtimeEventKind.Heartbeat
            else -> RealtimeEventKind.DomainEvent
        }
    }

    suspend fun applyEvent(
        event: ConversationRealtimeEvent,
        session: AppSessionState,
        currentState: ConversationThreadsState,
        selectedConversationId: String,
        currentRoute: AppRoute,
    ): ConversationThreadsState {
        return handleConversationRealtimeEvent(
            event = event,
            conversationRepository = conversationRepository,
            session = session,
            currentState = currentState,
            selectedConversationId = selectedConversationId,
            isChatRouteActive = currentRoute == AppRoute.Chat,
        )
    }
}

enum class RealtimeEventKind {
    StreamReady,
    Heartbeat,
    DomainEvent,
}
