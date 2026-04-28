package com.kzzz3.argus.lens.model.realtime

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
    val kind: ConversationRealtimeEventKind = ConversationRealtimeEventKind.fromBackendValue(eventType),
    val messageId: String? = null,
    val message: ConversationRealtimeMessagePayload? = null,
    val occurredAt: String? = null,
    val replayed: Boolean = false,
)

enum class ConversationRealtimeEventKind(
    val backendValue: String,
) {
    StreamReady("STREAM_READY"),
    Heartbeat("HEARTBEAT"),
    MessageCreated("MESSAGE_CREATED"),
    MessageStatusUpdated("MESSAGE_STATUS_UPDATED"),
    MessageRecalled("MESSAGE_RECALLED"),
    ConversationRead("CONVERSATION_READ"),
    ConversationCreated("CONVERSATION_CREATED"),
    ConversationUpdated("CONVERSATION_UPDATED"),
    Unknown("");

    val isDomainEvent: Boolean
        get() = when (this) {
            MessageCreated,
            MessageStatusUpdated,
            MessageRecalled,
            ConversationRead,
            ConversationCreated,
            ConversationUpdated,
            -> true

            StreamReady,
            Heartbeat,
            Unknown,
            -> false
        }

    companion object {
        fun fromBackendValue(value: String): ConversationRealtimeEventKind {
            return entries.firstOrNull { it.backendValue == value } ?: Unknown
        }
    }
}
