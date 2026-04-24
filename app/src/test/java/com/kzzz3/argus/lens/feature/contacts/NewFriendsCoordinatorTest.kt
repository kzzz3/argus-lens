package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendRequestEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class NewFriendsCoordinatorTest {

    @Test
    fun accept_refreshesFriendsRequestsAndConversationThreads() = runBlocking {
        val friendRepository = FakeFriendRepository(
            acceptResult = FriendRepositoryResult.FriendsSuccess(
                friends = listOf(FriendEntry("alice", "Alice", "")),
                message = "Accepted",
            ),
            friendsResult = FriendRepositoryResult.FriendsSuccess(
                friends = listOf(FriendEntry("alice", "Alice", "")),
            ),
            requestsResult = FriendRepositoryResult.RequestsSuccess(FriendRequestsSnapshot(emptyList(), emptyList())),
        )
        val conversationRepository = FakeConversationRepository()
        val coordinator = NewFriendsCoordinator(friendRepository, conversationRepository)

        val result = coordinator.accept(
            requestId = "request-1",
            session = createAuthenticatedSession("tester", "Tester", "token"),
            currentSnapshot = FriendRequestsSnapshot(
                incoming = listOf(FriendRequestEntry("request-1", "alice", "Alice", "INCOMING", "PENDING", "")),
                outgoing = emptyList(),
            ),
            currentConversationState = ConversationThreadsState(),
        )

        assertEquals("Accepted", result.status.message)
        assertEquals(false, result.status.isError)
        assertEquals(listOf(FriendEntry("alice", "Alice", "")), result.friends)
        assertEquals(true, conversationRepository.loadedThreads)
    }

    @Test
    fun reject_updatesStatusAndRefreshesRequestsWithoutReloadingConversations() = runBlocking {
        val refreshedSnapshot = FriendRequestsSnapshot(emptyList(), emptyList())
        val friendRepository = FakeFriendRepository(
            rejectResult = FriendRepositoryResult.FriendRequestSuccess(
                request = FriendRequestEntry("request-1", "alice", "Alice", "INCOMING", "REJECTED", ""),
                message = "Rejected",
            ),
            requestsResult = FriendRepositoryResult.RequestsSuccess(refreshedSnapshot),
        )
        val conversationRepository = FakeConversationRepository()
        val coordinator = NewFriendsCoordinator(friendRepository, conversationRepository)

        val result = coordinator.reject(
            requestId = "request-1",
            currentSnapshot = FriendRequestsSnapshot(
                incoming = listOf(FriendRequestEntry("request-1", "alice", "Alice", "INCOMING", "PENDING", "")),
                outgoing = emptyList(),
            ),
        )

        assertEquals("Rejected", result.status.message)
        assertEquals(false, result.status.isError)
        assertEquals(refreshedSnapshot, result.status.snapshot)
        assertEquals(false, conversationRepository.loadedThreads)
    }

    @Test
    fun ignore_failureKeepsSnapshotAndReportsError() = runBlocking {
        val currentSnapshot = FriendRequestsSnapshot(
            incoming = listOf(FriendRequestEntry("request-1", "alice", "Alice", "INCOMING", "PENDING", "")),
            outgoing = emptyList(),
        )
        val coordinator = NewFriendsCoordinator(
            friendRepository = FakeFriendRepository(
                ignoreResult = FriendRepositoryResult.Failure(code = "DENIED", message = "Cannot ignore"),
            ),
            conversationRepository = FakeConversationRepository(),
        )

        val result = coordinator.ignore(
            requestId = "request-1",
            currentSnapshot = currentSnapshot,
        )

        assertEquals(currentSnapshot, result.status.snapshot)
        assertEquals("Cannot ignore", result.status.message)
        assertEquals(true, result.status.isError)
    }

    private class FakeFriendRepository(
        private val acceptResult: FriendRepositoryResult = FriendRepositoryResult.Failure(null, "Unexpected accept"),
        private val rejectResult: FriendRepositoryResult = FriendRepositoryResult.Failure(null, "Unexpected reject"),
        private val ignoreResult: FriendRepositoryResult = FriendRepositoryResult.Failure(null, "Unexpected ignore"),
        private val friendsResult: FriendRepositoryResult = FriendRepositoryResult.FriendsSuccess(emptyList()),
        private val requestsResult: FriendRepositoryResult = FriendRepositoryResult.RequestsSuccess(FriendRequestsSnapshot(emptyList(), emptyList())),
    ) : FriendRepository {
        override suspend fun listFriends(): FriendRepositoryResult = friendsResult
        override suspend fun sendFriendRequest(friendAccountId: String): FriendRepositoryResult = FriendRepositoryResult.Failure(null, "Unexpected send")
        override suspend fun listFriendRequests(): FriendRepositoryResult = requestsResult
        override suspend fun acceptFriendRequest(requestId: String): FriendRepositoryResult = acceptResult
        override suspend fun rejectFriendRequest(requestId: String): FriendRepositoryResult = rejectResult
        override suspend fun ignoreFriendRequest(requestId: String): FriendRepositoryResult = ignoreResult
    }

    private class FakeConversationRepository : ConversationRepository {
        var loadedThreads = false

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()

        override suspend fun loadOrCreateConversationThreads(
            accountId: String,
            currentUserDisplayName: String,
        ): ConversationThreadsState {
            loadedThreads = true
            return ConversationThreadsState()
        }

        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) = Unit
        override suspend fun clearConversationThreads(accountId: String) = Unit
        override suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun sendMessage(state: ConversationThreadsState, conversationId: String, localMessageId: String, body: String, attachment: ChatMessageAttachment?): ConversationThreadsState = state
        override suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState = state
        override fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
        override fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
    }
}
