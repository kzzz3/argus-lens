package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class ContactsRouteRequest(
    val session: AppSessionState,
    val contactsState: ContactsState,
    val friends: List<FriendEntry>,
    val conversationThreadsState: ConversationThreadsState,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
)

data class ContactsOpenConversationResult(
    val conversationThreadsState: ConversationThreadsState,
    val conversationId: String,
)

data class ContactsRouteCallbacks(
    val onOpenNewFriends: () -> Unit,
    val onNavigateBackToInbox: () -> Unit,
    val onNavigateBackToContacts: () -> Unit,
    val onContactsStateChanged: (ContactsState) -> Unit,
    val onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onConversationOpened: (String) -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
)

class ContactsRouteHandler(
    private val scope: CoroutineScope,
    private val openConversation: suspend (ContactsRouteRequest, String) -> ContactsOpenConversationResult,
    private val addFriend: suspend (ContactsState, String) -> AddFriendResult,
    private val acceptFriendRequest: suspend (String, AppSessionState, FriendRequestsSnapshot, ConversationThreadsState) -> NewFriendsActionResult,
    private val rejectFriendRequest: suspend (String, FriendRequestsSnapshot) -> NewFriendsActionResult,
    private val ignoreFriendRequest: suspend (String, FriendRequestsSnapshot) -> NewFriendsActionResult,
) {
    fun handleContactsEffect(
        effect: ContactsEffect?,
        request: ContactsRouteRequest,
        callbacks: ContactsRouteCallbacks,
    ) {
        when (effect) {
            is ContactsEffect.OpenConversation -> scope.launch {
                val result = openConversation(request, effect.conversationId)
                callbacks.onConversationThreadsChanged(result.conversationThreadsState)
                callbacks.onConversationOpened(result.conversationId)
            }
            is ContactsEffect.AddFriend -> scope.launch {
                val result = addFriend(request.contactsState, effect.friendAccountId)
                callbacks.onContactsStateChanged(result.contactsState)
                result.friendRequestsSnapshot?.let(callbacks.onFriendRequestsSnapshotChanged)
            }
            ContactsEffect.OpenNewFriends -> callbacks.onOpenNewFriends()
            ContactsEffect.NavigateBackToInbox -> callbacks.onNavigateBackToInbox()
            null -> Unit
        }
    }

    fun handleNewFriendsAction(
        action: NewFriendsAction,
        request: ContactsRouteRequest,
        callbacks: ContactsRouteCallbacks,
    ) {
        when (action) {
            NewFriendsAction.NavigateBack -> callbacks.onNavigateBackToContacts()
            is NewFriendsAction.Accept -> scope.launch {
                callbacks.applyNewFriendsResult(
                    acceptFriendRequest(
                        action.requestId,
                        request.session,
                        request.friendRequestsSnapshot,
                        request.conversationThreadsState,
                    )
                )
            }
            is NewFriendsAction.Reject -> scope.launch {
                callbacks.applyNewFriendsResult(
                    rejectFriendRequest(action.requestId, request.friendRequestsSnapshot)
                )
            }
            is NewFriendsAction.Ignore -> scope.launch {
                callbacks.applyNewFriendsResult(
                    ignoreFriendRequest(action.requestId, request.friendRequestsSnapshot)
                )
            }
        }
    }

    private fun ContactsRouteCallbacks.applyNewFriendsResult(result: NewFriendsActionResult) {
        onFriendRequestStatusChanged(result.status)
        result.friends?.let(onFriendsChanged)
        result.conversationThreadsState?.let(onConversationThreadsChanged)
    }
}
