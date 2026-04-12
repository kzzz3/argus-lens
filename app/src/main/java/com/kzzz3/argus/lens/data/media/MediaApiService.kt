package com.kzzz3.argus.lens.data.media

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface MediaApiService {
    @POST("api/v1/media/upload-sessions")
    suspend fun createUploadSession(
        @Header("Authorization") authorizationHeader: String,
        @Body request: UploadSessionRequestBody,
    ): Response<UploadSessionResponse>
}
