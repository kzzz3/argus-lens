package com.kzzz3.argus.lens.feature.inbox

data class InboxConversationItem(
    val id: String,
    val title: String,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
)

data class InboxPlaceholderUiState(
    val title: String,
    val subtitle: String,
    val sessionLabel: String,
    val sessionSummary: String,
    val conversations: List<InboxConversationItem>,
    val primaryActionLabel: String,
)

data class ChatPlaceholderUiState(
    val conversationTitle: String,
    val conversationSubtitle: String,
    val messagePreview: String,
    val primaryActionLabel: String,
)
