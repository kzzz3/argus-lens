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
        draftLabel = "New local conversation",
        draftPlaceholder = "Type a contact or chat title",
        createConversationActionLabel = "Create chat",
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
