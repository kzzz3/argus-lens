package com.kzzz3.argus.lens.core.data.media

import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind

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
