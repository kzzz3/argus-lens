package com.kzzz3.argus.lens.feature.inbox

data class InboxUiState(
    val title: String,
    val subtitle: String,
    val shellStatusLabel: String,
    val sessionLabel: String,
    val sessionSummary: String,
    val conversations: List<InboxConversationItem>,
)
