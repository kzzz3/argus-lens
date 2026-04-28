package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.core.data.friend.FriendEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ContactsFeatureState(
    val contactsState: ContactsState = ContactsState(),
    val friends: List<FriendEntry> = emptyList(),
    val friendRequestsSnapshot: FriendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
    val friendRequestsStatusMessage: String? = null,
    val friendRequestsStatusError: Boolean = false,
)

class ContactsFeatureStateHolder(
    initialState: ContactsFeatureState = ContactsFeatureState(),
) {
    private val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<ContactsFeatureState> = mutableState.asStateFlow()

    fun replaceContactsState(contactsState: ContactsState) {
        mutableState.update { state -> state.copy(contactsState = contactsState) }
    }

    fun replaceFriends(friends: List<FriendEntry>) {
        mutableState.update { state -> state.copy(friends = friends) }
    }

    fun replaceFriendRequestsSnapshot(snapshot: FriendRequestsSnapshot) {
        mutableState.update { state -> state.copy(friendRequestsSnapshot = snapshot) }
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        mutableState.update { state ->
            state.copy(
                friendRequestsSnapshot = statusState.snapshot,
                friendRequestsStatusMessage = statusState.message,
                friendRequestsStatusError = statusState.isError,
            )
        }
    }

    fun resetFriendRequestStatus() {
        mutableState.update { state ->
            state.copy(
                friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
                friendRequestsStatusMessage = null,
                friendRequestsStatusError = false,
            )
        }
    }

    fun reset() {
        mutableState.value = ContactsFeatureState()
    }
}
