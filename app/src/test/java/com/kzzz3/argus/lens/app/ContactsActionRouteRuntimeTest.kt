package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsReducerResult
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsActionRouteRuntimeTest {
    @Test
    fun handleAction_publishesReducedStateBeforeHandlingEffect() {
        val currentState = ContactsState(draftFriendAccountId = "old")
        val reducedState = ContactsState(draftFriendAccountId = "new")
        val effect = ContactsEffect.OpenNewFriends
        val events = mutableListOf<String>()
        var reducerInputState: ContactsState? = null
        var reducerInputAction: ContactsAction? = null
        var effectInput: ContactsEffect? = null
        val runtime = ContactsActionRouteRuntime(
            reduceAction = { state, action ->
                reducerInputState = state
                reducerInputAction = action
                ContactsReducerResult(reducedState, effect)
            },
            handleEffect = { resolvedEffect ->
                events += "effect"
                effectInput = resolvedEffect
            },
        )

        runtime.handleAction(
            action = ContactsAction.OpenNewFriends,
            request = ContactsActionRouteRequest(currentState = currentState),
            callbacks = ContactsActionRouteCallbacks(
                onContactsStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
            ),
        )

        assertEquals(currentState, reducerInputState)
        assertEquals(ContactsAction.OpenNewFriends, reducerInputAction)
        assertEquals(effect, effectInput)
        assertEquals(listOf("state", "effect"), events)
    }

    @Test
    fun handleAction_passesNullEffectAfterPublishingState() {
        val currentState = ContactsState(draftFriendAccountId = "old")
        val reducedState = ContactsState(draftFriendAccountId = "new")
        val events = mutableListOf<String>()
        var effectHandled = true
        val runtime = ContactsActionRouteRuntime(
            reduceAction = { _, _ -> ContactsReducerResult(reducedState, null) },
            handleEffect = { effect ->
                events += "effect"
                effectHandled = effect != null
            },
        )

        runtime.handleAction(
            action = ContactsAction.UpdateDraftFriendAccountId("new"),
            request = ContactsActionRouteRequest(currentState = currentState),
            callbacks = ContactsActionRouteCallbacks(
                onContactsStateChanged = {
                    events += "state"
                    assertEquals(reducedState, it)
                },
            ),
        )

        assertEquals(false, effectHandled)
        assertEquals(listOf("state", "effect"), events)
    }
}
