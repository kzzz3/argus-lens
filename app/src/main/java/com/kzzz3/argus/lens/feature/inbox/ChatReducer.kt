package com.kzzz3.argus.lens.feature.inbox

data class ChatReducerResult(
    val state: ChatState,
    val effect: ChatEffect?,
)

fun reduceChatState(
    currentState: ChatState,
    action: ChatAction,
): ChatReducerResult {
    return when (action) {
        ChatAction.NavigateBackToInbox -> ChatReducerResult(
            state = currentState,
            effect = ChatEffect.NavigateBackToInbox,
        )

        ChatAction.SendMessage -> {
            val trimmedDraft = currentState.draftMessage.trim()
            if (trimmedDraft.isEmpty()) {
                ChatReducerResult(
                    state = currentState,
                    effect = null,
                )
            } else {
                val nextMessage = ChatMessageItem(
                    id = "${currentState.conversationId}-local-${currentState.messages.size + 1}",
                    senderDisplayName = currentState.currentUserDisplayName,
                    body = trimmedDraft,
                    timestampLabel = "Now",
                    isFromCurrentUser = true,
                )
                ChatReducerResult(
                    state = currentState.copy(
                        messages = currentState.messages + nextMessage,
                        draftMessage = "",
                    ),
                    effect = null,
                )
            }
        }

        is ChatAction.UpdateDraftMessage -> ChatReducerResult(
            state = currentState.copy(draftMessage = action.value),
            effect = null,
        )
    }
}
