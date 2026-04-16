package com.kzzz3.argus.lens.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.data.friend.FriendRequestEntry
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import com.kzzz3.argus.lens.ui.shell.AppTopHeader

data class NewFriendsUiState(
    val title: String,
    val subtitle: String,
    val isLoading: Boolean,
    val statusMessage: String?,
    val isStatusError: Boolean,
    val incoming: List<FriendRequestEntry>,
    val outgoing: List<FriendRequestEntry>,
)

sealed interface NewFriendsAction {
    data object NavigateBack : NewFriendsAction
    data class Accept(val requestId: String) : NewFriendsAction
    data class Reject(val requestId: String) : NewFriendsAction
    data class Ignore(val requestId: String) : NewFriendsAction
}

@Composable
fun NewFriendsScreen(
    state: NewFriendsUiState,
    onAction: (NewFriendsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        AppTopHeader(title = state.title) {
            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "${state.incoming.size + state.outgoing.size}",
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
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isStatusError) MaterialTheme.colorScheme.errorContainer else ImGreen.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImTextPrimary,
                        )
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { onAction(NewFriendsAction.NavigateBack) }) {
                        Text("Back")
                    }
                    Text(
                        text = if (state.incoming.isNotEmpty()) "Respond to requests to unlock direct chats." else "Track requests you have sent.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ImTextMuted,
                    )
                }
            }

            item {
                SectionHeader(title = "Incoming requests", count = state.incoming.size)
            }

            if (state.incoming.isEmpty()) {
                item { EmptyRow("No incoming requests", "When someone adds you, their request will appear here.") }
            } else {
                items(state.incoming, key = { it.requestId }) { request ->
                    IncomingRequestCard(request = request, onAction = onAction)
                }
            }

            item {
                SectionHeader(title = "Outgoing requests", count = state.outgoing.size)
            }

            if (state.outgoing.isEmpty()) {
                item { EmptyRow("No outgoing requests", "Requests you send will stay here until the other user responds.") }
            } else {
                items(state.outgoing, key = { it.requestId }) { request ->
                    OutgoingRequestCard(request = request)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = ImTextPrimary, fontWeight = FontWeight.SemiBold)
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = ImTextMuted)
    }
}

@Composable
private fun IncomingRequestCard(request: FriendRequestEntry, onAction: (NewFriendsAction) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = ImSurfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(request.displayName, style = MaterialTheme.typography.titleMedium, color = ImTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(request.accountId, style = MaterialTheme.typography.bodySmall, color = ImTextMuted)
                }
                RequestStatusPill(status = request.status)
            }
            Text(request.note.ifBlank { "Sent you a friend request." }, style = MaterialTheme.typography.bodyMedium, color = ImTextSecondary)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onAction(NewFriendsAction.Ignore(request.requestId)) }, modifier = Modifier.weight(1f)) { Text("Ignore") }
                OutlinedButton(onClick = { onAction(NewFriendsAction.Reject(request.requestId)) }, modifier = Modifier.weight(1f)) { Text("Reject") }
                Button(onClick = { onAction(NewFriendsAction.Accept(request.requestId)) }, modifier = Modifier.weight(1f)) { Text("Accept") }
            }
        }
    }
}

@Composable
private fun OutgoingRequestCard(request: FriendRequestEntry) {
    Surface(shape = RoundedCornerShape(18.dp), color = ImSurfaceElevated) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(request.displayName, style = MaterialTheme.typography.titleMedium, color = ImTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text(request.accountId, style = MaterialTheme.typography.bodySmall, color = ImTextMuted)
                }
                RequestStatusPill(status = request.status)
            }
            Text(request.note.ifBlank { "Friend request sent." }, style = MaterialTheme.typography.bodyMedium, color = ImTextSecondary)
        }
    }
}

@Composable
private fun RequestStatusPill(status: String) {
    val normalizedStatus = status.uppercase()
    val containerColor = when (normalizedStatus) {
        "ACCEPTED" -> ImGreen.copy(alpha = 0.14f)
        "REJECTED" -> MaterialTheme.colorScheme.errorContainer
        "IGNORED" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (normalizedStatus) {
        "REJECTED" -> MaterialTheme.colorScheme.error
        "IGNORED" -> ImTextMuted
        else -> ImGreen
    }
    Surface(shape = RoundedCornerShape(999.dp), color = containerColor) {
        Text(
            text = normalizedStatus.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyRow(message: String, supporting: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = ImSurfaceElevated) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = ImTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(text = supporting, style = MaterialTheme.typography.bodySmall, color = ImTextMuted)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NewFriendsScreenPreview() {
    ArguslensTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            NewFriendsScreen(
                state = NewFriendsUiState(
                    title = "New Friends",
                    subtitle = "Review incoming requests and track the status of requests you have sent.",
                    isLoading = false,
                    statusMessage = null,
                    isStatusError = false,
                    incoming = listOf(FriendRequestEntry("req-1", "lisi", "Li Si", "INCOMING", "PENDING", "Let's connect.")),
                    outgoing = listOf(FriendRequestEntry("req-2", "zhangsan", "Zhang San", "OUTGOING", "PENDING", "Hi there.")),
                ),
                onAction = {},
            )
        }
    }
}
