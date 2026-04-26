package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.synchronizeActiveConversation
import com.kzzz3.argus.lens.model.session.AppSessionState

data class OpenConversationResult(
    val conversationId: String,
    val conversationThreadsState: ConversationThreadsState,
)

data class AddFriendResult(
    val contactsState: ContactsState,
    val friendRequestsSnapshot: FriendRequestsSnapshot?,
)

class ContactsCoordinator(
    private val conversationRepository: ConversationRepository,
    private val friendRepository: FriendRepository,
) {
    suspend fun loadFriends(): List<FriendEntry>? {
        return when (val result = friendRepository.listFriends()) {
            is FriendRepositoryResult.FriendsSuccess -> result.friends
            else -> null
        }
    }

    suspend fun openConversation(
        session: AppSessionState,
        requestedConversationId: String,
        friends: List<FriendEntry>,
        state: ConversationThreadsState,
    ): OpenConversationResult {
        val target = resolveDirectConversationTarget(
            currentAccountId = session.accountId,
            requestedConversationId = requestedConversationId,
            friends = friends,
            existingThreadIds = state.threads.map { it.id }.toSet(),
        )
        val resolvedTarget = if (!target.requiresRefresh && !target.requiresPlaceholder) {
            ResolvedConversationTarget(target.conversationId, state)
        } else {
            resolveMissingConversation(session, target, state)
        }
        val markedState = conversationRepository.markConversationAsRead(
            state = ensureDirectConversationPlaceholderIfNeeded(resolvedTarget.state, target, resolvedTarget.conversationId),
            conversationId = resolvedTarget.conversationId,
        )
        return OpenConversationResult(
            conversationId = resolvedTarget.conversationId,
            conversationThreadsState = synchronizeActiveConversation(
                state = markedState,
                conversationId = resolvedTarget.conversationId,
                conversationRepository = conversationRepository,
            ),
        )
    }

    suspend fun addFriend(
        currentContactsState: ContactsState,
        friendAccountId: String,
    ): AddFriendResult {
        return when (val friendResult = friendRepository.sendFriendRequest(friendAccountId)) {
            is FriendRepositoryResult.FriendRequestSuccess -> {
                val refreshedSnapshot = when (val refreshed = friendRepository.listFriendRequests()) {
                    is FriendRepositoryResult.RequestsSuccess -> refreshed.snapshot
                    else -> null
                }
                AddFriendResult(
                    contactsState = createContactsStatusUpdate(
                        currentState = currentContactsState,
                        message = friendResult.message ?: "Friend request sent.",
                        isError = false,
                    ),
                    friendRequestsSnapshot = refreshedSnapshot,
                )
            }
            is FriendRepositoryResult.Failure -> AddFriendResult(
                contactsState = createContactsStatusUpdate(
                    currentState = currentContactsState,
                    message = friendResult.message,
                    isError = true,
                ),
                friendRequestsSnapshot = null,
            )
            else -> AddFriendResult(currentContactsState, null)
        }
    }

    private suspend fun resolveMissingConversation(
        session: AppSessionState,
        target: DirectConversationTarget,
        state: ConversationThreadsState,
    ): ResolvedConversationTarget {
        if (session.isAuthenticated && target.requiresRefresh) {
            val refreshed = conversationRepository.loadOrCreateConversationThreads(
                accountId = session.accountId,
                currentUserDisplayName = session.displayName,
            )
            return ResolvedConversationTarget(
                conversationId = refreshed.threads.firstOrNull { thread -> thread.id == target.conversationId }?.id
                    ?: target.conversationId,
                state = refreshed,
            )
        }
        return ResolvedConversationTarget(target.conversationId, state)
    }

    private fun ensureDirectConversationPlaceholderIfNeeded(
        state: ConversationThreadsState,
        target: DirectConversationTarget,
        resolvedConversationId: String,
    ): ConversationThreadsState {
        return if (target.requiresPlaceholder && state.threads.none { it.id == resolvedConversationId }) {
            ensureDirectConversationPlaceholder(
                state = state,
                conversationId = resolvedConversationId,
                title = target.placeholderTitle,
            )
        } else {
            state
        }
    }

    private data class ResolvedConversationTarget(
        val conversationId: String,
        val state: ConversationThreadsState,
    )
}
