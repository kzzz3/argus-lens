package com.kzzz3.argus.lens.data.conversation

data class RemoteConversationSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
)
