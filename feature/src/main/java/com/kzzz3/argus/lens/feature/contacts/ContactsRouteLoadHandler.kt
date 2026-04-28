package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot

enum class ContactsRouteLoadTarget {
    None,
    Contacts,
    NewFriends,
}

data class ContactsRouteLoadRequest(
    val target: ContactsRouteLoadTarget,
    val isAuthenticated: Boolean,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
)

data class ContactsRouteLoadCallbacks(
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
)

class ContactsRouteLoadHandler(
    private val loadFriends: suspend () -> List<FriendEntry>?,
    private val loadRequests: suspend (FriendRequestsSnapshot) -> FriendRequestStatusState,
) {
    suspend fun loadForTarget(
        request: ContactsRouteLoadRequest,
        callbacks: ContactsRouteLoadCallbacks,
    ) {
        if (!request.isAuthenticated) return

        when (request.target) {
            ContactsRouteLoadTarget.Contacts -> loadFriends()?.let(callbacks.onFriendsChanged)
            ContactsRouteLoadTarget.NewFriends -> callbacks.onFriendRequestStatusChanged(
                loadRequests(request.friendRequestsSnapshot)
            )
            ContactsRouteLoadTarget.None -> Unit
        }
    }
}
