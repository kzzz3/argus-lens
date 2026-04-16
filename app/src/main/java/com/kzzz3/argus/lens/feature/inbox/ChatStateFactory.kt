package com.kzzz3.argus.lens.feature.inbox

fun createChatUiState(
    state: ChatState,
    statusMessage: String? = null,
    isStatusError: Boolean = false,
): ChatUiState {
    return ChatUiState(
        conversationTitle = state.conversationTitle,
        conversationSubtitle = state.conversationSubtitle,
        statusMessage = statusMessage,
        isStatusError = isStatusError,
        messages = state.messages,
        draftMessage = state.draftMessage,
        draftAttachments = state.draftAttachments,
        composerTitle = "Stage-1 media composer",
        composerHint = "Build a local draft with text or file attachments (image, video) before wiring the real media stack.",
        imageActionLabel = "Add image",
        videoActionLabel = "Add video",
        voiceActionLabel = "",
        voiceRecordingLabel = "",
        cancelVoiceActionLabel = "",
        isCancelVoiceVisible = false,
        audioCallActionLabel = "Audio call",
        videoCallActionLabel = "Video call",
        isSendEnabled = state.draftMessage.trim().isNotEmpty() || state.draftAttachments.isNotEmpty(),
        sendActionLabel = "Send draft",
        backActionLabel = "Back to inbox",
        emptyStateLabel = "No messages yet. Send the first local message in this stage-1 shell.",
    )
}

