package com.kzzz3.argus.lens.feature.inbox

sealed interface InboxAction {
    data class OpenConversation(val conversationId: String) : InboxAction
    data object OpenContacts : InboxAction
    data object OpenScan : InboxAction
    data object SignOutToHud : InboxAction
}
