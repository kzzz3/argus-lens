package com.kzzz3.argus.lens.data.media

import com.google.gson.Gson
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.model.session.AppSessionState
import java.io.InputStream
import kotlinx.coroutines.runBlocking
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Response

class RemoteMediaRepositoryTest {
    @Test
    fun downloadAttachment_delegatesDownloadedContentToFileDataSource() = runBlocking {
        val apiService = FakeMediaApiService(
            downloadResponse = Response.success("downloaded image bytes".toResponseBody()),
        )
        val fileDataSource = RecordingMediaFileDataSource(savedPath = "downloads/attachment-1.png")
        val repository = RemoteMediaRepository(
            sessionRepository = FakeSessionRepository(accessToken = "token-1"),
            mediaApiService = apiService,
            mediaFileDataSource = fileDataSource,
            gson = Gson(),
        )

        val result = repository.downloadAttachment(
            attachmentId = "attachment-1",
            fileName = "../profile image.png",
        )

        assertEquals("Bearer token-1", apiService.downloadAuthorizationHeader)
        assertEquals("attachment-1", apiService.downloadAttachmentId)
        assertEquals("attachment-1", fileDataSource.savedAttachmentId)
        assertEquals("../profile image.png", fileDataSource.savedFileName)
        assertArrayEquals("downloaded image bytes".toByteArray(), fileDataSource.savedContent)
        assertEquals(MediaRepositoryResult.DownloadSuccess(savedPath = "downloads/attachment-1.png"), result)
    }

    private class RecordingMediaFileDataSource(
        private val savedPath: String,
    ) : MediaFileDataSource {
        var savedAttachmentId: String? = null
            private set
        var savedFileName: String? = null
            private set
        var savedContent: ByteArray = byteArrayOf()
            private set

        override fun saveDownloadedAttachment(
            attachmentId: String,
            fileName: String,
            content: InputStream,
        ): String {
            savedAttachmentId = attachmentId
            savedFileName = fileName
            savedContent = content.readBytes()
            return savedPath
        }
    }

    private class FakeSessionRepository(
        private val accessToken: String,
    ) : SessionRepository {
        override suspend fun loadSession(): AppSessionState = AppSessionState(
            isAuthenticated = true,
            accountId = "tester",
            displayName = "Argus Tester",
        )

        override suspend fun loadCredentials(): SessionCredentials = SessionCredentials(accessToken = accessToken)
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) = Unit
        override suspend fun clearSession() = Unit
    }

    private class FakeMediaApiService(
        private val downloadResponse: Response<ResponseBody>,
    ) : MediaApiService {
        var downloadAuthorizationHeader: String? = null
            private set
        var downloadAttachmentId: String? = null
            private set

        override suspend fun createUploadSession(
            authorizationHeader: String,
            request: UploadSessionRequestBody,
        ): Response<UploadSessionResponse> = error("Not used in test")

        override suspend fun finalizeUploadSession(
            authorizationHeader: String,
            sessionId: String,
            request: FinalizeUploadSessionRequest,
        ): Response<FinalizeUploadSessionResponse> = error("Not used in test")

        override suspend fun uploadContent(
            authorizationHeader: String,
            sessionId: String,
            uploadHeaders: Map<String, String>,
            body: RequestBody,
        ): Response<Unit> = error("Not used in test")

        override suspend fun downloadAttachment(
            authorizationHeader: String,
            attachmentId: String,
        ): Response<ResponseBody> {
            downloadAuthorizationHeader = authorizationHeader
            downloadAttachmentId = attachmentId
            return downloadResponse
        }
    }
}
