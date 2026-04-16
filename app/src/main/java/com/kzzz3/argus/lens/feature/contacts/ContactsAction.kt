package com.kzzz3.argus.lens.feature.contacts

sealed interface ContactsAction {
    data class UpdateDraftFriendAccountId(val value: String) : ContactsAction
    data object SubmitAddFriend : ContactsAction
    data object OpenNewFriends : ContactsAction
    data class OpenConversation(val conversationId: String) : ContactsAction
    data object NavigateBackToInbox : ContactsAction
}
