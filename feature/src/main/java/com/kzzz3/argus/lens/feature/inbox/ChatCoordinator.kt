package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.media.MediaRepository

class ChatCoordinator(
    private val conversationRepository: ConversationRepository,
    private val mediaRepository: MediaRepository,
    private val attachmentDownloader: ChatAttachmentDownloader = ChatAttachmentDownloader(mediaRepository),
) {
    fun openConversation(
        state: ConversationThreadsState,
        conversationId: String,
    ): OpenInboxConversationResult {
        return OpenInboxConversationResult(
            conversationThreadsState = conversationRepository.markConversationAsRead(
                state = state,
                conversationId = conversationId,
            ),
            conversationId = conversationId,
        )
    }

    suspend fun synchronizeConversation(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        return synchronizeActiveConversation(
            state = state,
            conversationId = conversationId,
            conversationRepository = conversationRepository,
        )
    }

    fun reduceAction(
        currentThreadsState: ConversationThreadsState,
        currentChatState: ChatState,
        action: ChatAction,
    ): ChatActionResult {
        val reducerResult = reduceChatState(
            currentState = currentChatState,
            action = action,
        )
        return ChatActionResult(
            conversationThreadsState = conversationRepository.updateConversationFromChatState(
                state = currentThreadsState,
                updatedState = reducerResult.state,
            ),
            chatState = reducerResult.state,
            effect = reducerResult.effect,
        )
    }

    suspend fun dispatchOutgoingMessages(
        state: ConversationThreadsState,
        conversationId: String,
        messages: List<ChatMessageItem>,
    ): ChatDispatchResult {
        var nextState = state
        val dispatchResults = mutableListOf<OutgoingDispatchResult>()
        messages.forEach { message ->
            val sendResult = dispatchOutgoingChatMessage(
                state = nextState,
                conversationId = conversationId,
                message = message,
                conversationRepository = conversationRepository,
                mediaRepository = mediaRepository,
            )
            dispatchResults += OutgoingDispatchResult(
                state = sendResult.state,
                failureMessage = sendResult.failureMessage,
            )
            nextState = sendResult.state
        }
        return ChatDispatchResult(
            conversationThreadsState = nextState,
            summary = summarizeOutgoingDispatch(dispatchResults),
        )
    }

    suspend fun downloadAttachment(
        attachmentId: String,
        fileName: String,
    ): ChatStatusResult {
        val result = attachmentDownloader.downloadAttachment(
            attachmentId = attachmentId,
            fileName = fileName,
        )
        return ChatStatusResult(result.message, result.isError)
    }

    suspend fun recallMessage(
        state: ConversationThreadsState,
        chatState: ChatState,
        messageId: String,
    ): ConversationThreadsState {
        if (!chatState.canRecallMessage(messageId)) return state
        return conversationRepository.recallMessage(
            state = state,
            conversationId = chatState.conversationId,
            messageId = messageId,
        )
    }
}

data class OpenInboxConversationResult(
    val conversationThreadsState: ConversationThreadsState,
    val conversationId: String,
)

data class ChatActionResult(
    val conversationThreadsState: ConversationThreadsState,
    val chatState: ChatState,
    val effect: ChatEffect?,
)

data class ChatDispatchResult(
    val conversationThreadsState: ConversationThreadsState,
    val summary: OutgoingDispatchResult?,
)

data class ChatStatusResult(
    val message: String?,
    val isError: Boolean,
)

fun summarizeOutgoingDispatch(
    results: List<OutgoingDispatchResult>,
): OutgoingDispatchResult? {
    if (results.isEmpty()) return null
    return OutgoingDispatchResult(
        state = results.last().state,
        failureMessage = results.firstNotNullOfOrNull { it.failureMessage },
    )
}

private fun ChatState.canRecallMessage(messageId: String): Boolean {
    return messages.any { message ->
        message.id == messageId &&
            message.isFromCurrentUser &&
            (message.deliveryStatus == ChatMessageDeliveryStatus.Sent ||
                message.deliveryStatus == ChatMessageDeliveryStatus.Delivered)
    }
}

