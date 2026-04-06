package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

data class InboxConversationThread(
    val id: String,
    val title: String,
    val subtitle: String,
    val unreadCount: Int,
    val messages: List<ChatMessageItem>,
    val draftMessage: String = "",
    val draftAttachments: List<ChatDraftAttachment> = emptyList(),
    val isVoiceRecording: Boolean = false,
) {
    companion object {
        val Saver: Saver<InboxConversationThread, Any> = listSaver(
            save = { thread ->
                listOf(
                    thread.id,
                    thread.title,
                    thread.subtitle,
                    thread.unreadCount,
                    thread.messages.map(::saveChatMessageItem),
                    thread.draftMessage,
                    thread.draftAttachments.map(::saveChatDraftAttachment),
                    thread.isVoiceRecording,
                )
            },
            restore = { values ->
                if (values.size != 8) {
                    null
                } else {
                    InboxConversationThread(
                        id = values[0] as String,
                        title = values[1] as String,
                        subtitle = values[2] as String,
                        unreadCount = values[3] as Int,
                        messages = restoreChatMessageList(values[4]),
                        draftMessage = values[5] as String,
                        draftAttachments = restoreChatDraftAttachmentList(values[6]),
                        isVoiceRecording = values[7] as Boolean,
                    )
                }
            }
        )
    }
}

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

data class ChatMessageItem(
    val id: String,
    val senderDisplayName: String,
    val body: String,
    val timestampLabel: String,
    val isFromCurrentUser: Boolean,
    val deliveryStatus: ChatMessageDeliveryStatus = ChatMessageDeliveryStatus.Sent,
) {
    companion object {
        val Saver: Saver<ChatMessageItem, Any> = listSaver(
            save = { message -> saveChatMessageItem(message) },
            restore = { values -> restoreChatMessageItem(values) }
        )
    }
}

enum class ChatMessageDeliveryStatus {
    Sending,
    Sent,
    Failed,
}

enum class ChatDraftAttachmentKind {
    Image,
    Video,
    Voice,
}

data class ChatDraftAttachment(
    val id: String,
    val kind: ChatDraftAttachmentKind,
    val title: String,
    val summary: String,
) {
    companion object {
        val Saver: Saver<ChatDraftAttachment, Any> = listSaver(
            save = { attachment -> saveChatDraftAttachment(attachment) },
            restore = { values -> restoreChatDraftAttachment(values) }
        )
    }
}

fun inboxConversationThreadListSaver(): Saver<List<InboxConversationThread>, Any> = listSaver(
    save = { threads -> threads.map { saveInboxConversationThread(it) } },
    restore = { values -> values.mapNotNull { restoreInboxConversationThread(it) } }
)

private fun saveInboxConversationThread(
    thread: InboxConversationThread,
): List<Any> {
    return listOf(
        thread.id,
        thread.title,
        thread.subtitle,
        thread.unreadCount,
        thread.messages.map(::saveChatMessageItem),
        thread.draftMessage,
        thread.draftAttachments.map(::saveChatDraftAttachment),
        thread.isVoiceRecording,
    )
}

private fun restoreInboxConversationThread(
    value: Any?,
): InboxConversationThread? {
    val values = value as? List<*> ?: return null
    if (values.size != 8) return null

    return InboxConversationThread(
        id = values[0] as String,
        title = values[1] as String,
        subtitle = values[2] as String,
        unreadCount = values[3] as Int,
        messages = restoreChatMessageList(values[4]),
        draftMessage = values[5] as String,
        draftAttachments = restoreChatDraftAttachmentList(values[6]),
        isVoiceRecording = values[7] as Boolean,
    )
}

private fun saveChatMessageItem(
    message: ChatMessageItem,
): List<Any> {
    return listOf(
        message.id,
        message.senderDisplayName,
        message.body,
        message.timestampLabel,
        message.isFromCurrentUser,
        message.deliveryStatus.name,
    )
}

private fun restoreChatMessageItem(
    values: List<Any?>,
): ChatMessageItem? {
    if (values.size != 6) return null

    return ChatMessageItem(
        id = values[0] as String,
        senderDisplayName = values[1] as String,
        body = values[2] as String,
        timestampLabel = values[3] as String,
        isFromCurrentUser = values[4] as Boolean,
        deliveryStatus = ChatMessageDeliveryStatus.valueOf(values[5] as String),
    )
}

private fun restoreChatMessageList(
    value: Any?,
): List<ChatMessageItem> {
    val values = value as? List<*> ?: return emptyList()
    return values.mapNotNull { entry ->
        restoreChatMessageItem(entry as? List<Any?> ?: return@mapNotNull null)
    }
}

private fun saveChatDraftAttachment(
    attachment: ChatDraftAttachment,
): List<Any> {
    return listOf(
        attachment.id,
        attachment.kind.name,
        attachment.title,
        attachment.summary,
    )
}

private fun restoreChatDraftAttachment(
    values: List<Any?>,
): ChatDraftAttachment? {
    if (values.size != 4) return null

    return ChatDraftAttachment(
        id = values[0] as String,
        kind = ChatDraftAttachmentKind.valueOf(values[1] as String),
        title = values[2] as String,
        summary = values[3] as String,
    )
}

private fun restoreChatDraftAttachmentList(
    value: Any?,
): List<ChatDraftAttachment> {
    val values = value as? List<*> ?: return emptyList()
    return values.mapNotNull { entry ->
        restoreChatDraftAttachment(entry as? List<Any?> ?: return@mapNotNull null)
    }
}
