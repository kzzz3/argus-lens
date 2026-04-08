package com.kzzz3.argus.lens.feature.inbox

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class InboxConversationThread(
    val id: String,
    val title: String,
    val subtitle: String,
    val unreadCount: Int,
    val messages: List<ChatMessageItem>,
    val draftMessage: String = "",
    val draftAttachments: List<ChatDraftAttachment> = emptyList(),
    val isVoiceRecording: Boolean = false,
    val voiceRecordingSeconds: Int = 0,
) : Parcelable

data class InboxConversationItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
    val latestMessageStatusLabel: String?,
    val latestMessageStatusColorToken: InboxStatusColorToken,
)

enum class InboxStatusColorToken {
    Neutral,
    Success,
    Warning,
}

@Parcelize
data class ChatMessageItem(
    val id: String,
    val senderDisplayName: String,
    val body: String,
    val timestampLabel: String,
    val isFromCurrentUser: Boolean,
    val deliveryStatus: ChatMessageDeliveryStatus = ChatMessageDeliveryStatus.Sent,
) : Parcelable

enum class ChatMessageDeliveryStatus {
    Sending,
    Sent,
    Delivered,
    Failed,
    Recalled,
}

enum class ChatDraftAttachmentKind {
    Image,
    Video,
    Voice,
}

@Parcelize
data class ChatDraftAttachment(
    val id: String,
    val kind: ChatDraftAttachmentKind,
    val title: String,
    val summary: String,
) : Parcelable

@Parcelize
data class ConversationThreadsState(
    val threads: List<InboxConversationThread> = emptyList(),
) : Parcelable
