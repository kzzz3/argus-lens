package com.kzzz3.argus.lens.data.media

import android.content.Context
import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppRetrofit
import com.kzzz3.argus.lens.data.session.SessionRepository

fun createMediaRepository(
    sessionRepository: SessionRepository,
    context: Context,
): MediaRepository {
    return createRemoteMediaRepository(sessionRepository, context)
}

private fun createRemoteMediaRepository(
    sessionRepository: SessionRepository,
    context: Context,
): MediaRepository {
    val gson = createAppGson()
    val retrofit = createAppRetrofit(gson = gson)

    return RemoteMediaRepository(
        context = context,
        sessionRepository = sessionRepository,
        mediaApiService = retrofit.create(MediaApiService::class.java),
        gson = gson,
    )
}
