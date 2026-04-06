package com.kzzz3.argus.lens.feature.inbox

fun createChatUiState(
    state: ChatState,
): ChatUiState {
    return ChatUiState(
        conversationTitle = state.conversationTitle,
        conversationSubtitle = state.conversationSubtitle,
        messages = state.messages,
        draftMessage = state.draftMessage,
        isSendEnabled = state.draftMessage.trim().isNotEmpty(),
        sendActionLabel = "Send",
        backActionLabel = "Back to inbox",
        emptyStateLabel = "No messages yet. Send the first local message in this stage-1 shell.",
    )
}
