package com.kzzz3.argus.lens.feature.inbox

data class ChatState(
    val conversationId: String,
    val conversationTitle: String,
    val conversationSubtitle: String,
    val memberSummary: String = "",
    val currentUserDisplayName: String,
    val messages: List<ChatMessageItem>,
    val draftMessage: String = "",
    val draftAttachments: List<ChatDraftAttachment> = emptyList(),
    val isVoiceRecording: Boolean = false,
    val voiceRecordingSeconds: Int = 0,
)
