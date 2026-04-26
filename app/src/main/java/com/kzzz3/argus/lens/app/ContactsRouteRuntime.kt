package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.AddFriendResult
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsActionResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal data class ContactsRouteRequest(
    val session: AppSessionState,
    val contactsState: ContactsState,
    val friends: List<FriendEntry>,
    val conversationThreadsState: ConversationThreadsState,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
)

internal data class ContactsOpenConversationResult(
    val conversationThreadsState: ConversationThreadsState,
    val conversationId: String,
)

internal data class ContactsRouteCallbacks(
    val onRouteChanged: (AppRoute) -> Unit,
    val onContactsStateChanged: (ContactsState) -> Unit,
    val onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onConversationOpened: (String) -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
)

internal class ContactsRouteRuntime(
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
            ContactsEffect.OpenNewFriends -> callbacks.onRouteChanged(AppRoute.NewFriends)
            ContactsEffect.NavigateBackToInbox -> callbacks.onRouteChanged(AppRoute.Inbox)
            null -> Unit
        }
    }

    fun handleNewFriendsAction(
        action: NewFriendsAction,
        request: ContactsRouteRequest,
        callbacks: ContactsRouteCallbacks,
    ) {
        when (action) {
            NewFriendsAction.NavigateBack -> callbacks.onRouteChanged(AppRoute.Contacts)
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
