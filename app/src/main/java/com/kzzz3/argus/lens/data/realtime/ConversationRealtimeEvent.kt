package com.kzzz3.argus.lens.data.realtime

data class ConversationRealtimeEvent(
    val conversationId: String = "",
    val eventType: String = "",
    val messageId: String? = null,
    val occurredAt: String? = null,
)
