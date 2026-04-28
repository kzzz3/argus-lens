package com.kzzz3.argus.lens.core.data.media

import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind

data class MediaUploadSession(
    val conversationId: String,
    val attachmentKind: ChatDraftAttachmentKind,
    val uploadSessionId: String,
    val attachmentId: String,
    val uploadUrl: String,
    val objectKey: String,
    val uploadHeaders: Map<String, String>,
    val uploaded: Boolean,
    val contentType: String,
    val contentLength: Long,
    val expiresAt: String,
)

sealed interface MediaRepositoryResult {
    data class Success(val session: MediaUploadSession) : MediaRepositoryResult
    data class FinalizeSuccess(val metadata: FinalizedAttachmentMetadata) : MediaRepositoryResult
    data class UploadSuccess(
        val sessionId: String,
        val objectKey: String,
    ) : MediaRepositoryResult
    data class DownloadSuccess(val savedPath: String) : MediaRepositoryResult
    data class Failure(
        val code: String?,
        val message: String,
    ) : MediaRepositoryResult
}

interface MediaRepository {
    suspend fun createUploadSession(
        conversationId: String,
        attachmentKind: ChatDraftAttachmentKind,
        fileName: String,
        contentType: String,
        contentLength: Long,
        durationSeconds: Int? = null,
    ): MediaRepositoryResult

    suspend fun finalizeUploadSession(
        sessionId: String,
        conversationId: String,
        fileName: String,
        contentType: String,
        contentLength: Long,
        objectKey: String,
    ): MediaRepositoryResult

    suspend fun uploadContent(
        uploadSession: MediaUploadSession,
        contentBytes: ByteArray,
    ): MediaRepositoryResult

    suspend fun downloadAttachment(
        attachmentId: String,
        fileName: String,
    ): MediaRepositoryResult
}
