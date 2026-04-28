package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.AddFriendResult
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureCallbacks
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureController
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureSnapshot
import com.kzzz3.argus.lens.feature.contacts.ContactsOpenConversationResult
import com.kzzz3.argus.lens.feature.contacts.ContactsReducerResult
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteHandler
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsActionResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsFeatureControllerTest {
    @Test
    fun handleAction_publishesReducedStateBeforeHandlingEffect() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val currentState = ContactsState(draftFriendAccountId = "old")
        val reducedState = ContactsState(draftFriendAccountId = "new")
        val events = mutableListOf<String>()
        var routedTo: AppRoute? = null
        val controller = ContactsFeatureController(
            routeHandler = createRouteHandler(scope),
            reduceAction = { _, _ -> ContactsReducerResult(reducedState, com.kzzz3.argus.lens.feature.contacts.ContactsEffect.OpenNewFriends) },
        )

        controller.handleContactsAction(
            action = ContactsAction.OpenNewFriends,
            snapshot = snapshot(currentState),
            callbacks = callbacks(
                onContactsStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
            ),
        )

        assertEquals(AppRoute.NewFriends, routedTo)
        assertEquals(listOf("state", "route"), events)
        scope.cancel()
    }

    @Test
    fun handleAction_publishesStateWithoutRoutingWhenReducerHasNoEffect() {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val currentState = ContactsState(draftFriendAccountId = "old")
        val reducedState = ContactsState(draftFriendAccountId = "new")
        val events = mutableListOf<String>()
        val controller = ContactsFeatureController(
            routeHandler = createRouteHandler(scope),
            reduceAction = { _, _ -> ContactsReducerResult(reducedState, null) },
        )

        controller.handleContactsAction(
            action = ContactsAction.UpdateDraftFriendAccountId("new"),
            snapshot = snapshot(currentState),
            callbacks = callbacks(onContactsStateChanged = {
                events += "state"
                assertEquals(reducedState, it)
            }),
        )

        assertEquals(listOf("state"), events)
        scope.cancel()
    }

    private fun createRouteHandler(scope: CoroutineScope): ContactsRouteHandler {
        return ContactsRouteHandler(
            scope = scope,
            openConversation = { request, conversationId ->
                ContactsOpenConversationResult(request.conversationThreadsState, conversationId)
            },
            addFriend = { state, _ -> AddFriendResult(state, null) },
            acceptFriendRequest = { _, _, snapshot, _ -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
            rejectFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
            ignoreFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
        )
    }

    private fun snapshot(contactsState: ContactsState): ContactsFeatureSnapshot {
        return ContactsFeatureSnapshot(
            session = AppSessionState(isAuthenticated = true, accountId = "tester", displayName = "Tester"),
            contactsState = contactsState,
            friends = emptyList(),
            conversationThreadsState = ConversationThreadsState(),
            friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
        )
    }

    private fun callbacks(
        onContactsStateChanged: (ContactsState) -> Unit = {},
        onRouteChanged: (AppRoute) -> Unit = {},
    ): ContactsFeatureCallbacks {
        return ContactsFeatureCallbacks(
            onOpenNewFriends = { onRouteChanged(AppRoute.NewFriends) },
            onNavigateBackToInbox = { onRouteChanged(AppRoute.Inbox) },
            onNavigateBackToContacts = { onRouteChanged(AppRoute.Contacts) },
            onContactsStateChanged = onContactsStateChanged,
            onFriendRequestsSnapshotChanged = {},
            onConversationThreadsChanged = {},
            onConversationOpened = {},
            onFriendRequestStatusChanged = {},
            onFriendsChanged = {},
        )
    }
}
