package com.kzzz3.argus.lens.data.media

import com.google.gson.Gson
import com.kzzz3.argus.lens.model.conversation.ChatDraftAttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaApiModelsTest {
    private val gson = Gson()

    @Test
    fun uploadSessionRequestBody_matchesCortexCreateUploadSessionContract() {
        val body = UploadSessionRequestBody(
            attachmentType = ChatDraftAttachmentKind.Image.toBackendValue(),
            fileName = "design-spec.png",
            estimatedBytes = 12L,
        )

        val json = gson.toJson(body)

        assertTrue(json.contains("\"attachmentType\":\"IMAGE\""))
        assertTrue(json.contains("\"fileName\":\"design-spec.png\""))
        assertTrue(json.contains("\"estimatedBytes\":12"))
        assertFalse(json.contains("conversationId"))
        assertFalse(json.contains("contentLength"))
        assertFalse(json.contains("durationSeconds"))
    }

    @Test
    fun uploadSessionResponse_readsCortexCreateUploadSessionResponse() {
        val response = gson.fromJson(
            """
                {
                  "sessionId":"session-1",
                  "attachmentType":"IMAGE",
                  "objectKey":"tester/image/session-1/design-spec.png",
                  "uploadUrl":"https://media.example.com/api/v1/media/upload-sessions/session-1/content",
                  "maxPayloadBytes":5242880,
                  "uploadToken":"token-1",
                  "uploadHeaders":{"X-Upload-Session":"session-1"},
                  "instructions":"Upload with PUT.",
                  "uploaded":false
                }
            """.trimIndent(),
            UploadSessionResponse::class.java,
        )

        assertEquals("session-1", response.sessionId)
        assertEquals("tester/image/session-1/design-spec.png", response.objectKey)
        assertEquals(5_242_880L, response.maxPayloadBytes)
        assertEquals("token-1", response.uploadToken)
        assertEquals("session-1", response.uploadHeaders["X-Upload-Session"])
    }
}
