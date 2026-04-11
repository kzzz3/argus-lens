package com.kzzz3.argus.lens.feature.inbox

sealed interface ChatAction {
    data class UpdateDraftMessage(val value: String) : ChatAction
    data class UpdateDraftMemberAccountId(val value: String) : ChatAction
    data object SubmitAddMember : ChatAction
    data object StartAudioCall : ChatAction
    data object StartVideoCall : ChatAction
    data object AddImageAttachment : ChatAction
    data object AddVideoAttachment : ChatAction
    data object ToggleVoiceDraft : ChatAction
    data object TickVoiceRecording : ChatAction
    data object CancelVoiceRecording : ChatAction
    data class RemoveDraftAttachment(val attachmentId: String) : ChatAction
    data class RetryFailedMessage(val messageId: String) : ChatAction
    data class RecallMessage(val messageId: String) : ChatAction
    data object SendMessage : ChatAction
    data object NavigateBackToInbox : ChatAction
}
