package com.kzzz3.argus.lens.feature.contacts

sealed interface ContactsEffect {
    data class AddFriend(val friendAccountId: String) : ContactsEffect
    data object OpenNewFriends : ContactsEffect
    data object NavigateBackToInbox : ContactsEffect
    data class OpenConversation(val conversationId: String) : ContactsEffect
}
