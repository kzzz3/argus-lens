package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState

data class NewFriendsActionResult(
    val status: FriendRequestStatusState,
    val friends: List<FriendEntry>? = null,
    val conversationThreadsState: ConversationThreadsState? = null,
)

class NewFriendsCoordinator(
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository,
) {
    suspend fun loadRequests(
        fallbackSnapshot: FriendRequestsSnapshot,
    ): FriendRequestStatusState {
        return when (val result = friendRepository.listFriendRequests()) {
            is FriendRepositoryResult.RequestsSuccess -> createFriendRequestStatusState(
                snapshot = result.snapshot,
                message = null,
                isError = false,
            )
            is FriendRepositoryResult.Failure -> createFriendRequestStatusState(
                snapshot = fallbackSnapshot,
                message = result.message,
                isError = true,
            )
            else -> createFriendRequestStatusState(
                snapshot = fallbackSnapshot,
                message = null,
                isError = false,
            )
        }
    }

    suspend fun accept(
        requestId: String,
        session: AppSessionState,
        currentSnapshot: FriendRequestsSnapshot,
        currentConversationState: ConversationThreadsState,
    ): NewFriendsActionResult {
        return when (val result = friendRepository.acceptFriendRequest(requestId)) {
            is FriendRepositoryResult.FriendsSuccess -> {
                val status = refreshRequestStatus(
                    fallbackSnapshot = currentSnapshot,
                    message = result.message ?: "Friend request accepted.",
                    isError = false,
                )
                val refreshedFriends = when (val friendsResult = friendRepository.listFriends()) {
                    is FriendRepositoryResult.FriendsSuccess -> friendsResult.friends
                    else -> null
                }
                val refreshedConversations = if (session.isAuthenticated) {
                    conversationRepository.loadOrCreateConversationThreads(
                        accountId = session.accountId,
                        currentUserDisplayName = session.displayName,
                    )
                } else {
                    currentConversationState
                }
                NewFriendsActionResult(
                    status = status,
                    friends = refreshedFriends,
                    conversationThreadsState = refreshedConversations,
                )
            }
            is FriendRepositoryResult.Failure -> failure(currentSnapshot, result.message)
            else -> unchanged(currentSnapshot)
        }
    }

    suspend fun reject(
        requestId: String,
        currentSnapshot: FriendRequestsSnapshot,
    ): NewFriendsActionResult {
        return when (val result = friendRepository.rejectFriendRequest(requestId)) {
            is FriendRepositoryResult.FriendRequestSuccess -> NewFriendsActionResult(
                status = refreshRequestStatus(
                    fallbackSnapshot = currentSnapshot,
                    message = result.message ?: "Friend request rejected.",
                    isError = false,
                )
            )
            is FriendRepositoryResult.Failure -> failure(currentSnapshot, result.message)
            else -> unchanged(currentSnapshot)
        }
    }

    suspend fun ignore(
        requestId: String,
        currentSnapshot: FriendRequestsSnapshot,
    ): NewFriendsActionResult {
        return when (val result = friendRepository.ignoreFriendRequest(requestId)) {
            is FriendRepositoryResult.FriendRequestSuccess -> NewFriendsActionResult(
                status = refreshRequestStatus(
                    fallbackSnapshot = currentSnapshot,
                    message = result.message ?: "Friend request ignored.",
                    isError = false,
                )
            )
            is FriendRepositoryResult.Failure -> failure(currentSnapshot, result.message)
            else -> unchanged(currentSnapshot)
        }
    }

    private suspend fun refreshRequestStatus(
        fallbackSnapshot: FriendRequestsSnapshot,
        message: String,
        isError: Boolean,
    ): FriendRequestStatusState {
        val snapshot = when (val refreshedRequests = friendRepository.listFriendRequests()) {
            is FriendRepositoryResult.RequestsSuccess -> refreshedRequests.snapshot
            else -> fallbackSnapshot
        }
        return createFriendRequestStatusState(
            snapshot = snapshot,
            message = message,
            isError = isError,
        )
    }

    private fun failure(
        currentSnapshot: FriendRequestsSnapshot,
        message: String,
    ): NewFriendsActionResult {
        return NewFriendsActionResult(
            status = createFriendRequestStatusState(
                snapshot = currentSnapshot,
                message = message,
                isError = true,
            )
        )
    }

    private fun unchanged(currentSnapshot: FriendRequestsSnapshot): NewFriendsActionResult {
        return NewFriendsActionResult(
            status = createFriendRequestStatusState(
                snapshot = currentSnapshot,
                message = null,
                isError = false,
            )
        )
    }
}
