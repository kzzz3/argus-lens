package com.kzzz3.argus.lens.data.media

import com.google.gson.Gson
import com.kzzz3.argus.lens.data.auth.ApiErrorResponse
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull


class RemoteMediaRepository(
    private val sessionRepository: SessionRepository,
    private val mediaApiService: MediaApiService,
    private val gson: Gson = Gson(),
) : MediaRepository {
    override suspend fun createUploadSession(
        conversationId: String,
        attachmentKind: ChatDraftAttachmentKind,
        fileName: String,
        contentType: String,
        contentLength: Long,
        durationSeconds: Int?,
    ): MediaRepositoryResult {
        val session = sessionRepository.loadSession()
        val accessToken = session.accessToken
        if (accessToken.isBlank()) {
            return MediaRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        val request = UploadSessionRequestBody(
            conversationId = conversationId,
            attachmentType = attachmentKind.toBackendValue(),
            fileName = fileName,
            contentType = contentType,
            contentLength = contentLength,
            durationSeconds = durationSeconds,
        )

        return try {
            val response = mediaApiService.createUploadSession(
                authorizationHeader = "Bearer $accessToken",
                request = request,
            )

            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return MediaRepositoryResult.Failure(
                    code = "EMPTY_UPLOAD_SESSION",
                    message = "Media service returned an empty upload session response.",
                )
                MediaRepositoryResult.Success(
                    body.toMediaUploadSession(
                        conversationId = conversationId,
                        attachmentKind = attachmentKind,
                        contentType = contentType,
                        contentLength = contentLength,
                    )
                )
            }
        } catch (_: IOException) {
            MediaRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach media service.",
            )
        }
    }

    override suspend fun finalizeUploadSession(
        sessionId: String,
        conversationId: String,
        fileName: String,
        contentType: String,
        contentLength: Long,
        objectKey: String,
    ): MediaRepositoryResult {
        val session = sessionRepository.loadSession()
        val accessToken = session.accessToken
        if (accessToken.isBlank()) {
            return MediaRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        val request = FinalizeUploadSessionRequest(
            fileName = fileName,
            contentType = contentType,
            contentLength = contentLength,
            objectKey = objectKey,
            conversationId = conversationId.ifBlank { null },
        )

        return try {
            val response = mediaApiService.finalizeUploadSession(
                authorizationHeader = "Bearer $accessToken",
                sessionId = sessionId,
                request = request,
            )

            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                val body = response.body() ?: return MediaRepositoryResult.Failure(
                    code = "EMPTY_FINALIZATION_RESPONSE",
                    message = "Media service returned an empty finalize response.",
                )
                MediaRepositoryResult.FinalizeSuccess(body.toFinalizedAttachmentMetadata())
            }
        } catch (_: IOException) {
            MediaRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach media service.",
            )
        }
    }


    override suspend fun uploadContent(
        uploadSession: MediaUploadSession,
        contentBytes: ByteArray,
    ): MediaRepositoryResult {
        val currentSession = sessionRepository.loadSession()
        val accessToken = currentSession.accessToken
        if (accessToken.isBlank()) {
            return MediaRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "No active session token.",
            )
        }

        if (uploadSession.uploaded) {
            return MediaRepositoryResult.UploadSuccess(
                sessionId = uploadSession.uploadSessionId,
                objectKey = uploadSession.objectKey,
            )
        }

        val mediaType = uploadSession.contentType.toMediaTypeOrNull()
        val requestBody = contentBytes.toRequestBody(mediaType)

        return try {
            val response = mediaApiService.uploadContent(
                authorizationHeader = "Bearer $accessToken",
                sessionId = uploadSession.uploadSessionId,
                uploadHeaders = uploadSession.uploadHeaders,
                body = requestBody,
            )

            if (!response.isSuccessful) {
                parseFailure(response.code(), response.errorBody()?.string().orEmpty())
            } else {
                MediaRepositoryResult.UploadSuccess(
                    sessionId = uploadSession.uploadSessionId,
                    objectKey = uploadSession.objectKey,
                )
            }
        } catch (_: IOException) {
            MediaRepositoryResult.Failure(
                code = "NETWORK_UNAVAILABLE",
                message = "Cannot reach media service.",
            )
        }
    }


    private fun UploadSessionResponse.toMediaUploadSession(
        conversationId: String,
        attachmentKind: ChatDraftAttachmentKind,
        contentType: String,
        contentLength: Long,
    ): MediaUploadSession {
        return MediaUploadSession(
            conversationId = conversationId,
            attachmentKind = attachmentKind,
            uploadSessionId = uploadSessionId,
            attachmentId = attachmentId,
            uploadUrl = uploadUrl,
            objectKey = objectKey,
            uploadHeaders = uploadHeaders,
            uploaded = uploaded,
            contentType = contentType,
            contentLength = contentLength,
            expiresAt = expiresAt,
        )
    }

    private fun FinalizeUploadSessionResponse.toFinalizedAttachmentMetadata(): FinalizedAttachmentMetadata {
        return FinalizedAttachmentMetadata(
            attachmentId = attachmentId,
            sessionId = sessionId,
            conversationId = conversationId,
            attachmentType = attachmentType,
            fileName = fileName,
            contentType = contentType,
            contentLength = contentLength,
            objectKey = objectKey,
            uploadUrl = uploadUrl,
            createdAt = createdAt,
        )
    }

    private fun parseFailure(
        httpCode: Int,
        rawBody: String,
    ): MediaRepositoryResult.Failure {
        val parsed = runCatching {
            gson.fromJson(rawBody, ApiErrorResponse::class.java)
        }.getOrNull()

        val code = parsed?.code ?: if (httpCode == 401) {
            "INVALID_CREDENTIALS"
        } else {
            null
        }

        return MediaRepositoryResult.Failure(
            code = code,
            message = parsed?.message ?: "Media upload session request failed with HTTP $httpCode.",
        )
    }
}
