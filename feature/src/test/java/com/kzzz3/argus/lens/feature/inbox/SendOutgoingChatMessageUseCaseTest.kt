package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.media.FinalizedAttachmentMetadata
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.data.media.MediaUploadSession
import kotlin.text.Charsets
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SendOutgoingChatMessageUseCaseTest {

    @Test
    fun invoke_sendsTextThroughConversationRepositoryWithoutMediaUpload() = runBlocking {
        val initialState = sampleState(
            ChatMessageItem(
                id = "message-0",
                senderDisplayName = "Tester",
                body = "hello",
                timestampLabel = "now",
                deliveryStatus = ChatMessageDeliveryStatus.Sending,
                isFromCurrentUser = true,
            )
        )
        val conversationRepository = CapturingConversationRepository()
        val useCase = SendOutgoingChatMessageUseCase(
            conversationRepository = conversationRepository,
            mediaRepository = UnusedMediaRepository,
        )

        val result = useCase(
            state = initialState,
            conversationId = "conv-1",
            message = initialState.threads.single().messages.single(),
        )

        assertEquals("conv-1", conversationRepository.sentConversationId)
        assertEquals("message-0", conversationRepository.sentLocalMessageId)
        assertEquals("hello", conversationRepository.sentBody)
        assertEquals(null, conversationRepository.sentAttachment)
        assertEquals(null, result.failureMessage)
        assertEquals(ChatMessageDeliveryStatus.Sent, result.state.threads.single().messages.single().deliveryStatus)
    }

    @Test
    fun invoke_uploadsDraftAttachmentThenSendsFinalizedMessage() = runBlocking {
        val initialState = sampleState(
            ChatMessageItem(
                id = "message-1",
                senderDisplayName = "Tester",
                body = "[Image] Local gallery placeholder ready to send",
                timestampLabel = "now",
                deliveryStatus = ChatMessageDeliveryStatus.Sending,
                isFromCurrentUser = true,
                attachment = ChatMessageAttachment(
                    attachmentId = null,
                    attachmentType = "IMAGE",
                    fileName = "photo.jpg",
                    contentType = "",
                    contentLength = 9,
                ),
            )
        )
        val conversationRepository = CapturingConversationRepository()
        val mediaRepository = CapturingMediaRepository()
        val useCase = SendOutgoingChatMessageUseCase(
            conversationRepository = conversationRepository,
            mediaRepository = mediaRepository,
        )

        val result = useCase(
            state = initialState,
            conversationId = "conv-1",
            message = initialState.threads.single().messages.single(),
        )

        assertEquals("conv-1", mediaRepository.createdConversationId)
        assertEquals(ChatDraftAttachmentKind.Image, mediaRepository.createdAttachmentKind)
        assertEquals("photo.jpg", mediaRepository.createdFileName)
        assertEquals("image/jpeg", mediaRepository.createdContentType)
        assertEquals("photo.jpg|Image", String(mediaRepository.uploadedBytes!!, Charsets.UTF_8))
        assertEquals("upload-1", mediaRepository.finalizedSessionId)
        assertEquals("object-1", mediaRepository.finalizedObjectKey)

        assertEquals("conv-1", conversationRepository.sentConversationId)
        assertEquals("message-1", conversationRepository.sentLocalMessageId)
        assertEquals("photo.jpg", conversationRepository.sentBody)
        assertNotNull(conversationRepository.sentAttachment)
        assertEquals("attachment-1", conversationRepository.sentAttachment!!.attachmentId)
        assertEquals("IMAGE", conversationRepository.sentAttachment!!.attachmentType)
        assertEquals("photo.jpg", conversationRepository.sentAttachment!!.fileName)
        assertEquals(null, result.failureMessage)
        assertEquals(ChatMessageDeliveryStatus.Sent, result.state.threads.single().messages.single().deliveryStatus)
    }

    @Test
    fun invoke_marksAttachmentMessageFailedWhenUploadSessionFails() = runBlocking {
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
        val useCase = SendOutgoingChatMessageUseCase(
            conversationRepository = CapturingConversationRepository(),
            mediaRepository = FailingMediaRepository,
        )

        val result = useCase(
            state = initialState,
            conversationId = "conv-1",
            message = initialState.threads.single().messages.single(),
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
        var sentAttachment: ChatMessageAttachment? = null

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
            sentAttachment = attachment
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

    private class CapturingMediaRepository : MediaRepository {
        var createdConversationId: String? = null
        var createdAttachmentKind: ChatDraftAttachmentKind? = null
        var createdFileName: String? = null
        var createdContentType: String? = null
        var uploadedBytes: ByteArray? = null
        var finalizedSessionId: String? = null
        var finalizedObjectKey: String? = null

        override suspend fun createUploadSession(
            conversationId: String,
            attachmentKind: ChatDraftAttachmentKind,
            fileName: String,
            contentType: String,
            contentLength: Long,
            durationSeconds: Int?,
        ): MediaRepositoryResult {
            createdConversationId = conversationId
            createdAttachmentKind = attachmentKind
            createdFileName = fileName
            createdContentType = contentType
            return MediaRepositoryResult.Success(
                MediaUploadSession(
                    conversationId = conversationId,
                    attachmentKind = attachmentKind,
                    uploadSessionId = "upload-1",
                    attachmentId = "attachment-1",
                    uploadUrl = "https://upload.example/photo.jpg",
                    objectKey = "object-1",
                    uploadHeaders = emptyMap(),
                    uploaded = false,
                    contentType = contentType,
                    contentLength = contentLength,
                    expiresAt = "2026-04-28T00:00:00Z",
                )
            )
        }

        override suspend fun uploadContent(uploadSession: MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult {
            uploadedBytes = contentBytes
            return MediaRepositoryResult.UploadSuccess(
                sessionId = uploadSession.uploadSessionId,
                objectKey = uploadSession.objectKey,
            )
        }

        override suspend fun finalizeUploadSession(
            sessionId: String,
            conversationId: String,
            fileName: String,
            contentType: String,
            contentLength: Long,
            objectKey: String,
        ): MediaRepositoryResult {
            finalizedSessionId = sessionId
            finalizedObjectKey = objectKey
            return MediaRepositoryResult.FinalizeSuccess(
                FinalizedAttachmentMetadata(
                    attachmentId = "attachment-1",
                    sessionId = sessionId,
                    conversationId = conversationId,
                    attachmentType = "IMAGE",
                    fileName = fileName,
                    contentType = contentType,
                    contentLength = contentLength,
                    objectKey = objectKey,
                    uploadUrl = "https://upload.example/photo.jpg",
                    createdAt = "2026-04-28T00:00:01Z",
                )
            )
        }

        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")
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

        override suspend fun finalizeUploadSession(
            sessionId: String,
            conversationId: String,
            fileName: String,
            contentType: String,
            contentLength: Long,
            objectKey: String,
        ): MediaRepositoryResult = MediaRepositoryResult.Failure(code = null, message = "Unused")

        override suspend fun uploadContent(uploadSession: MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")

        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult =
            MediaRepositoryResult.Failure(code = null, message = "Unused")
    }

    private object UnusedMediaRepository : MediaRepository {
        override suspend fun createUploadSession(
            conversationId: String,
            attachmentKind: ChatDraftAttachmentKind,
            fileName: String,
            contentType: String,
            contentLength: Long,
            durationSeconds: Int?,
        ): MediaRepositoryResult = error("Not expected")

        override suspend fun finalizeUploadSession(
            sessionId: String,
            conversationId: String,
            fileName: String,
            contentType: String,
            contentLength: Long,
            objectKey: String,
        ): MediaRepositoryResult = error("Not expected")

        override suspend fun uploadContent(uploadSession: MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult =
            error("Not expected")

        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult =
            error("Not expected")
    }
}
