package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.data.conversation.buildDirectConversationId
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.ui.status.UiStatusMessage

data class FriendRequestStatusState(
    val snapshot: FriendRequestsSnapshot,
    val status: UiStatusMessage?,
) {
    val message: String?
        get() = status?.text

    val isError: Boolean
        get() = status?.isError ?: false
}

data class DirectConversationTarget(
    val conversationId: String,
    val requiresRefresh: Boolean,
    val requiresPlaceholder: Boolean,
    val placeholderTitle: String,
)

fun createContactsStatusUpdate(
    currentState: ContactsState,
    message: String,
    isError: Boolean,
): ContactsState {
    return currentState.copy(
        statusMessage = message,
        isStatusError = isError,
    )
}

fun createFriendRequestStatusState(
    snapshot: FriendRequestsSnapshot,
    message: String?,
    isError: Boolean,
): FriendRequestStatusState {
    return FriendRequestStatusState(
        snapshot = snapshot,
        status = message?.let { text ->
            if (isError) UiStatusMessage.error(text) else UiStatusMessage.success(text)
        },
    )
}

fun resolveDirectConversationTarget(
    currentAccountId: String,
    requestedConversationId: String,
    friends: List<FriendEntry>,
    existingThreadIds: Set<String>,
): DirectConversationTarget {
    if (requestedConversationId in existingThreadIds) {
        return DirectConversationTarget(
            conversationId = requestedConversationId,
            requiresRefresh = false,
            requiresPlaceholder = false,
            placeholderTitle = requestedConversationId,
        )
    }

    val matchingFriend = friends.firstOrNull { friend ->
        friend.accountId == requestedConversationId ||
            buildDirectConversationId(currentAccountId, friend.accountId) == requestedConversationId
    }
    val preferredConversationId = matchingFriend?.let { friend ->
        buildDirectConversationId(currentAccountId, friend.accountId)
    }

    return if (matchingFriend != null && preferredConversationId != null) {
        DirectConversationTarget(
            conversationId = preferredConversationId,
            requiresRefresh = true,
            requiresPlaceholder = false,
            placeholderTitle = matchingFriend.displayName,
        )
    } else {
        DirectConversationTarget(
            conversationId = requestedConversationId,
            requiresRefresh = false,
            requiresPlaceholder = true,
            placeholderTitle = requestedConversationId,
        )
    }
}

fun ensureDirectConversationPlaceholder(
    state: ConversationThreadsState,
    conversationId: String,
    title: String,
): ConversationThreadsState {
    if (state.threads.any { it.id == conversationId }) {
        return state
    }
    return state.copy(
        threads = listOf(
            InboxConversationThread(
                id = conversationId,
                title = title,
                subtitle = "Direct friend conversation",
                unreadCount = 0,
                messages = emptyList(),
            )
        ) + state.threads
    )
}
