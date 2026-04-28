package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.core.data.conversation.buildDirectConversationId
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.core.data.friend.FriendEntry

fun createContactsUiState(
    state: ContactsState,
    friends: List<FriendEntry>,
    threads: List<InboxConversationThread>,
    currentAccountId: String,
): ContactsUiState {
    return ContactsUiState(
        title = "Contacts",
        subtitle = "Manage friends and open the one direct chat linked to each contact.",
        statusMessage = state.statusMessage,
        isStatusError = state.isStatusError,
        newFriendsLabel = "New Friends",
        newFriendsSubtitle = "Review incoming requests and track requests you have sent.",
        draftFriendAccountId = state.draftFriendAccountId,
        addFriendLabel = "Add remote friend",
        addFriendPlaceholder = "Type a friend account ID",
        addFriendActionLabel = "Add friend",
        isAddFriendEnabled = state.draftFriendAccountId.trim().isNotEmpty(),
        contacts = friends.map { friend ->
            val preferredConversationId = buildDirectConversationId(
                currentAccountId = currentAccountId,
                friendAccountId = friend.accountId,
            )
            val matchingThread = threads.firstOrNull {
                it.id.equals(preferredConversationId, ignoreCase = true) ||
                    it.id.equals(friend.accountId, ignoreCase = true)
            }
            ContactEntryUiState(
                conversationId = matchingThread?.id ?: preferredConversationId,
                accountId = friend.accountId,
                displayName = friend.displayName,
                supportingLabel = friend.note.ifBlank { "Remote friend" },
                lastSeenPreview = matchingThread?.messages?.lastOrNull()?.body ?: "No local messages yet",
            )
        },
        backActionLabel = "Back to inbox",
    )
}
