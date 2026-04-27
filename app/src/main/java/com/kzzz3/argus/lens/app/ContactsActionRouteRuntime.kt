package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsReducerResult
import com.kzzz3.argus.lens.feature.contacts.ContactsState

internal data class ContactsActionRouteRequest(
    val currentState: ContactsState,
)

internal data class ContactsActionRouteCallbacks(
    val onContactsStateChanged: (ContactsState) -> Unit,
)

internal class ContactsActionRouteRuntime(
    private val reduceAction: (ContactsState, ContactsAction) -> ContactsReducerResult,
    private val handleEffect: (ContactsEffect?) -> Unit,
) {
    fun handleAction(
        action: ContactsAction,
        request: ContactsActionRouteRequest,
        callbacks: ContactsActionRouteCallbacks,
    ) {
        val result = reduceAction(request.currentState, action)
        callbacks.onContactsStateChanged(result.state)
        handleEffect(result.effect)
    }
}
