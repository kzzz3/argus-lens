package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.data.friend.FriendEntry

fun createContactsUiState(
    state: ContactsState,
    friends: List<FriendEntry>,
    threads: List<InboxConversationThread>,
): ContactsUiState {
    return ContactsUiState(
        title = "Contacts",
        subtitle = "Manage friends, then open or create chats from the local-first IM shell.",
        statusMessage = state.statusMessage,
        isStatusError = state.isStatusError,
        draftConversationName = state.draftConversationName,
        draftFriendAccountId = state.draftFriendAccountId,
        addFriendLabel = "Add remote friend",
        addFriendPlaceholder = "Type a friend account ID",
        addFriendActionLabel = "Add friend",
        isAddFriendEnabled = state.draftFriendAccountId.trim().isNotEmpty(),
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
        contacts = friends.map { friend ->
            val matchingThread = threads.firstOrNull {
                it.title.equals(friend.displayName, ignoreCase = true) ||
                    it.id.equals(friend.accountId, ignoreCase = true)
            }
            ContactEntryUiState(
                conversationId = matchingThread?.id ?: friend.accountId,
                accountId = friend.accountId,
                displayName = friend.displayName,
                supportingLabel = friend.note.ifBlank { "Remote friend" },
                lastSeenPreview = matchingThread?.messages?.lastOrNull()?.body ?: "No local messages yet",
            )
        },
        backActionLabel = "Back to inbox",
    )
}
