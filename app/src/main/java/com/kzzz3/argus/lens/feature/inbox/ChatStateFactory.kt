package com.kzzz3.argus.lens.feature.inbox

fun createChatUiState(
    state: ChatState,
): ChatUiState {
    return ChatUiState(
        conversationTitle = state.conversationTitle,
        conversationSubtitle = state.conversationSubtitle,
        memberSummary = state.memberSummary,
        messages = state.messages,
        draftMessage = state.draftMessage,
        draftAttachments = state.draftAttachments,
        composerTitle = "Stage-1 media composer",
        composerHint = if (state.isVoiceRecording) {
            "Recording local voice note ${formatVoiceRecordingLabel(state.voiceRecordingSeconds)}. Tap voice again to finish or cancel it below."
        } else {
            "Build a local draft with text, image, video, or voice before wiring the real media stack."
        },
        imageActionLabel = "Add image",
        videoActionLabel = "Add video",
        voiceActionLabel = if (state.isVoiceRecording) "Finish voice" else "Start voice",
        voiceRecordingLabel = if (state.isVoiceRecording) {
            "Recording ${formatVoiceRecordingLabel(state.voiceRecordingSeconds)}"
        } else {
            "Ready for local voice note"
        },
        cancelVoiceActionLabel = "Cancel voice",
        isCancelVoiceVisible = state.isVoiceRecording,
        audioCallActionLabel = "Audio call",
        videoCallActionLabel = "Video call",
        isSendEnabled = state.draftMessage.trim().isNotEmpty() || state.draftAttachments.isNotEmpty(),
        sendActionLabel = "Send draft",
        backActionLabel = "Back to inbox",
        emptyStateLabel = "No messages yet. Send the first local message in this stage-1 shell.",
    )
}

private fun formatVoiceRecordingLabel(
    seconds: Int,
): String {
    val minutes = seconds / 60
    val remainSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainSeconds)
}
