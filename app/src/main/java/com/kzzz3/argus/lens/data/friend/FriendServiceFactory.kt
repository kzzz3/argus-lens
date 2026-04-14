package com.kzzz3.argus.lens.data.friend

import com.kzzz3.argus.lens.data.network.createAppGson
import com.kzzz3.argus.lens.data.network.createAppRetrofit
import com.kzzz3.argus.lens.data.session.SessionRepository

fun createFriendRepository(
    sessionRepository: SessionRepository,
): FriendRepository {
    val gson = createAppGson()
    val retrofit = createAppRetrofit(gson = gson)

    return RemoteFriendRepository(
        sessionRepository = sessionRepository,
        friendApiService = retrofit.create(FriendApiService::class.java),
        gson = gson,
    )
}
