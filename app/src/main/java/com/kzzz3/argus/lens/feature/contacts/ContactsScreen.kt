package com.kzzz3.argus.lens.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

@Composable
fun ContactsScreen(
    state: ContactsUiState,
    onAction: (ContactsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08131E),
                        Color(0xFF122A40),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x182BFFC8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD8EBFB)
                )
            }
        }

        state.statusMessage?.let { message ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (state.isStatusError) Color(0x33FF6F61) else Color(0x297AF5C9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0x142D4258),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = state.draftFriendAccountId,
                    onValueChange = { onAction(ContactsAction.UpdateDraftFriendAccountId(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = state.addFriendLabel) },
                    placeholder = { Text(text = state.addFriendPlaceholder) },
                    maxLines = 1,
                )
                Button(
                    onClick = { onAction(ContactsAction.SubmitAddFriend) },
                    enabled = state.isAddFriendEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = state.addFriendActionLabel)
                }
                OutlinedTextField(
                    value = state.draftConversationName,
                    onValueChange = { onAction(ContactsAction.UpdateDraftConversationName(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = state.draftLabel) },
                    placeholder = { Text(text = state.draftPlaceholder) },
                    maxLines = 1,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.creationModeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFAAC9E3),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = state.toggleCreationModeActionLabel,
                        modifier = Modifier.clickable(onClick = { onAction(ContactsAction.ToggleCreationMode) }),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF7AF5C9),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = { onAction(ContactsAction.CreateConversation) },
                    enabled = state.isCreateConversationEnabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = state.createConversationActionLabel)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.contacts, key = { it.conversationId }) { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { onAction(ContactsAction.OpenConversation(contact.conversationId)) }
                )
            }
        }

        Button(
            onClick = { onAction(ContactsAction.NavigateBackToInbox) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = state.backActionLabel)
        }
    }
}

@Composable
private fun ContactCard(
    contact: ContactEntryUiState,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x182D4258),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = contact.supportingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9FB7CC)
                )
                Text(
                    text = contact.lastSeenPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFDCEBFA)
                )
            }

            Text(
                text = "Open",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF7AF5C9),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsScreenPreview() {
    ArguslensTheme {
        ContactsScreen(
            state = ContactsUiState(
                title = "Contacts",
                subtitle = "Manage friends, then open or create chats from the local-first IM shell.",
                statusMessage = "Friend added successfully.",
                isStatusError = false,
                draftConversationName = "",
                draftFriendAccountId = "",
                addFriendLabel = "Add remote friend",
                addFriendPlaceholder = "Type a friend account ID",
                addFriendActionLabel = "Add friend",
                isAddFriendEnabled = false,
                draftLabel = "New local conversation",
                draftPlaceholder = "Type a contact or chat title",
                creationModeLabel = "Direct mode",
                toggleCreationModeActionLabel = "Switch to group",
                createConversationActionLabel = "Create chat",
                isCreateConversationEnabled = false,
                contacts = listOf(
                    ContactEntryUiState(
                        conversationId = "conv-1",
                        accountId = "zhangsan",
                        displayName = "Zhang San",
                        supportingLabel = "Default remote contact",
                        lastSeenPreview = "Let me know when the stage-1 IM shell is ready.",
                    ),
                    ContactEntryUiState(
                        conversationId = "conv-2",
                        accountId = "lisi",
                        displayName = "Li Si",
                        supportingLabel = "Remote friend",
                        lastSeenPreview = "No local messages yet.",
                    )
                ),
                backActionLabel = "Back to inbox",
            ),
            onAction = {}
        )
    }
}
