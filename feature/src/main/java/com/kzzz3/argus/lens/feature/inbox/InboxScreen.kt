package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImAvatarBlue
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImDivider
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import com.kzzz3.argus.lens.ui.shell.AppTopHeader

@Composable
fun InboxScreen(
    state: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val filteredConversations = remember(searchQuery, state.conversations) {
        val normalizedQuery = searchQuery.trim()
        if (normalizedQuery.isEmpty()) {
            state.conversations
        } else {
            state.conversations.filter { item ->
                item.title.contains(normalizedQuery, ignoreCase = true) ||
                    item.preview.contains(normalizedQuery, ignoreCase = true) ||
                    item.subtitle.contains(normalizedQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppTopHeader(
            title = state.title,
        ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (state.shellStatusLabel == "Online") ImGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = state.shellStatusLabel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ImTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ImSurfaceElevated,
                            modifier = Modifier.width(52.dp).height(44.dp),
                        ) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Quick actions",
                                    tint = ImTextPrimary,
                                )
                            }
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("Add friend") },
                                onClick = {
                                    menuExpanded = false
                                    onAction(InboxAction.OpenContacts)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Open wallet") },
                                onClick = {
                                    menuExpanded = false
                                    onAction(InboxAction.OpenWallet)
                                },
                            )
                        }
                    }
                }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 20.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search") },
                    placeholder = { Text("Search chats or contacts") },
                    singleLine = true,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent conversations",
                        style = MaterialTheme.typography.titleMedium,
                        color = ImTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${filteredConversations.size} chats",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImTextMuted,
                    )
                }
            }

            items(filteredConversations, key = { it.id }) { item ->
                ConversationCard(
                    item = item,
                    onClick = { onAction(InboxAction.OpenConversation(item.id)) },
                )
            }
        }
    }
}

@Composable
private fun ConversationCard(
    item: InboxConversationItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = ImSurfaceElevated.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = CircleShape,
                    color = if (item.unreadCount > 0) ImGreen else ImAvatarBlue,
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.title.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = ImTextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (item.unreadCount > 0) {
                    Badge(containerColor = ImBlue) {
                        Text(text = item.unreadCount.toString(), color = ImTextPrimary)
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = ImTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = ImTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.timestampLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = ImTextMuted,
                    )
                }

                Text(
                    text = item.preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.latestMessageStatusLabel?.let { label ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = when (item.latestMessageStatusColorToken) {
                                InboxStatusColorToken.Neutral -> ImDivider
                                InboxStatusColorToken.Success -> ImGreen.copy(alpha = 0.18f)
                                InboxStatusColorToken.Warning -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                            },
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ImTextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    } ?: Box {}

                    Text(
                        text = "Open chat",
                        style = MaterialTheme.typography.labelMedium,
                        color = ImGreen,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InboxScreenPreview() {
    ArguslensTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            InboxScreen(
                state = InboxUiState(
                    title = "Chats",
                    subtitle = "A tighter IM inbox with dense cards and clearer conversation hierarchy.",
                    shellStatusLabel = "Online",
                    sessionLabel = "Signed in as demo-account",
                    sessionSummary = "Account ID: argus-demo. Realtime: LIVE.",
                    conversations = listOf(
                        InboxConversationItem(
                            id = "conv-1",
                            title = "Zhang San",
                            subtitle = "1:1 direct chat",
                            preview = "Let me know when the wallet flow is ready for review.",
                            timestampLabel = "09:24",
                            unreadCount = 2,
                            latestMessageStatusLabel = "Sent",
                            latestMessageStatusColorToken = InboxStatusColorToken.Success,
                        ),
                        InboxConversationItem(
                            id = "conv-2",
                            title = "Li Si",
                            subtitle = "1:1 direct chat",
                            preview = "We can run one more polish pass on the shell after the wallet page settles.",
                            timestampLabel = "Yesterday",
                            unreadCount = 0,
                            latestMessageStatusLabel = "Sending",
                            latestMessageStatusColorToken = InboxStatusColorToken.Neutral,
                        ),
                    ),
                ),
                onAction = {},
            )
        }
    }
}
