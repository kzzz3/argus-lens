package com.kzzz3.argus.lens.data.realtime

data class ConversationRealtimeMessagePayload(
    val id: String = "",
    val conversationId: String = "",
    val senderDisplayName: String = "",
    val body: String = "",
    val timestampLabel: String = "",
    val deliveryStatus: String = "",
    val statusUpdatedAt: String = "",
)

data class ConversationRealtimeEvent(
    val eventId: String = "",
    val conversationId: String = "",
    val eventType: String = "",
    val messageId: String? = null,
    val message: ConversationRealtimeMessagePayload? = null,
    val occurredAt: String? = null,
    val replayed: Boolean = false,
)
