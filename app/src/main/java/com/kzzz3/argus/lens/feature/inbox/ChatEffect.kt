package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatEffect {
    data object NavigateBackToInbox : ChatEffect
    data class StartCall(
        val conversationId: String,
        val contactDisplayName: String,
        val mode: ChatCallMode,
    ) : ChatEffect
    data class DispatchOutgoingMessages(
        val conversationId: String,
        val messageIds: List<String>,
    ) : ChatEffect
}

enum class ChatCallMode {
    Audio,
    Video,
}
