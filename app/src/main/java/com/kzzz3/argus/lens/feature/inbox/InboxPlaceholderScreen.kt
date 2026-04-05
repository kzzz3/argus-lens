package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Badge
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
fun InboxPlaceholderScreen(
    state: InboxPlaceholderUiState,
    onConversationClick: (InboxConversationItem) -> Unit,
    onPrimaryActionClick: () -> Unit,
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
                Text(
                    text = "This is the first post-login placeholder. Later it will become the IM inbox / conversation list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAC9E3)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Conversation Preview",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            state.conversations.forEach { item ->
                ConversationCard(
                    item = item,
                    onClick = { onConversationClick(item) }
                )
            }
        }

        Button(
            onClick = onPrimaryActionClick,
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
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
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
private fun InboxPlaceholderScreenPreview() {
    ArguslensTheme {
        InboxPlaceholderScreen(
            state = InboxPlaceholderUiState(
                title = "Login success",
                subtitle = "You have entered the stage-1 inbox placeholder.",
                sessionLabel = "Signed in as demo-account",
                sessionSummary = "Session placeholder is active and ready for future backend integration.",
                conversations = listOf(
                    InboxConversationItem(
                        id = "conv-1",
                        title = "Zhang San",
                        preview = "Let me know when the stage-1 IM shell is ready.",
                        timestampLabel = "09:24",
                        unreadCount = 2,
                    ),
                    InboxConversationItem(
                        id = "conv-2",
                        title = "Project Group",
                        preview = "We can wire real message sync after the inbox UI stabilizes.",
                        timestampLabel = "Yesterday",
                        unreadCount = 0,
                    )
                ),
                primaryActionLabel = "Back to HUD"
            ),
            onConversationClick = {},
            onPrimaryActionClick = {}
        )
    }
}
