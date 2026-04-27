package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState

internal data class AppRouteLoadRequest(
    val route: AppRoute,
    val isAuthenticated: Boolean,
    val friendRequestsSnapshot: FriendRequestsSnapshot,
)

internal data class AppRouteLoadCallbacks(
    val onFriendsChanged: (List<FriendEntry>) -> Unit,
    val onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
)

internal class AppRouteLoadRuntime(
    private val loadFriends: suspend () -> List<FriendEntry>?,
    private val loadRequests: suspend (FriendRequestsSnapshot) -> FriendRequestStatusState,
) {
    suspend fun loadForRoute(
        request: AppRouteLoadRequest,
        callbacks: AppRouteLoadCallbacks,
    ) {
        if (!request.isAuthenticated) return

        if (request.route == AppRoute.Contacts) {
            loadFriends()?.let(callbacks.onFriendsChanged)
        }
        if (request.route == AppRoute.NewFriends) {
            callbacks.onFriendRequestStatusChanged(loadRequests(request.friendRequestsSnapshot))
        }
    }
}
