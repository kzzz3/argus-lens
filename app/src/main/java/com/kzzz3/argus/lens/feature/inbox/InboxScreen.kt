package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
fun InboxScreen(
    state: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF08131E),
                        Color(0xFF10253A),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x1F9AD0FF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Text(
                    text = state.sessionLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF9AD0FF),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.sessionSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFEAF6FF)
                )
            }
        }

        Text(
            text = "Conversations",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.conversations, key = { it.id }) { item ->
                ConversationCard(
                    item = item,
                    onClick = { onAction(InboxAction.OpenConversation(item.id)) }
                )
            }
        }

        Button(
            onClick = { onAction(InboxAction.SignOutToHud) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = state.primaryActionLabel)
        }
    }
}

@Composable
private fun ConversationCard(
    item: InboxConversationItem,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x182D4258),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9FB7CC)
                    )
                }
                Text(
                    text = item.timestampLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAC9E3)
                )
            }

            Text(
                text = item.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDCEBFA)
            )

            if (item.unreadCount > 0) {
                Badge(containerColor = Color(0xFF7AF5C9)) {
                    Text(
                        text = item.unreadCount.toString(),
                        color = Color(0xFF062118)
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
        InboxScreen(
            state = InboxUiState(
                title = "Stage-1 Inbox",
                subtitle = "Real local conversation list",
                sessionLabel = "Signed in as demo-account",
                sessionSummary = "Auth is real; messages are local for this step.",
                conversations = listOf(
                    InboxConversationItem(
                        id = "conv-1",
                        title = "Zhang San",
                        subtitle = "1:1 direct chat",
                        preview = "Let me know when the stage-1 IM shell is ready.",
                        timestampLabel = "09:24",
                        unreadCount = 2,
                    ),
                    InboxConversationItem(
                        id = "conv-2",
                        title = "Project Group",
                        subtitle = "3 members",
                        preview = "We can wire real sync later.",
                        timestampLabel = "Yesterday",
                        unreadCount = 0,
                    )
                ),
                primaryActionLabel = "Back to HUD"
            ),
            onAction = {}
        )
    }
}
