package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread

internal suspend fun synchronizeActiveConversation(
    state: ConversationThreadsState,
    conversationId: String,
    conversationRepository: ConversationRepository,
    refreshDetail: Boolean = true,
): ConversationThreadsState {
    var nextState = conversationRepository.markConversationAsRead(
        state = state,
        conversationId = conversationId,
    )
    if (refreshDetail) {
        nextState = conversationRepository.refreshConversationDetail(
            state = nextState,
            conversationId = conversationId,
        )
    }
    nextState = conversationRepository.markConversationReadRemote(
        state = nextState,
        conversationId = conversationId,
    )
    nextState = conversationRepository.refreshConversationMessages(
        state = nextState,
        conversationId = conversationId,
    )
    return acknowledgeVisibleRemoteMessagesAsRead(
        state = nextState,
        conversationId = conversationId,
        conversationRepository = conversationRepository,
    )
}

internal suspend fun handleConversationRealtimeEvent(
    event: ConversationRealtimeEvent,
    conversationRepository: ConversationRepository,
    session: AppSessionState,
    currentState: ConversationThreadsState,
    selectedConversationId: String,
    isChatRouteActive: Boolean,
): ConversationThreadsState {
    if (session.accountId.isBlank()) return currentState

    return when (event.eventType) {
        REALTIME_EVENT_STREAM_READY,
        REALTIME_EVENT_HEARTBEAT -> currentState

        REALTIME_EVENT_MESSAGE_CREATED,
        REALTIME_EVENT_MESSAGE_STATUS_UPDATED,
        REALTIME_EVENT_MESSAGE_RECALLED,
        REALTIME_EVENT_CONVERSATION_READ,
        REALTIME_EVENT_CONVERSATION_CREATED,
        REALTIME_EVENT_CONVERSATION_UPDATED -> {
            val refreshedState = conversationRepository.loadOrCreateConversationThreads(
                accountId = session.accountId,
                currentUserDisplayName = session.displayName,
            )
            if (event.conversationId.isBlank()) {
                refreshedState
            } else if (isChatRouteActive && selectedConversationId == event.conversationId) {
                synchronizeActiveConversation(
                    state = refreshedState,
                    conversationId = event.conversationId,
                    conversationRepository = conversationRepository,
                )
            } else {
                refreshedState
            }
        }

        else -> currentState
    }
}

internal fun ensureDirectConversationPlaceholder(
    state: ConversationThreadsState,
    conversationId: String,
    title: String,
): ConversationThreadsState {
    if (state.threads.any { it.id == conversationId }) {
        return state
    }
    return state.copy(
        threads = listOf(
            InboxConversationThread(
                id = conversationId,
                title = title,
                subtitle = "Direct friend conversation",
                unreadCount = 0,
                messages = emptyList(),
            )
        ) + state.threads
    )
}

private suspend fun acknowledgeVisibleRemoteMessagesAsRead(
    state: ConversationThreadsState,
    conversationId: String,
    conversationRepository: ConversationRepository,
): ConversationThreadsState {
    val visibleRemoteMessageIds = state.threads
        .firstOrNull { it.id == conversationId }
        ?.messages
        ?.filter { message ->
            !message.isFromCurrentUser &&
                message.deliveryStatus != ChatMessageDeliveryStatus.Read &&
                message.deliveryStatus != ChatMessageDeliveryStatus.Recalled
        }
        ?.map { it.id }
        .orEmpty()

    return visibleRemoteMessageIds.fold(state) { currentState, messageId ->
        conversationRepository.acknowledgeMessageRead(
            state = currentState,
            conversationId = conversationId,
            messageId = messageId,
        )
    }
}

internal const val REALTIME_EVENT_STREAM_READY = "STREAM_READY"
internal const val REALTIME_EVENT_HEARTBEAT = "HEARTBEAT"
internal const val REALTIME_EVENT_MESSAGE_CREATED = "MESSAGE_CREATED"
internal const val REALTIME_EVENT_MESSAGE_STATUS_UPDATED = "MESSAGE_STATUS_UPDATED"
internal const val REALTIME_EVENT_MESSAGE_RECALLED = "MESSAGE_RECALLED"
internal const val REALTIME_EVENT_CONVERSATION_READ = "CONVERSATION_READ"
internal const val REALTIME_EVENT_CONVERSATION_CREATED = "CONVERSATION_CREATED"
internal const val REALTIME_EVENT_CONVERSATION_UPDATED = "CONVERSATION_UPDATED"


