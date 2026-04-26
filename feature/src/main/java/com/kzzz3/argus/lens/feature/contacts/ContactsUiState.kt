package com.kzzz3.argus.lens.feature.contacts

data class ContactsUiState(
    val title: String,
    val subtitle: String,
    val statusMessage: String?,
    val isStatusError: Boolean,
    val newFriendsLabel: String,
    val newFriendsSubtitle: String,
    val draftFriendAccountId: String,
    val addFriendLabel: String,
    val addFriendPlaceholder: String,
    val addFriendActionLabel: String,
    val isAddFriendEnabled: Boolean,
    val contacts: List<ContactEntryUiState>,
    val backActionLabel: String,
)

data class ContactEntryUiState(
    val conversationId: String,
    val accountId: String,
    val displayName: String,
    val supportingLabel: String,
    val lastSeenPreview: String,
)
