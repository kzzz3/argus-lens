package com.kzzz3.argus.lens.data.friend

data class FriendResponse(
    val accountId: String,
    val displayName: String,
    val note: String,
)

data class AddFriendRequestBody(
    val friendAccountId: String,
)
