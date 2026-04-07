package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread

fun createContactsUiState(
    state: ContactsState,
    threads: List<InboxConversationThread>,
): ContactsUiState {
    return ContactsUiState(
        title = "Contacts",
        subtitle = "Start a conversation from your current local thread roster.",
        draftConversationName = state.draftConversationName,
        draftLabel = if (state.creationMode == ConversationCreationMode.Group) {
            "New local group"
        } else {
            "New local conversation"
        },
        draftPlaceholder = if (state.creationMode == ConversationCreationMode.Group) {
            "Type a group name"
        } else {
            "Type a contact or chat title"
        },
        creationModeLabel = if (state.creationMode == ConversationCreationMode.Group) {
            "Group mode"
        } else {
            "Direct mode"
        },
        toggleCreationModeActionLabel = if (state.creationMode == ConversationCreationMode.Group) {
            "Switch to direct"
        } else {
            "Switch to group"
        },
        createConversationActionLabel = if (state.creationMode == ConversationCreationMode.Group) {
            "Create group"
        } else {
            "Create chat"
        },
        isCreateConversationEnabled = state.draftConversationName.trim().isNotEmpty(),
        contacts = threads.map { thread ->
            ContactEntryUiState(
                conversationId = thread.id,
                displayName = thread.title,
                supportingLabel = thread.subtitle,
                lastSeenPreview = thread.messages.lastOrNull()?.body ?: "No local messages yet",
            )
        },
        backActionLabel = "Back to inbox",
    )
}
