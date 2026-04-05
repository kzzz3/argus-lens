package com.kzzz3.argus.lens.feature.inbox

sealed interface InboxAction {
    data class OpenConversation(val conversationId: String) : InboxAction
    data object SignOutToHud : InboxAction
}
