package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadCallbacks
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadHandler
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadRequest
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadTarget
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.createFriendRequestStatusState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactsRouteLoadHandlerTest {
    @Test
    fun loadForRoute_contactsWhenAuthenticatedPublishesLoadedFriends() = runBlocking {
        val loadedFriends = listOf(FriendEntry("alice", "Alice", "direct"))
        var loadFriendsCount = 0
        var publishedFriends: List<FriendEntry>? = null
        val handler = ContactsRouteLoadHandler(
            loadFriends = {
                loadFriendsCount += 1
                loadedFriends
            },
            loadRequests = { createFriendRequestStatusState(emptySnapshot(), null, false) },
        )

        handler.loadForTarget(
            request = ContactsRouteLoadRequest(
                target = ContactsRouteLoadTarget.Contacts,
                isAuthenticated = true,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = ContactsRouteLoadCallbacks(
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
        val handler = ContactsRouteLoadHandler(
            loadFriends = {
                loadFriendsCount += 1
                emptyList()
            },
            loadRequests = { createFriendRequestStatusState(emptySnapshot(), null, false) },
        )

        handler.loadForTarget(
            request = ContactsRouteLoadRequest(
                target = ContactsRouteLoadTarget.Contacts,
                isAuthenticated = false,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = ContactsRouteLoadCallbacks(
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
        val handler = ContactsRouteLoadHandler(
            loadFriends = { emptyList() },
            loadRequests = { snapshot ->
                loadRequestsSnapshot = snapshot
                createFriendRequestStatusState(loadedSnapshot, "loaded", false)
            },
        )

        handler.loadForTarget(
            request = ContactsRouteLoadRequest(
                target = ContactsRouteLoadTarget.NewFriends,
                isAuthenticated = true,
                friendRequestsSnapshot = fallbackSnapshot,
            ),
            callbacks = ContactsRouteLoadCallbacks(
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
        val handler = ContactsRouteLoadHandler(
            loadFriends = {
                loadFriendsCount += 1
                emptyList()
            },
            loadRequests = {
                loadRequestsCount += 1
                createFriendRequestStatusState(emptySnapshot(), null, false)
            },
        )

        handler.loadForTarget(
            request = ContactsRouteLoadRequest(
                target = ContactsRouteLoadTarget.None,
                isAuthenticated = true,
                friendRequestsSnapshot = emptySnapshot(),
            ),
            callbacks = ContactsRouteLoadCallbacks(
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
