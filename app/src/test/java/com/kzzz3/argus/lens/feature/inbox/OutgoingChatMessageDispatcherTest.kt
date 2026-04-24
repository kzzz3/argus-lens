package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.data.media.MediaUploadSession
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OutgoingChatMessageDispatcherTest {

    @Test
    fun dispatchOutgoingChatMessage_sendsTextThroughConversationRepository() = runBlocking {
        val initialState = sampleState(
            ChatMessageItem(
                id = "message-1",
                senderDisplayName = "Tester",
                body = "hello",
                timestampLabel = "now",
                deliveryStatus = ChatMessageDeliveryStatus.Sending,
                isFromCurrentUser = true,
            )
        )
        val conversationRepository = CapturingConversationRepository()

        val result = dispatchOutgoingChatMessage(
            state = initialState,
            conversationId = "conv-1",
            message = initialState.threads.single().messages.single(),
            conversationRepository = conversationRepository,
            mediaRepository = UnusedMediaRepository,
        )

        assertEquals("conv-1", conversationRepository.sentConversationId)
        assertEquals("message-1", conversationRepository.sentLocalMessageId)
        assertEquals("hello", conversationRepository.sentBody)
        assertEquals(null, result.failureMessage)
        assertEquals(ChatMessageDeliveryStatus.Sent, result.state.threads.single().messages.single().deliveryStatus)
    }

    @Test
    fun dispatchOutgoingChatMessage_marksAttachmentMessageFailedWhenUploadSessionFails() = runBlocking {
        val initialState = sampleState(
            ChatMessageItem(
                id = "message-2",
                senderDisplayName = "Tester",
                body = "[Image] Local gallery placeholder ready to send",
                timestampLabel = "now",
                deliveryStatus = ChatMessageDeliveryStatus.Sending,
                isFromCurrentUser = true,
                attachment = ChatMessageAttachment(
                    attachmentId = null,
                    attachmentType = "IMAGE",
                    fileName = "",
                    contentType = "",
                    contentLength = 10,
                ),
            )
        )

        val result = dispatchOutgoingChatMessage(
            state = initialState,
            conversationId = "conv-1",
            message = initialState.threads.single().messages.single(),
            conversationRepository = CapturingConversationRepository(),
            mediaRepository = FailingMediaRepository,
        )

        assertEquals("Upload failed", result.failureMessage)
        assertEquals(ChatMessageDeliveryStatus.Failed, result.state.threads.single().messages.single().deliveryStatus)
    }

    private fun sampleState(message: ChatMessageItem): ConversationThreadsState {
        return ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = "conv-1",
                    title = "Alice",
                    subtitle = "",
                    unreadCount = 0,
                    messages = listOf(message),
                )
            )
        )
    }

    private class CapturingConversationRepository : ConversationRepository {
        var sentConversationId: String? = null
        var sentLocalMessageId: String? = null
        var sentBody: String? = null

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) = Unit
        override suspend fun clearConversationThreads(accountId: String) = Unit
        override suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state

        override suspend fun sendMessage(
            state: ConversationThreadsState,
            conversationId: String,
            localMessageId: String,
            body: String,
            attachment: ChatMessageAttachment?,
        ): ConversationThreadsState {
            sentConversationId = conversationId
            sentLocalMessageId = localMessageId
            sentBody = body
            return state.copy(
                threads = state.threads.map { thread ->
                    thread.copy(
                        messages = thread.messages.map { message ->
                            if (message.id == localMessageId) {
                                message.copy(deliveryStatus = ChatMessageDeliveryStatus.Sent)
                            } else {
                                message
                            }
                        }
                    )
                }
            )
        }

        override suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState = state
        override fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
        override fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
    }

    private object FailingMediaRepository : MediaRepository {
        override suspend fun createUploadSession(
            conversationId: String,
            attachmentKind: ChatDraftAttachmentKind,
            fileName: String,
            contentType: String,
            contentLength: Long,
            durationSeconds: Int?,
        ): MediaRepositoryResult = MediaRepositoryResult.Failure(code = "UPLOAD_FAILED", message = "Upload failed")

        override suspend fun finalizeUploadSession(sessionId: String, conversationId: String, fileName: String, contentType: String, contentLength: Long, objectKey: String): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")

        override suspend fun uploadContent(uploadSession: MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")

        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")
    }

    private object UnusedMediaRepository : MediaRepository {
        override suspend fun createUploadSession(conversationId: String, attachmentKind: ChatDraftAttachmentKind, fileName: String, contentType: String, contentLength: Long, durationSeconds: Int?): MediaRepositoryResult =
            error("Not expected")

        override suspend fun finalizeUploadSession(sessionId: String, conversationId: String, fileName: String, contentType: String, contentLength: Long, objectKey: String): MediaRepositoryResult =
            error("Not expected")

        override suspend fun uploadContent(uploadSession: MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult =
            error("Not expected")

        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult =
            error("Not expected")
    }
}
