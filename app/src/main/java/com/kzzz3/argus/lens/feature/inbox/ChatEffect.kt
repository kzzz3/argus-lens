package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatEffect {
    data object NavigateBackToInbox : ChatEffect
}
