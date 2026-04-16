package com.kzzz3.argus.lens.data.local

import android.content.Context
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.applyLocalMessageStatus
import com.kzzz3.argus.lens.data.conversation.clearConversationUnreadCount
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachment
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread

class LocalConversationStore(
    private val dao: LocalConversationDao,
) {
    suspend fun loadConversationThreads(accountId: String): List<InboxConversationThread>? {
        val rows = dao.getConversationsWithMessages(accountId)
        if (rows.isNotEmpty()) {
            return rows.map { row ->
                InboxConversationThread(
                    id = row.conversation.id,
                    title = row.conversation.title,
                    subtitle = row.conversation.subtitle,
                    unreadCount = row.conversation.unreadCount,
                    syncCursor = row.conversation.syncCursor,
                    messages = row.messages
                        .sortedBy { it.sortOrder }
                        .map { message ->
                            ChatMessageItem(
                                id = message.id,
                                senderDisplayName = message.senderDisplayName,
                                body = message.body,
                                timestampLabel = message.timestampLabel,
                                isFromCurrentUser = message.isFromCurrentUser,
                                deliveryStatus = ChatMessageDeliveryStatus.valueOf(message.deliveryStatus),
                                statusUpdatedAt = message.statusUpdatedAt,
                                attachment = if (
                                    !message.attachmentId.isNullOrBlank() ||
                                        !message.attachmentType.isNullOrBlank() ||
                                        !message.attachmentFileName.isNullOrBlank()
                                ) {
                                    ChatMessageAttachment(
                                        attachmentId = message.attachmentId,
                                        attachmentType = message.attachmentType.orEmpty(),
                                        fileName = message.attachmentFileName.orEmpty(),
                                        contentType = message.attachmentContentType.orEmpty(),
                                        contentLength = message.attachmentContentLength,
                                    )
                                } else {
                                    null
                                },
                            )
                        },
                    draftMessage = row.conversation.draftMessage,
                    draftAttachments = row.draftAttachments
                        .sortedBy { it.sortOrder }
                        .map { attachment ->
                            ChatDraftAttachment(
                                id = attachment.id,
                                kind = com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind.valueOf(attachment.kind),
                                title = attachment.title,
                                summary = attachment.summary,
                            )
                        },
                    isVoiceRecording = row.conversation.isVoiceRecording,
                    voiceRecordingSeconds = row.conversation.voiceRecordingSeconds,
                )
            }
        }
        return null
    }

    suspend fun saveConversationThreads(
        accountId: String,
        threads: List<InboxConversationThread>,
    ) {
        val conversationEntities = threads.mapIndexed { index, thread ->
            LocalConversationEntity(
                storageId = conversationStorageId(accountId, thread.id),
                id = thread.id,
                accountId = accountId,
                title = thread.title,
                subtitle = thread.subtitle,
                unreadCount = thread.unreadCount,
                syncCursor = thread.syncCursor,
                draftMessage = thread.draftMessage,
                isVoiceRecording = thread.isVoiceRecording,
                voiceRecordingSeconds = thread.voiceRecordingSeconds,
                sortOrder = index,
            )
        }

        val messageEntities = threads.flatMap { thread ->
            thread.messages.mapIndexed { index, message ->
                LocalMessageEntity(
                    storageId = messageStorageId(accountId, thread.id, message.id),
                    id = message.id,
                    accountId = accountId,
                    conversationId = thread.id,
                    conversationStorageId = conversationStorageId(accountId, thread.id),
                    senderDisplayName = message.senderDisplayName,
                    body = message.body,
                    attachmentId = message.attachment?.attachmentId,
                    attachmentType = message.attachment?.attachmentType,
                    attachmentFileName = message.attachment?.fileName,
                    attachmentContentType = message.attachment?.contentType,
                    attachmentContentLength = message.attachment?.contentLength ?: 0L,
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.isFromCurrentUser,
                    deliveryStatus = message.deliveryStatus.name,
                    statusUpdatedAt = message.statusUpdatedAt,
                    sortOrder = index,
                )
            }
        }

        val draftAttachmentEntities = threads.flatMap { thread ->
            thread.draftAttachments.mapIndexed { index, attachment ->
                LocalDraftAttachmentEntity(
                    storageId = draftAttachmentStorageId(accountId, thread.id, attachment.id),
                    id = attachment.id,
                    accountId = accountId,
                    conversationId = thread.id,
                    conversationStorageId = conversationStorageId(accountId, thread.id),
                    kind = attachment.kind.name,
                    title = attachment.title,
                    summary = attachment.summary,
                    sortOrder = index,
                )
            }
        }

        dao.replaceAccountSnapshot(accountId, conversationEntities, messageEntities, draftAttachmentEntities)
    }

    suspend fun clearConversationThreads(accountId: String) {
        dao.replaceAccountSnapshot(accountId, emptyList(), emptyList(), emptyList())
    }
}

