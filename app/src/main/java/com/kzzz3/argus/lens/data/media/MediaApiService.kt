package com.kzzz3.argus.lens.data.media

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface MediaApiService {
    @POST("api/v1/media/upload-sessions")
    suspend fun createUploadSession(
        @Header("Authorization") authorizationHeader: String,
        @Body request: UploadSessionRequestBody,
    ): Response<UploadSessionResponse>

    @POST("api/v1/media/upload-sessions/{sessionId}/finalize")
    suspend fun finalizeUploadSession(
        @Header("Authorization") authorizationHeader: String,
        @Path("sessionId") sessionId: String,
        @Body request: FinalizeUploadSessionRequest,
    ): Response<FinalizeUploadSessionResponse>
}
