package com.kzzz3.argus.lens.data.friend

data class FriendEntry(
    val accountId: String,
    val displayName: String,
    val note: String,
)

sealed interface FriendRepositoryResult {
    data class Success(
        val friends: List<FriendEntry>,
        val message: String? = null,
    ) : FriendRepositoryResult

    data class Failure(
        val code: String?,
        val message: String,
    ) : FriendRepositoryResult
}

interface FriendRepository {
    suspend fun listFriends(): FriendRepositoryResult
    suspend fun addFriend(friendAccountId: String): FriendRepositoryResult
}
