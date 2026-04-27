package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InboxState(
    val uiState: InboxUiState = createInboxUiState(
        sessionState = AppSessionState(),
        threads = emptyList(),
        realtimeStatusLabel = "disabled",
        shellStatusLabel = "Signed out",
    ),
)

data class InboxStateHolderCallbacks(
    val onOpenConversation: (String) -> Unit,
    val onOpenContacts: () -> Unit,
    val onOpenWallet: () -> Unit,
    val onSignOutToHud: () -> Unit,
)

class InboxStateHolder(
    initialState: InboxState = InboxState(),
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<InboxState> = mutableState.asStateFlow()

    fun replaceInputs(
        sessionState: AppSessionState,
        threadsState: ConversationThreadsState,
        realtimeStatusLabel: String,
        shellStatusLabel: String,
    ) {
        mutableState.value = InboxState(
            uiState = createInboxUiState(
                sessionState = sessionState,
                threads = threadsState.threads,
                realtimeStatusLabel = realtimeStatusLabel,
                shellStatusLabel = shellStatusLabel,
            ),
        )
    }

    fun handleAction(
        action: InboxAction,
        callbacks: InboxStateHolderCallbacks,
    ) {
        when (action) {
            is InboxAction.OpenConversation -> callbacks.onOpenConversation(action.conversationId)
            InboxAction.OpenContacts -> callbacks.onOpenContacts()
            InboxAction.OpenWallet -> callbacks.onOpenWallet()
            InboxAction.SignOutToHud -> callbacks.onSignOutToHud()
        }
    }
}
