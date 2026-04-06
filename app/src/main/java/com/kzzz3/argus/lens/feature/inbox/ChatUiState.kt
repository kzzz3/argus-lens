package com.kzzz3.argus.lens.feature.inbox

data class ChatUiState(
    val conversationTitle: String,
    val conversationSubtitle: String,
    val messages: List<ChatMessageItem>,
    val draftMessage: String,
    val isSendEnabled: Boolean,
    val sendActionLabel: String,
    val backActionLabel: String,
    val emptyStateLabel: String,
)
