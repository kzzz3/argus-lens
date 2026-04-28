package com.kzzz3.argus.lens.core.data.media

import android.content.Context
import com.kzzz3.argus.lens.core.datastore.media.AndroidMediaFileDataSource
import com.kzzz3.argus.lens.core.network.createAppGson
import com.kzzz3.argus.lens.core.network.createAppRetrofit
import com.kzzz3.argus.lens.core.network.media.MediaApiService
import com.kzzz3.argus.lens.session.SessionRepository

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
        sessionRepository = sessionRepository,
        mediaApiService = retrofit.create(MediaApiService::class.java),
        mediaFileDataSource = AndroidMediaFileDataSource(context),
        gson = gson,
    )
}
