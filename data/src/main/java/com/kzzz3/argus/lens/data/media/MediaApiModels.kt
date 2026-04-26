package com.kzzz3.argus.lens.data.media

import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind

data class UploadSessionRequestBody(
    val conversationId: String,
    val attachmentType: String,
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
    val durationSeconds: Int? = null,
)

data class UploadSessionResponse(
    val uploadSessionId: String,
    val attachmentId: String,
    val uploadUrl: String,
    val expiresAt: String,
    val objectKey: String,
    val uploadHeaders: Map<String, String>,
    val uploaded: Boolean,
)

data class FinalizeUploadSessionRequest(
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
    val objectKey: String,
    val conversationId: String?,
)

data class FinalizeUploadSessionResponse(
    val attachmentId: String,
    val sessionId: String,
    val accountId: String,
    val conversationId: String?,
    val attachmentType: String,
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
    val objectKey: String,
    val uploadUrl: String,
    val createdAt: String,
)

data class FinalizedAttachmentMetadata(
    val attachmentId: String,
    val sessionId: String,
    val conversationId: String?,
    val attachmentType: String,
    val fileName: String,
    val contentType: String,
    val contentLength: Long,
    val objectKey: String,
    val uploadUrl: String,
    val createdAt: String,
)

enum class MediaAttachmentType(
    val draftKind: ChatDraftAttachmentKind,
    val backendValue: String,
) {
    Image(ChatDraftAttachmentKind.Image, "image"),
    Video(ChatDraftAttachmentKind.Video, "video"),
    Voice(ChatDraftAttachmentKind.Voice, "voice");

    companion object {
        fun fromDraftKind(kind: ChatDraftAttachmentKind): MediaAttachmentType {
            return values().first { it.draftKind == kind }
        }
    }
}

fun ChatDraftAttachmentKind.toBackendValue(): String {
    return MediaAttachmentType.fromDraftKind(this).backendValue
}
