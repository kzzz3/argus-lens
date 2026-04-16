package com.kzzz3.argus.lens.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImAvatarBlue
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import com.kzzz3.argus.lens.ui.shell.AppTopHeader

@Composable
fun ContactsScreen(
    state: ContactsUiState,
    onAction: (ContactsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppTopHeader(title = state.title) {
                Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        text = "${state.contacts.size}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = ImTextMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        ) {
            state.statusMessage?.let { message ->
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (state.isStatusError) MaterialTheme.colorScheme.errorContainer else ImGreen.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImTextPrimary,
                        )
                    }
                }
            }

            item {
                NewFriendsEntryCard(state = state, onAction = onAction)
            }

            item {
                AddFriendCard(state = state, onAction = onAction)
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "People in your network",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${state.contacts.size} friends",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImTextMuted,
                    )
                }
            }

            items(state.contacts, key = { it.conversationId }) { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { onAction(ContactsAction.OpenConversation(contact.conversationId)) },
                )
            }
        }
    }
}

@Composable
private fun NewFriendsEntryCard(
    state: ContactsUiState,
    onAction: (ContactsAction) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ImSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAction(ContactsAction.OpenNewFriends) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = state.newFriendsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = ImTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = state.newFriendsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ImTextMuted,
                )
            }
            Surface(shape = RoundedCornerShape(999.dp), color = ImGreen.copy(alpha = 0.14f)) {
                Text(
                    text = "Open",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = ImGreen,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AddFriendCard(
    state: ContactsUiState,
    onAction: (ContactsAction) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ImSurfaceElevated,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Add friend",
                    style = MaterialTheme.typography.titleMedium,
                    color = ImTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Request first",
                    style = MaterialTheme.typography.labelMedium,
                    color = ImTextMuted,
                )
            }
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = state.addFriendActionLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: ContactEntryUiState,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ImSurfaceElevated,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = ImAvatarBlue) {
                Box(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = contact.displayName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = ImTextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = ImTextPrimary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = contact.supportingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = ImTextMuted,
                )
                Text(
                    text = contact.lastSeenPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ContactsScreenPreview() {
    ArguslensTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            ContactsScreen(
                state = ContactsUiState(
                    title = "Contacts",
                    subtitle = "Manage your friend graph and open chats from one tighter IM surface.",
                    statusMessage = "Friend added successfully.",
                    isStatusError = false,
                    newFriendsLabel = "New Friends",
                    newFriendsSubtitle = "Review incoming requests and track requests you have sent.",
                    draftFriendAccountId = "",
                    addFriendLabel = "Add remote friend",
                    addFriendPlaceholder = "Type a friend account ID",
                    addFriendActionLabel = "Add friend",
                    isAddFriendEnabled = false,
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
                        ),
                    ),
                    backActionLabel = "Back to inbox",
                ),
                onAction = {},
            )
        }
    }
}
