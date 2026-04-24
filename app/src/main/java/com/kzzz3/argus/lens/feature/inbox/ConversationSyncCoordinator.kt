package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.conversation.ConversationRepository

suspend fun synchronizeActiveConversation(
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
