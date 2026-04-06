package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatEffect {
    data object NavigateBackToInbox : ChatEffect
    data class DispatchOutgoingMessages(
        val conversationId: String,
        val messageIds: List<String>,
    ) : ChatEffect
}
