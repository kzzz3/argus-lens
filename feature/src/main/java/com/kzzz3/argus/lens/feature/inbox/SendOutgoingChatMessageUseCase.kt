package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.media.FinalizedAttachmentMetadata
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import kotlin.text.Charsets

data class OutgoingDispatchResult(
    val state: ConversationThreadsState,
    val failureMessage: String? = null,
)

class SendOutgoingChatMessageUseCase(
    private val conversationRepository: ConversationRepository,
    private val mediaRepository: MediaRepository,
) {
    suspend operator fun invoke(
        state: ConversationThreadsState,
        conversationId: String,
        message: ChatMessageItem,
    ): OutgoingDispatchResult {
        val attachment = message.attachment
        if (attachment == null) {
            return OutgoingDispatchResult(
                state = conversationRepository.sendMessage(
                    state = state,
                    conversationId = conversationId,
                    localMessageId = message.id,
                    body = message.body,
                    attachment = null,
                )
            )
        }

        val finalizedAttachment = if (attachment.attachmentId.isNullOrBlank()) {
            val attachmentKind = attachment.toDraftAttachmentKind()
            val fileName = attachment.fileName.ifBlank {
                buildMediaPlaceholderFileName(conversationId, message.id, attachmentKind)
            }
            when (val uploadSessionResult = mediaRepository.createUploadSession(
                conversationId = conversationId,
                attachmentKind = attachmentKind,
                fileName = fileName,
                contentType = attachment.contentType.ifBlank { mediaContentTypeFor(attachmentKind) },
                contentLength = attachment.contentLength,
                durationSeconds = null,
            )) {
                is MediaRepositoryResult.Success -> {
                    val session = uploadSessionResult.session
                    val placeholderBytes = buildMediaPlaceholderBytes(fileName, attachmentKind)
                    when (val uploadResult = mediaRepository.uploadContent(session, placeholderBytes)) {
                        is MediaRepositoryResult.UploadSuccess -> {
                            when (val finalizeResult = mediaRepository.finalizeUploadSession(
                                sessionId = session.uploadSessionId,
                                conversationId = conversationId,
                                fileName = fileName,
                                contentType = session.contentType,
                                contentLength = placeholderBytes.size.toLong(),
                                objectKey = session.objectKey,
                            )) {
                                is MediaRepositoryResult.FinalizeSuccess -> finalizeResult.metadata.toChatMessageAttachment()
                                is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                                    state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                                    failureMessage = finalizeResult.message,
                                )
                                else -> null
                            }
                        }
                        is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                            state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                            failureMessage = uploadResult.message,
                        )
                        else -> null
                    }
                }
                is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                    state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                    failureMessage = uploadSessionResult.message,
                )
                else -> null
            }
        } else {
            attachment
        }

        if (finalizedAttachment == null) {
            return OutgoingDispatchResult(
                state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                failureMessage = "File upload failed.",
            )
        }

        return OutgoingDispatchResult(
            state = conversationRepository.sendMessage(
                state = state,
                conversationId = conversationId,
                localMessageId = message.id,
                body = finalizedAttachment.fileName,
                attachment = finalizedAttachment,
            )
        )
    }
}

fun markOutgoingMessagesFailed(
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
                            message.copy(deliveryStatus = ChatMessageDeliveryStatus.Failed)
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

fun isMediaPlaceholderBody(body: String): Boolean {
    return body.startsWith("[Image]") || body.startsWith("[Video]")
}

fun mediaAttachmentKindFromPlaceholder(body: String): ChatDraftAttachmentKind? {
    return when {
        body.startsWith("[Image]") -> ChatDraftAttachmentKind.Image
        body.startsWith("[Video]") -> ChatDraftAttachmentKind.Video
        else -> null
    }
}

fun buildFileMessageBodyWithFinalizedAttachment(
    kind: ChatDraftAttachmentKind,
    metadata: FinalizedAttachmentMetadata,
): String {
    return buildString {
        append("[File] ")
        append(kind.name)
        append(" · ")
        append(metadata.fileName)
        append(" · Download or Save As")
        append(" · attachmentId=")
        append(metadata.attachmentId)
        append(" · objectKey=")
        append(metadata.objectKey)
        append(" · uploadUrl=")
        append(metadata.uploadUrl)
    }
}

private fun mediaContentTypeFor(kind: ChatDraftAttachmentKind): String {
    return when (kind) {
        ChatDraftAttachmentKind.Image -> "image/jpeg"
        ChatDraftAttachmentKind.Video -> "video/mp4"
        ChatDraftAttachmentKind.Voice -> "audio/mpeg"
    }
}

private fun FinalizedAttachmentMetadata.toChatMessageAttachment(): ChatMessageAttachment {
    return ChatMessageAttachment(
        attachmentId = attachmentId,
        attachmentType = attachmentType,
        fileName = fileName,
        contentType = contentType,
        contentLength = contentLength,
    )
}

private fun ChatMessageAttachment.toDraftAttachmentKind(): ChatDraftAttachmentKind {
    return when (attachmentType.uppercase()) {
        "IMAGE" -> ChatDraftAttachmentKind.Image
        "VIDEO" -> ChatDraftAttachmentKind.Video
        else -> ChatDraftAttachmentKind.Voice
    }
}

private fun buildMediaPlaceholderBytes(fileName: String, kind: ChatDraftAttachmentKind): ByteArray {
    return "$fileName|${kind.name}".toByteArray(Charsets.UTF_8)
}

private fun buildMediaPlaceholderFileName(conversationId: String, localMessageId: String, kind: ChatDraftAttachmentKind): String {
    val extension = when (kind) {
        ChatDraftAttachmentKind.Image -> "jpg"
        ChatDraftAttachmentKind.Video -> "mp4"
        else -> "bin"
    }
    return "$conversationId-${kind.name.lowercase()}-$localMessageId.$extension"
}
