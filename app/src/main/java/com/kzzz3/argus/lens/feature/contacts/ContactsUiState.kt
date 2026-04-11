package com.kzzz3.argus.lens.feature.contacts

data class ContactsUiState(
    val title: String,
    val subtitle: String,
    val statusMessage: String?,
    val isStatusError: Boolean,
    val draftConversationName: String,
    val draftFriendAccountId: String,
    val addFriendLabel: String,
    val addFriendPlaceholder: String,
    val addFriendActionLabel: String,
    val isAddFriendEnabled: Boolean,
    val draftLabel: String,
    val draftPlaceholder: String,
    val creationModeLabel: String,
    val toggleCreationModeActionLabel: String,
    val createConversationActionLabel: String,
    val isCreateConversationEnabled: Boolean,
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
