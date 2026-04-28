package com.kzzz3.argus.lens.core.data.friend

import com.kzzz3.argus.lens.core.network.createAppGson
import com.kzzz3.argus.lens.core.network.createAppRetrofit
import com.kzzz3.argus.lens.core.network.friend.FriendApiService
import com.kzzz3.argus.lens.session.SessionRepository

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
