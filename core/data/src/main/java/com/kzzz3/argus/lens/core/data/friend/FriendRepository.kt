package com.kzzz3.argus.lens.core.data.friend

data class FriendEntry(
    val accountId: String,
    val displayName: String,
    val note: String,
)

data class FriendRequestEntry(
    val requestId: String,
    val accountId: String,
    val displayName: String,
    val direction: String,
    val status: String,
    val note: String,
)

data class FriendRequestsSnapshot(
    val incoming: List<FriendRequestEntry>,
    val outgoing: List<FriendRequestEntry>,
)

sealed interface FriendRepositoryResult {
    data class FriendsSuccess(
        val friends: List<FriendEntry>,
        val message: String? = null,
    ) : FriendRepositoryResult

    data class FriendRequestSuccess(
        val request: FriendRequestEntry,
        val message: String? = null,
    ) : FriendRepositoryResult

    data class RequestsSuccess(
        val snapshot: FriendRequestsSnapshot,
    ) : FriendRepositoryResult

    data class Failure(
        val code: String?,
        val message: String,
    ) : FriendRepositoryResult
}

interface FriendRepository {
    suspend fun listFriends(): FriendRepositoryResult
    suspend fun sendFriendRequest(friendAccountId: String): FriendRepositoryResult
    suspend fun listFriendRequests(): FriendRepositoryResult
    suspend fun acceptFriendRequest(requestId: String): FriendRepositoryResult
    suspend fun rejectFriendRequest(requestId: String): FriendRepositoryResult
    suspend fun ignoreFriendRequest(requestId: String): FriendRepositoryResult
}
