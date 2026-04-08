package com.kzzz3.argus.lens.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.createInboxSampleThreads

class LocalConversationStore(
    private val snapshotDao: ConversationSnapshotDao,
    private val dao: LocalConversationDao,
    private val gson: Gson,
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
                            )
                        },
                    draftMessage = row.conversation.draftMessage,
                    draftAttachments = if (row.draftAttachments.isNotEmpty()) {
                        row.draftAttachments
                            .sortedBy { it.sortOrder }
                            .map { attachment ->
                                ChatDraftAttachment(
                                    id = attachment.id,
                                    kind = com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind.valueOf(attachment.kind),
                                    title = attachment.title,
                                    summary = attachment.summary,
                                )
                            }
                    } else {
                        restoreDraftAttachments(row.conversation.draftAttachmentsJson)
                    },
                    isVoiceRecording = row.conversation.isVoiceRecording,
                    voiceRecordingSeconds = row.conversation.voiceRecordingSeconds,
                )
            }
        }

        val legacyEntity = snapshotDao.findByKey(conversationSnapshotKey(accountId)) ?: return null
        val type = object : TypeToken<List<InboxConversationThread>>() {}.type
        val legacyThreads: List<InboxConversationThread> = gson.fromJson(legacyEntity.payloadJson, type)
        saveConversationThreads(accountId, legacyThreads)
        snapshotDao.deleteByKey(conversationSnapshotKey(accountId))
        return legacyThreads
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
                draftMessage = thread.draftMessage,
                draftAttachmentsJson = "",
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
                    timestampLabel = message.timestampLabel,
                    isFromCurrentUser = message.isFromCurrentUser,
                    deliveryStatus = message.deliveryStatus.name,
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
        snapshotDao.deleteByKey(conversationSnapshotKey(accountId))
    }

    private fun restoreDraftAttachments(
        payloadJson: String,
    ): List<ChatDraftAttachment> {
        if (payloadJson.isBlank()) return emptyList()
        val type = object : TypeToken<List<ChatDraftAttachment>>() {}.type
        return gson.fromJson(payloadJson, type)
    }
}

class LocalConversationCoordinator(
    private val store: LocalConversationStore,
) {
    fun createPreviewState(
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        return ConversationThreadsState(
            threads = createInboxSampleThreads(currentUserDisplayName = currentUserDisplayName),
        )
    }

    suspend fun loadOrCreateConversationThreads(
        accountId: String,
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        val storedThreads = store.loadConversationThreads(accountId)
        if (!storedThreads.isNullOrEmpty()) {
            return ConversationThreadsState(storedThreads)
        }

        val seededState = createPreviewState(currentUserDisplayName)
        store.saveConversationThreads(accountId, seededState.threads)
        return seededState
    }

    suspend fun saveConversationThreads(
        accountId: String,
        state: ConversationThreadsState,
    ) {
        if (accountId.isBlank()) return
        store.saveConversationThreads(accountId, state.threads)
    }

    suspend fun clearConversationThreads(accountId: String) {
        if (accountId.isBlank()) return
        store.clearConversationThreads(accountId)
    }
}

fun createLocalConversationStore(
    context: Context,
): LocalConversationStore {
    val database = ArgusLensDatabase.getInstance(context)
    return LocalConversationStore(
        snapshotDao = database.conversationSnapshotDao(),
        dao = database.localConversationDao(),
        gson = Gson(),
    )
}

fun createLocalConversationCoordinator(
    context: Context,
): LocalConversationCoordinator {
    return LocalConversationCoordinator(createLocalConversationStore(context))
}

private fun conversationSnapshotKey(accountId: String): String {
    val normalizedAccountId = accountId.trim().ifEmpty { "anonymous" }
    return "local-conversation-threads:$normalizedAccountId"
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
