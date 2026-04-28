package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

data class ContactsFeatureSnapshot(
    val session: AppSessionState,
    val contactsState: ContactsState,
    val friends: List<FriendEntry>,
    val conversationThreadsState: ConversationThreadsState,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
)

data class ContactsFeatureCallbacks(
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

class ContactsFeatureController(
    private val routeHandler: ContactsRouteHandler,
    private val reduceAction: (ContactsState, ContactsAction) -> ContactsReducerResult = ::reduceContactsState,
) {
    fun handleContactsAction(
        action: ContactsAction,
        snapshot: ContactsFeatureSnapshot,
        callbacks: ContactsFeatureCallbacks,
    ) {
        val result = reduceAction(snapshot.contactsState, action)
        callbacks.onContactsStateChanged(result.state)
        routeHandler.handleContactsEffect(
            effect = result.effect,
            request = snapshot.toRouteRequest(),
            callbacks = callbacks.toRouteCallbacks(),
        )
    }

    fun handleNewFriendsAction(
        action: NewFriendsAction,
        snapshot: ContactsFeatureSnapshot,
        callbacks: ContactsFeatureCallbacks,
    ) {
        routeHandler.handleNewFriendsAction(
            action = action,
            request = snapshot.toRouteRequest(),
            callbacks = callbacks.toRouteCallbacks(),
        )
    }
}

private fun ContactsFeatureSnapshot.toRouteRequest(): ContactsRouteRequest {
    return ContactsRouteRequest(
        session = session,
        contactsState = contactsState,
        friends = friends,
        conversationThreadsState = conversationThreadsState,
        friendRequestsSnapshot = friendRequestsSnapshot,
    )
}

private fun ContactsFeatureCallbacks.toRouteCallbacks(): ContactsRouteCallbacks {
    return ContactsRouteCallbacks(
        onOpenNewFriends = onOpenNewFriends,
        onNavigateBackToInbox = onNavigateBackToInbox,
        onNavigateBackToContacts = onNavigateBackToContacts,
        onContactsStateChanged = onContactsStateChanged,
        onFriendRequestsSnapshotChanged = onFriendRequestsSnapshotChanged,
        onConversationThreadsChanged = onConversationThreadsChanged,
        onConversationOpened = onConversationOpened,
        onFriendRequestStatusChanged = onFriendRequestStatusChanged,
        onFriendsChanged = onFriendsChanged,
    )
}
