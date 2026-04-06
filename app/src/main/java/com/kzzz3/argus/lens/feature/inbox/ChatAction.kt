package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatAction {
    data class UpdateDraftMessage(val value: String) : ChatAction
    data object SendMessage : ChatAction
    data object NavigateBackToInbox : ChatAction
}
