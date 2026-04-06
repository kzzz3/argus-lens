package com.kzzz3.argus.lens.feature.contacts

data class ContactsUiState(
    val title: String,
    val subtitle: String,
    val draftConversationName: String,
    val draftLabel: String,
    val draftPlaceholder: String,
    val createConversationActionLabel: String,
    val isCreateConversationEnabled: Boolean,
    val contacts: List<ContactEntryUiState>,
    val backActionLabel: String,
)

data class ContactEntryUiState(
    val conversationId: String,
    val displayName: String,
    val supportingLabel: String,
    val lastSeenPreview: String,
)
