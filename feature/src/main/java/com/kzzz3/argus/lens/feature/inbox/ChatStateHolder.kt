package com.kzzz3.argus.lens.feature.inbox

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatStateHolderState(
    val chatState: ChatState? = null,
    val uiState: ChatUiState? = null,
)

class ChatStateHolder(
    initialState: ChatStateHolderState = ChatStateHolderState(),
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<ChatStateHolderState> = mutableState.asStateFlow()

    fun replaceInputs(
        currentUserDisplayName: String,
        threadsState: ConversationThreadsState,
        activeChatConversationId: String,
        statusMessage: String?,
        isStatusError: Boolean,
    ) {
        val activeChatConversation = threadsState.threads.firstOrNull { conversation ->
            conversation.id == activeChatConversationId
        }
        val chatState = activeChatConversation?.let { conversation ->
            ChatState(
                conversationId = conversation.id,
                conversationTitle = conversation.title,
                conversationSubtitle = conversation.subtitle,
                currentUserDisplayName = currentUserDisplayName,
                messages = conversation.messages,
                draftMessage = conversation.draftMessage,
                draftAttachments = conversation.draftAttachments,
                isVoiceRecording = conversation.isVoiceRecording,
                voiceRecordingSeconds = conversation.voiceRecordingSeconds,
            )
        }

        mutableState.value = ChatStateHolderState(
            chatState = chatState,
            uiState = chatState?.let { state ->
                createChatUiState(
                    state = state,
                    statusMessage = statusMessage,
                    isStatusError = isStatusError,
                )
            },
        )
    }
}
