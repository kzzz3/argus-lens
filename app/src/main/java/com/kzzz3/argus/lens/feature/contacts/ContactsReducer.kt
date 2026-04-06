package com.kzzz3.argus.lens.feature.contacts

data class ContactsReducerResult(
    val state: ContactsState,
    val effect: ContactsEffect?,
)

fun reduceContactsState(
    currentState: ContactsState,
    action: ContactsAction,
): ContactsReducerResult {
    return when (action) {
        is ContactsAction.OpenConversation -> ContactsReducerResult(
            state = currentState,
            effect = ContactsEffect.OpenConversation(action.conversationId),
        )

        ContactsAction.NavigateBackToInbox -> ContactsReducerResult(
            state = currentState,
            effect = ContactsEffect.NavigateBackToInbox,
        )

        is ContactsAction.UpdateDraftConversationName -> ContactsReducerResult(
            state = currentState.copy(draftConversationName = action.value),
            effect = null,
        )

        ContactsAction.CreateConversation -> {
            val trimmedName = currentState.draftConversationName.trim()
            if (trimmedName.isEmpty()) {
                ContactsReducerResult(
                    state = currentState,
                    effect = null,
                )
            } else {
                ContactsReducerResult(
                    state = currentState.copy(draftConversationName = ""),
                    effect = ContactsEffect.CreateConversation(trimmedName),
                )
            }
        }
    }
}