class LocalConversationRepository(
    private val store: LocalConversationStore,
) : ConversationRepository {
    override fun createPreviewState(
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        return ConversationThreadsState()
    }

    override suspend fun loadOrCreateConversationThreads(
        accountId: String,
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        val storedThreads = store.loadConversationThreads(accountId)
        if (!storedThreads.isNullOrEmpty()) {
            return ConversationThreadsState(storedThreads)
        }
        return ConversationThreadsState()
    }

    override suspend fun saveConversationThreads(
        accountId: String,
        state: ConversationThreadsState,
    ) {
        if (accountId.isBlank()) return
        store.saveConversationThreads(accountId, state.threads)
    }

    override suspend fun clearConversationThreads(accountId: String) {
        if (accountId.isBlank()) return
        store.clearConversationThreads(accountId)
    }

    override suspend fun refreshConversationMessages(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        return state
    }

    override suspend fun refreshConversationDetail(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        return state
    }

    override suspend fun sendMessage(
        state: ConversationThreadsState,
        conversationId: String,
        localMessageId: String,
        body: String,
        attachment: ChatMessageAttachment?,
    ): ConversationThreadsState {
        return state
    }

    override suspend fun acknowledgeMessageDelivery(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        return applyLocalMessageStatus(
            state = state,
            conversationId = conversationId,
            messageId = messageId,
            targetStatus = ChatMessageDeliveryStatus.Delivered,
        )
    }

    override suspend fun acknowledgeMessageRead(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        return applyLocalMessageStatus(
            state = state,
            conversationId = conversationId,
            messageId = messageId,
            targetStatus = ChatMessageDeliveryStatus.Read,
        )
    }

    override suspend fun recallMessage(
        state: ConversationThreadsState,
        conversationId: String,
        messageId: String,
    ): ConversationThreadsState {
        return state
    }

    override suspend fun markConversationReadRemote(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        return clearConversationUnreadCount(
            state = state,
            conversationId = conversationId,
        )
    }

    override fun markConversationAsRead(
        state: ConversationThreadsState,
        conversationId: String,
    ): ConversationThreadsState {
        return clearConversationUnreadCount(
            state = state,
            conversationId = conversationId,
        )
    }

    override fun updateConversationFromChatState(
        state: ConversationThreadsState,
        updatedState: ChatState,
    ): ConversationThreadsState {
        return state.copy(
            threads = state.threads.map { thread ->
                if (thread.id == updatedState.conversationId) {
                    thread.copy(
                        messages = updatedState.messages,
                        draftMessage = updatedState.draftMessage,
                        draftAttachments = updatedState.draftAttachments,
                        isVoiceRecording = updatedState.isVoiceRecording,
                        voiceRecordingSeconds = updatedState.voiceRecordingSeconds,
                    )
                } else {
                    thread
                }
            }
        )
    }

    override fun resolveOutgoingMessages(
        state: ConversationThreadsState,
        conversationId: String,
        messageIds: List<String>,
    ): ConversationThreadsState {
        return state.copy(
            threads = state.threads.map { thread ->
                if (thread.id == conversationId) {
                    thread.copy(
                        messages = thread.messages.map { message ->
                            if (message.id in messageIds) {
                                message.copy(
                                    deliveryStatus = if (shouldFailOutgoingMessage(message)) {
                                        ChatMessageDeliveryStatus.Failed
                                    } else {
                                        ChatMessageDeliveryStatus.Sent
                                    }
                                )
                            } else {
                                message
                            }
                        }
                    )
                } else {
                    thread
                }
            }
        )
    }

    override fun resolveDeliveredMessages(
        state: ConversationThreadsState,
        conversationId: String,
        messageIds: List<String>,
    ): ConversationThreadsState {
        return state.copy(
            threads = state.threads.map { thread ->
                if (thread.id == conversationId) {
                    thread.copy(
                        messages = thread.messages.map { message ->
                            if (
                                message.id in messageIds &&
                                message.deliveryStatus == ChatMessageDeliveryStatus.Sent
                            ) {
                                message.copy(deliveryStatus = ChatMessageDeliveryStatus.Delivered)
                            } else {
                                message
                            }
                        }
                    )
                } else {
                    thread
                }
            }
        )
    }
}

fun createLocalConversationStore(
    context: Context,
): LocalConversationStore {
    val database = ArgusLensDatabase.getInstance(context)
    return LocalConversationStore(
        dao = database.localConversationDao(),
    )
}

fun createLocalConversationRepository(
    context: Context,
) : ConversationRepository {
    return LocalConversationRepository(createLocalConversationStore(context))
}

private fun conversationStorageId(
    accountId: String,
    conversationId: String,
): String {
    return "${accountId.trim()}:${conversationId.trim()}"
}

private fun messageStorageId(
    accountId: String,
    conversationId: String,
    messageId: String,
): String {
    return "${accountId.trim()}:${conversationId.trim()}:${messageId.trim()}"
}

private fun draftAttachmentStorageId(
    accountId: String,
    conversationId: String,
    attachmentId: String,
): String {
    return "${accountId.trim()}:${conversationId.trim()}:${attachmentId.trim()}"
}

private fun shouldFailOutgoingMessage(
    message: ChatMessageItem,
): Boolean {
    return message.body.startsWith("[Video]") || message.body.contains("#fail", ignoreCase = true)
}

