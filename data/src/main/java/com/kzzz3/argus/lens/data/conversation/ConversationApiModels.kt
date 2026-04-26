package com.kzzz3.argus.lens.data.conversation

data class RemoteConversationSummary(
    val id: String,
    val title: String,
    val subtitle: String,
    val preview: String,
    val timestampLabel: String,
    val unreadCount: Int,
    val syncCursor: String,
)

data class RemoteConversationDetail(
    val id: String,
    val title: String,
    val subtitle: String,
    val memberCount: Int,
    val memberDisplayNames: List<String>,
)

data class RemoteConversationMessageAttachment(
    val attachmentId: String,
    val attachmentType: String,
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
)

data class RemoteConversationMessage(
    val id: String,
    val conversationId: String,
    val senderDisplayName: String,
    val body: String,
    val timestampLabel: String,
    val fromCurrentUser: Boolean,
    val deliveryStatus: String,
    val statusUpdatedAt: String,
    val attachment: RemoteConversationMessageAttachment? = null,
)

data class RemoteConversationMessagePage(
    val messages: List<RemoteConversationMessage>,
    val nextSyncCursor: String,
    val recentWindowDays: Int,
    val limit: Int,
)

data class SendRemoteMessageAttachmentRequest(
    val attachmentId: String,
)

data class SendRemoteMessageRequest(
    val clientMessageId: String,
    val body: String? = null,
    val attachment: SendRemoteMessageAttachmentRequest? = null,
)

data class MessageReceiptRequest(
    val receiptType: String,
)

data class ConversationApiErrorResponse(
    val code: String,
    val message: String,
)
