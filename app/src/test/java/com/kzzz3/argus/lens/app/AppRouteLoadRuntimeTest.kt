package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.createFriendRequestStatusState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteLoadRuntimeTest {
    @Test
    fun loadForRoute_contactsWhenAuthenticatedPublishesLoadedFriends() = runBlocking {
        val loadedFriends = listOf(FriendEntry("alice", "Alice", "direct"))
        var loadFriendsCount = 0
        var publishedFriends: List<FriendEntry>? = null
        val runtime = AppRouteLoadRuntime(
            loadFriends = {
                loadFriendsCount += 1
                loadedFriends
            },
            loadRequests = { createFriendRequestStatusState(emptySnapshot(), null, false) },
        )

        runtime.loadForRoute(
            request = AppRouteLoadRequest(
                route = AppRoute.Contacts,
                isAuthenticated = true,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = { publishedFriends = it },
                onFriendRequestStatusChanged = {},
            ),
        )

        assertEquals(1, loadFriendsCount)
        assertEquals(loadedFriends, publishedFriends)
    }

    @Test
    fun loadForRoute_contactsWhenSignedOutDoesNotLoadFriends() = runBlocking {
        var loadFriendsCount = 0
        var publishedFriends: List<FriendEntry>? = null
        val runtime = AppRouteLoadRuntime(
            loadFriends = {
                loadFriendsCount += 1
                emptyList()
            },
            loadRequests = { createFriendRequestStatusState(emptySnapshot(), null, false) },
        )

        runtime.loadForRoute(
            request = AppRouteLoadRequest(
                route = AppRoute.Contacts,
                isAuthenticated = false,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = { publishedFriends = it },
                onFriendRequestStatusChanged = {},
            ),
        )

        assertEquals(0, loadFriendsCount)
        assertNull(publishedFriends)
    }

    @Test
    fun loadForRoute_newFriendsWhenAuthenticatedPublishesRequestStatus() = runBlocking {
        val fallbackSnapshot = snapshot("fallback")
        val loadedSnapshot = snapshot("loaded")
        var loadRequestsSnapshot: FriendRequestsSnapshot? = null
        var publishedStatus: FriendRequestStatusState? = null
        val runtime = AppRouteLoadRuntime(
            loadFriends = { emptyList() },
            loadRequests = { snapshot ->
                loadRequestsSnapshot = snapshot
                createFriendRequestStatusState(loadedSnapshot, "loaded", false)
            },
        )

        runtime.loadForRoute(
            request = AppRouteLoadRequest(
                route = AppRoute.NewFriends,
                isAuthenticated = true,
                friendRequestsSnapshot = fallbackSnapshot,
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = {},
                onFriendRequestStatusChanged = { publishedStatus = it },
            ),
        )

        assertEquals(fallbackSnapshot, loadRequestsSnapshot)
        assertEquals(loadedSnapshot, publishedStatus?.snapshot)
        assertEquals("loaded", publishedStatus?.message)
        assertTrue(publishedStatus?.isError == false)
    }

    @Test
    fun loadForRoute_otherAuthenticatedRouteDoesNothing() = runBlocking {
        var loadFriendsCount = 0
        var loadRequestsCount = 0
        var publishedFriends: List<FriendEntry>? = null
        var publishedStatus: FriendRequestStatusState? = null
        val runtime = AppRouteLoadRuntime(
            loadFriends = {
                loadFriendsCount += 1
                emptyList()
            },
            loadRequests = {
                loadRequestsCount += 1
                createFriendRequestStatusState(emptySnapshot(), null, false)
            },
        )

        runtime.loadForRoute(
            request = AppRouteLoadRequest(
                route = AppRoute.Inbox,
                isAuthenticated = true,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = { publishedFriends = it },
                onFriendRequestStatusChanged = { publishedStatus = it },
            ),
        )

        assertEquals(0, loadFriendsCount)
        assertEquals(0, loadRequestsCount)
        assertNull(publishedFriends)
        assertNull(publishedStatus)
    }

    private fun emptySnapshot(): FriendRequestsSnapshot {
        return FriendRequestsSnapshot(incoming = emptyList(), outgoing = emptyList())
    }

    private fun snapshot(requestId: String): FriendRequestsSnapshot {
        return FriendRequestsSnapshot(
            incoming = listOf(
                FriendRequestEntry(
                    requestId = requestId,
                    accountId = requestId,
                    displayName = requestId,
                    direction = "incoming",
                    status = "pending",
                    note = "",
                )
            ),
            outgoing = emptyList(),
        )
    }
}
