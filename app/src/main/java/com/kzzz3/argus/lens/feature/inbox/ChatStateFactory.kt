package com.kzzz3.argus.lens.feature.inbox

fun createChatUiState(
    state: ChatState,
): ChatUiState {
    return ChatUiState(
        conversationTitle = state.conversationTitle,
        conversationSubtitle = state.conversationSubtitle,
        messages = state.messages,
        draftMessage = state.draftMessage,
        draftAttachments = state.draftAttachments,
        composerTitle = "Stage-1 media composer",
        composerHint = if (state.isVoiceRecording) {
            "Voice recording is armed. Tap voice again to turn it into a local draft clip."
        } else {
            "Build a local draft with text, image, video, or voice before wiring the real media stack."
        },
        imageActionLabel = "Add image",
        videoActionLabel = "Add video",
        voiceActionLabel = if (state.isVoiceRecording) "Finish voice" else "Voice draft",
        isSendEnabled = state.draftMessage.trim().isNotEmpty() || state.draftAttachments.isNotEmpty(),
        sendActionLabel = "Send draft",
        backActionLabel = "Back to inbox",
        emptyStateLabel = "No messages yet. Send the first local message in this stage-1 shell.",
    )
}
