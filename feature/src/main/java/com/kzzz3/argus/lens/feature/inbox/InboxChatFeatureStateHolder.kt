package com.kzzz3.argus.lens.feature.inbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class InboxChatFeatureState(
    val conversationThreadsState: ConversationThreadsState = ConversationThreadsState(),
    val chatStatusMessage: String? = null,
    val chatStatusError: Boolean = false,
)

class InboxChatFeatureStateHolder(
    initialState: InboxChatFeatureState = InboxChatFeatureState(),
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<InboxChatFeatureState> = mutableState.asStateFlow()

    fun replaceConversationThreadsState(conversationThreadsState: ConversationThreadsState) {
        mutableState.update { state -> state.copy(conversationThreadsState = conversationThreadsState) }
    }

    fun updateChatStatus(message: String?, isError: Boolean) {
        mutableState.update { state ->
            state.copy(
                chatStatusMessage = message,
                chatStatusError = isError,
            )
        }
    }

    fun clearChatStatus() {
        updateChatStatus(message = null, isError = false)
    }
}
