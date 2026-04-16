package com.kzzz3.argus.lens.data.friend

data class FriendResponse(
    val accountId: String,
    val displayName: String,
    val note: String,
)

data class FriendRequestResponse(
    val requestId: String,
    val accountId: String,
    val displayName: String,
    val direction: String,
    val status: String,
    val note: String,
)

data class PendingFriendRequestsResponse(
    val incoming: List<FriendRequestResponse>,
    val outgoing: List<FriendRequestResponse>,
)

data class AddFriendRequestBody(
    val friendAccountId: String,
)
