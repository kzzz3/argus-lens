package com.kzzz3.argus.lens.data.conversation

import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import kotlin.math.max

internal fun mergeRemoteConversationSummaries(
    localState: ConversationThreadsState,
    remoteSummaries: List<RemoteConversationSummary>,
): ConversationThreadsState {
    val localById = localState.threads.associateBy { it.id }
    val mergedThreads = remoteSummaries.map { summary ->
        val existingThread = localById[summary.id]
        InboxConversationThread(
            id = summary.id,
            title = summary.title,
            subtitle = summary.subtitle,
            unreadCount = summary.unreadCount,
            syncCursor = summary.syncCursor.ifBlank { existingThread?.syncCursor.orEmpty() },
            messages = mergeSummaryMessages(existingThread, summary),
            draftMessage = existingThread?.draftMessage.orEmpty(),
            draftAttachments = existingThread?.draftAttachments.orEmpty(),
            isVoiceRecording = existingThread?.isVoiceRecording ?: false,
            voiceRecordingSeconds = existingThread?.voiceRecordingSeconds ?: 0,
        )
    }
    val remoteIds = remoteSummaries.map { it.id }.toSet()
    val localOnlyThreads = localState.threads.filterNot { it.id in remoteIds }
    return ConversationThreadsState(threads = mergedThreads + localOnlyThreads)
}

internal fun applyLocalMessageStatus(
    state: ConversationThreadsState,
    conversationId: String,
    messageId: String,
    targetStatus: ChatMessageDeliveryStatus,
): ConversationThreadsState {
    return state.copy(
        threads = state.threads.map { thread ->
            if (thread.id != conversationId) {
                thread
            } else {
                val existingMessage = thread.messages.firstOrNull { it.id == messageId }
                if (existingMessage == null) {
                    thread
                } else {
                    val nextStatus = resolveNextDeliveryStatus(existingMessage.deliveryStatus, targetStatus)
                    val nextUnreadCount = if (
                        shouldDecreaseUnreadCount(
                            existingMessage = existingMessage,
                            nextStatus = nextStatus,
                        )
                    ) {
                        max(0, thread.unreadCount - 1)
                    } else {
                        thread.unreadCount
                    }
                    thread.copy(
                        unreadCount = nextUnreadCount,
                        messages = thread.messages.map { message ->
                            if (message.id == messageId) {
                                message.copy(deliveryStatus = nextStatus)
                            } else {
                                message
                            }
                        }
                    )
                }
            }
        }
    )
}

internal fun applyRemoteMessageUpdate(
    state: ConversationThreadsState,
    conversationId: String,
    remoteMessage: ChatMessageItem,
): ConversationThreadsState {
    return state.copy(
        threads = state.threads.map { thread ->
            if (thread.id != conversationId) {
                thread
            } else {
                val existingMessage = thread.messages.firstOrNull { it.id == remoteMessage.id }
                val nextUnreadCount = if (
                    existingMessage != null &&
                    shouldDecreaseUnreadCount(
                        existingMessage = existingMessage,
                        nextStatus = remoteMessage.deliveryStatus,
                    )
                ) {
                    max(0, thread.unreadCount - 1)
                } else {
                    thread.unreadCount
                }
                thread.copy(
                    unreadCount = nextUnreadCount,
                    messages = thread.messages.map { message ->
                        if (message.id == remoteMessage.id) remoteMessage else message
                    }
                )
            }
        }
    )
}

internal fun clearConversationUnreadCount(
    state: ConversationThreadsState,
    conversationId: String,
): ConversationThreadsState {
    return state.copy(
        threads = state.threads.map { thread ->
            if (thread.id == conversationId) {
                thread.copy(unreadCount = 0)
            } else {
                thread
            }
        }
    )
}

private fun mergeSummaryMessages(
    existingThread: InboxConversationThread?,
    summary: RemoteConversationSummary,
): List<ChatMessageItem> {
    val previewMessage = createSummaryPreviewMessage(summary)
    if (existingThread == null) {
        return listOf(previewMessage)
    }
    if (existingThread.messages.isEmpty()) {
        return listOf(previewMessage)
    }
    return if (
        existingThread.messages.size == 1 &&
        existingThread.messages.first().id == previewMessage.id
    ) {
        listOf(previewMessage)
    } else {
        existingThread.messages
    }
}

private fun createSummaryPreviewMessage(
    summary: RemoteConversationSummary,
): ChatMessageItem {
    return ChatMessageItem(
        id = "${summary.id}-remote-preview",
        senderDisplayName = summary.title,
        body = summary.preview,
        timestampLabel = summary.timestampLabel,
        isFromCurrentUser = false,
        statusUpdatedAt = summary.timestampLabel,
    )
}

private fun resolveNextDeliveryStatus(
    existingStatus: ChatMessageDeliveryStatus,
    targetStatus: ChatMessageDeliveryStatus,
): ChatMessageDeliveryStatus {
    return when (targetStatus) {
        ChatMessageDeliveryStatus.Delivered -> when (existingStatus) {
            ChatMessageDeliveryStatus.Read,
            ChatMessageDeliveryStatus.Recalled,
            ChatMessageDeliveryStatus.Failed,
            -> existingStatus

            else -> ChatMessageDeliveryStatus.Delivered
        }

        ChatMessageDeliveryStatus.Read -> when (existingStatus) {
            ChatMessageDeliveryStatus.Recalled,
            ChatMessageDeliveryStatus.Failed,
            -> existingStatus

            else -> ChatMessageDeliveryStatus.Read
        }

        else -> targetStatus
    }
}

private fun shouldDecreaseUnreadCount(
    existingMessage: ChatMessageItem,
    nextStatus: ChatMessageDeliveryStatus,
): Boolean {
    return !existingMessage.isFromCurrentUser &&
        existingMessage.deliveryStatus != ChatMessageDeliveryStatus.Read &&
        existingMessage.deliveryStatus != ChatMessageDeliveryStatus.Recalled &&
        nextStatus == ChatMessageDeliveryStatus.Read
}
