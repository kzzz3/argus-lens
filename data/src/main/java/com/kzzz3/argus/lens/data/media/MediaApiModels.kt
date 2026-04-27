package com.kzzz3.argus.lens.data.media

import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind

data class UploadSessionRequestBody(
    val attachmentType: String,
    val fileName: String,
    val estimatedBytes: Long,
)

data class UploadSessionResponse(
    val sessionId: String,
    val attachmentType: String,
    val maxPayloadBytes: Long,
    val uploadToken: String,
    val uploadUrl: String,
    val objectKey: String,
    val uploadHeaders: Map<String, String>,
    val instructions: String,
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
    Image(ChatDraftAttachmentKind.Image, "IMAGE"),
    Video(ChatDraftAttachmentKind.Video, "VIDEO"),
    Voice(ChatDraftAttachmentKind.Voice, "VOICE");

    companion object {
        fun fromDraftKind(kind: ChatDraftAttachmentKind): MediaAttachmentType {
            return values().first { it.draftKind == kind }
        }
    }
}

fun ChatDraftAttachmentKind.toBackendValue(): String {
    return MediaAttachmentType.fromDraftKind(this).backendValue
}
