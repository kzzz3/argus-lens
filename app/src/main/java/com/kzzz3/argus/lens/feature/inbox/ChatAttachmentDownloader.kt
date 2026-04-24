package com.kzzz3.argus.lens.feature.inbox

import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult

data class ChatAttachmentDownloadResult(
    val message: String?,
    val isError: Boolean,
)

class ChatAttachmentDownloader(
    private val mediaRepository: MediaRepository,
) {
    suspend fun downloadAttachment(
        attachmentId: String,
        fileName: String,
    ): ChatAttachmentDownloadResult {
        return when (val downloadResult = mediaRepository.downloadAttachment(attachmentId, fileName)) {
            is MediaRepositoryResult.DownloadSuccess -> ChatAttachmentDownloadResult(
                message = "Saved to ${downloadResult.savedPath}",
                isError = false,
            )
            is MediaRepositoryResult.Failure -> ChatAttachmentDownloadResult(
                message = downloadResult.message,
                isError = true,
            )
            else -> ChatAttachmentDownloadResult(
                message = null,
                isError = false,
            )
        }
    }
}
