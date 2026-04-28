package com.kzzz3.argus.lens.feature.contacts

data class ContactsActionRouteRequest(
    val currentState: ContactsState,
)

data class ContactsActionRouteCallbacks(
    val onContactsStateChanged: (ContactsState) -> Unit,
)

class ContactsActionHandler(
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
