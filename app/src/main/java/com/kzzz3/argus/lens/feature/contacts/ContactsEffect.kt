package com.kzzz3.argus.lens.feature.contacts

sealed interface ContactsEffect {
    data class CreateConversation(
        val displayName: String,
        val mode: ConversationCreationMode,
    ) : ContactsEffect
    data object NavigateBackToInbox : ContactsEffect
    data class OpenConversation(val conversationId: String) : ContactsEffect
}
