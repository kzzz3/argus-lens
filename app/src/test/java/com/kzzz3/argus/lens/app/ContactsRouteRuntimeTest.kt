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
import com.kzzz3.argus.lens.feature.contacts.createFriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsRouteRuntimeTest {
    @Test
    fun handleContactsEffect_navigateBackRoutesToInbox() {
        val runtime = createRuntime()
        var routedTo: AppRoute? = null

        runtime.handleContactsEffect(
            effect = ContactsEffect.NavigateBackToInbox,
            request = contactsRequest(),
            callbacks = callbacks(onRouteChanged = { routedTo = it }),
        )

        assertEquals(AppRoute.Inbox, routedTo)
    }

    @Test
    fun handleContactsEffect_openNewFriendsRoutesToNewFriends() {
        val runtime = createRuntime()
        var routedTo: AppRoute? = null

        runtime.handleContactsEffect(
            effect = ContactsEffect.OpenNewFriends,
            request = contactsRequest(),
            callbacks = callbacks(onRouteChanged = { routedTo = it }),
        )

        assertEquals(AppRoute.NewFriends, routedTo)
    }

    @Test
    fun handleContactsEffect_openConversationPublishesThreadsAndOpenedId() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val conversations = ConversationThreadsState()
        val runtime = createRuntime(
            scope = scope,
            openConversation = { _, conversationId ->
                ContactsOpenConversationResult(
                    conversationThreadsState = conversations,
                    conversationId = "resolved-$conversationId",
                )
            },
        )
        var appliedConversations: ConversationThreadsState? = null
        var openedConversationId: String? = null

        runtime.handleContactsEffect(
            effect = ContactsEffect.OpenConversation("alice"),
            request = contactsRequest(),
            callbacks = callbacks(
                onConversationThreadsChanged = { appliedConversations = it },
                onConversationOpened = { openedConversationId = it },
            ),
        )

        assertEquals(conversations, appliedConversations)
        assertEquals("resolved-alice", openedConversationId)
        scope.cancel()
    }

    @Test
    fun handleContactsEffect_addFriendUsesProvidedStateAndPublishesSnapshot() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        var requestInput: ContactsState? = null
        val refreshedSnapshot = FriendRequestsSnapshot(emptyList(), emptyList())
        val runtime = createRuntime(
            scope = scope,
            addFriend = { state, friendAccountId ->
                requestInput = state
                AddFriendResult(
                    contactsState = state.copy(statusMessage = "sent to $friendAccountId"),
                    friendRequestsSnapshot = refreshedSnapshot,
                )
            },
        )
        var contactsState = ContactsState(statusMessage = "old")
        var snapshot: FriendRequestsSnapshot? = null

        runtime.handleContactsEffect(
            effect = ContactsEffect.AddFriend("alice"),
            request = contactsRequest(contactsState = contactsState.copy(statusMessage = "sending")),
            callbacks = callbacks(
                onContactsStateChanged = { contactsState = it },
                onFriendRequestsSnapshotChanged = { snapshot = it },
            ),
        )

        assertEquals("sending", requestInput?.statusMessage)
        assertEquals("sent to alice", contactsState.statusMessage)
        assertEquals(refreshedSnapshot, snapshot)
        scope.cancel()
    }

    @Test
    fun handleNewFriendsAction_acceptPublishesStatusFriendsAndConversations() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val friends = listOf(FriendEntry("alice", "Alice", ""))
        val conversations = ConversationThreadsState()
        val status = FriendRequestStatusState(
            snapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
            status = createFriendRequestStatusState(
                snapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
                message = "Accepted",
                isError = false,
            ).status,
        )
        val runtime = createRuntime(
            scope = scope,
            accept = { requestId, _, _, _ ->
                assertEquals("request-1", requestId)
                NewFriendsActionResult(
                    status = status,
                    friends = friends,
                    conversationThreadsState = conversations,
                )
            },
        )
        var appliedStatus: FriendRequestStatusState? = null
        var appliedFriends: List<FriendEntry>? = null
        var appliedConversations: ConversationThreadsState? = null

        runtime.handleNewFriendsAction(
            action = NewFriendsAction.Accept("request-1"),
            request = newFriendsRequest(),
            callbacks = callbacks(
                onFriendRequestStatusChanged = { appliedStatus = it },
                onFriendsChanged = { appliedFriends = it },
                onConversationThreadsChanged = { appliedConversations = it },
            ),
        )

        assertEquals(status, appliedStatus)
        assertEquals(friends, appliedFriends)
        assertEquals(conversations, appliedConversations)
        scope.cancel()
    }

    @Test
    fun handleNewFriendsAction_navigateBackRoutesToContacts() {
        val runtime = createRuntime()
        var routedTo: AppRoute? = null

        runtime.handleNewFriendsAction(
            action = NewFriendsAction.NavigateBack,
            request = newFriendsRequest(),
            callbacks = callbacks(onRouteChanged = { routedTo = it }),
        )

        assertEquals(AppRoute.Contacts, routedTo)
    }

    @Test
    fun handleNewFriendsAction_rejectPublishesStatusOnly() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val status = createFriendRequestStatusState(
            snapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
            message = "Rejected",
            isError = false,
        )
        val runtime = createRuntime(
            scope = scope,
            reject = { requestId, _ ->
                assertEquals("request-2", requestId)
                NewFriendsActionResult(status = status)
            },
        )
        var appliedStatus: FriendRequestStatusState? = null
        var appliedFriends: List<FriendEntry>? = null

        runtime.handleNewFriendsAction(
            action = NewFriendsAction.Reject("request-2"),
            request = newFriendsRequest(),
            callbacks = callbacks(
                onFriendRequestStatusChanged = { appliedStatus = it },
                onFriendsChanged = { appliedFriends = it },
            ),
        )

        assertEquals(status, appliedStatus)
        assertEquals(null, appliedFriends)
        scope.cancel()
    }

    @Test
    fun handleNewFriendsAction_ignorePublishesStatusOnly() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val status = createFriendRequestStatusState(
            snapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
            message = "Ignored",
            isError = false,
        )
        val runtime = createRuntime(
            scope = scope,
            ignore = { requestId, _ ->
                assertEquals("request-3", requestId)
                NewFriendsActionResult(status = status)
            },
        )
        var appliedStatus: FriendRequestStatusState? = null

        runtime.handleNewFriendsAction(
            action = NewFriendsAction.Ignore("request-3"),
            request = newFriendsRequest(),
            callbacks = callbacks(onFriendRequestStatusChanged = { appliedStatus = it }),
        )

        assertEquals(status, appliedStatus)
        scope.cancel()
    }

    private fun createRuntime(
        scope: CoroutineScope = CoroutineScope(Dispatchers.Unconfined),
        openConversation: suspend (ContactsRouteRequest, String) -> ContactsOpenConversationResult = { request, conversationId ->
            ContactsOpenConversationResult(request.conversationThreadsState, conversationId)
        },
        addFriend: suspend (ContactsState, String) -> AddFriendResult = { state, _ -> AddFriendResult(state, null) },
        accept: suspend (String, AppSessionState, FriendRequestsSnapshot, ConversationThreadsState) -> NewFriendsActionResult = { _, _, snapshot, _ ->
            NewFriendsActionResult(FriendRequestStatusState(snapshot, null))
        },
        reject: suspend (String, FriendRequestsSnapshot) -> NewFriendsActionResult = { _, snapshot ->
            NewFriendsActionResult(FriendRequestStatusState(snapshot, null))
        },
        ignore: suspend (String, FriendRequestsSnapshot) -> NewFriendsActionResult = { _, snapshot ->
            NewFriendsActionResult(FriendRequestStatusState(snapshot, null))
        },
    ): ContactsRouteRuntime {
        return ContactsRouteRuntime(
            scope = scope,
            openConversation = openConversation,
            addFriend = addFriend,
            acceptFriendRequest = accept,
            rejectFriendRequest = reject,
            ignoreFriendRequest = ignore,
        )
    }

    private fun contactsRequest(
        contactsState: ContactsState = ContactsState(),
    ): ContactsRouteRequest {
        return ContactsRouteRequest(
            session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
            contactsState = contactsState,
            friends = emptyList(),
            conversationThreadsState = ConversationThreadsState(),
            friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
        )
    }

    private fun newFriendsRequest(): ContactsRouteRequest = contactsRequest()

    private fun callbacks(
        onRouteChanged: (AppRoute) -> Unit = {},
        onContactsStateChanged: (ContactsState) -> Unit = {},
        onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit = {},
        onConversationThreadsChanged: (ConversationThreadsState) -> Unit = {},
        onConversationOpened: (String) -> Unit = {},
        onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit = {},
        onFriendsChanged: (List<FriendEntry>) -> Unit = {},
    ): ContactsRouteCallbacks {
        return ContactsRouteCallbacks(
            onRouteChanged = onRouteChanged,
            onContactsStateChanged = onContactsStateChanged,
            onFriendRequestsSnapshotChanged = onFriendRequestsSnapshotChanged,
            onConversationThreadsChanged = onConversationThreadsChanged,
            onConversationOpened = onConversationOpened,
            onFriendRequestStatusChanged = onFriendRequestStatusChanged,
            onFriendsChanged = onFriendsChanged,
        )
    }
}
