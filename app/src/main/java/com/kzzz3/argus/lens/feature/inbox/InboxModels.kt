package com.kzzz3.argus.lens.feature.inbox

data class InboxConversationThread(
    val id: String,
    val title: String,
    val subtitle: String,
    val unreadCount: Int,
    val messages: List<ChatMessageItem>,
    val draftMessage: String = "",
)

data class InboxConversationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
)

data class ChatMessageItem(
    val id: String,
    val senderDisplayName: String,
    val body: String,
    val timestampLabel: String,
    val isFromCurrentUser: Boolean,
)
