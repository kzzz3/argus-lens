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

        ContactsAction.OpenNewFriends -> ContactsReducerResult(
            state = currentState,
            effect = ContactsEffect.OpenNewFriends,
        )

        is ContactsAction.UpdateDraftFriendAccountId -> ContactsReducerResult(
            state = currentState.copy(draftFriendAccountId = action.value),
            effect = null,
        )

        ContactsAction.SubmitAddFriend -> {
            val trimmedFriendAccountId = currentState.draftFriendAccountId.trim()
            if (trimmedFriendAccountId.isEmpty()) {
                ContactsReducerResult(
                    state = currentState,
                    effect = null,
                )
            } else {
                ContactsReducerResult(
                    state = currentState.copy(draftFriendAccountId = ""),
                    effect = ContactsEffect.AddFriend(trimmedFriendAccountId),
                )
            }
        }
    }
}
