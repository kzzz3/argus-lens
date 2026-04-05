package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatAction {
    data object NavigateBackToInbox : ChatAction
}
