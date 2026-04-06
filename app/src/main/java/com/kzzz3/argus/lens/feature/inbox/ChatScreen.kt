package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme

@Composable
fun ChatScreen(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1620),
                        Color(0xFF182635),
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0x1629FFB2)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = state.conversationTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.conversationSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD8EBFB)
                )
            }
        }

        if (state.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.emptyStateLabel,
                    color = Color(0xFFAAC9E3),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message = message)
                }
            }
        }

        OutlinedTextField(
            value = state.draftMessage,
            onValueChange = { onAction(ChatAction.UpdateDraftMessage(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = "Message draft") },
            placeholder = { Text(text = "Type a local stage-1 message") },
            minLines = 3,
            maxLines = 5,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onAction(ChatAction.NavigateBackToInbox) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = state.backActionLabel)
            }
            Button(
                onClick = { onAction(ChatAction.SendMessage) },
                enabled = state.isSendEnabled,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = state.sendActionLabel)
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageItem,
) {
    val bubbleColor = if (message.isFromCurrentUser) Color(0xFF1E785D) else Color(0x1F9AD0FF)
    val contentColor = if (message.isFromCurrentUser) Color.White else Color(0xFFEAF6FF)
    val horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${message.senderDisplayName} · ${message.timestampLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF9FB7CC)
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = bubbleColor,
        ) {
            Text(
                text = message.body,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    ArguslensTheme {
        ChatScreen(
            state = ChatUiState(
                conversationTitle = "Zhang San",
                conversationSubtitle = "1:1 direct chat",
                messages = listOf(
                    ChatMessageItem(
                        id = "1",
                        senderDisplayName = "Zhang San",
                        body = "Let me know when the stage-1 IM shell is ready.",
                        timestampLabel = "09:24",
                        isFromCurrentUser = false,
                    ),
                    ChatMessageItem(
                        id = "2",
                        senderDisplayName = "Argus Tester",
                        body = "I am building the timeline now.",
                        timestampLabel = "09:28",
                        isFromCurrentUser = true,
                    )
                ),
                draftMessage = "",
                isSendEnabled = false,
                sendActionLabel = "Send",
                backActionLabel = "Back to inbox",
                emptyStateLabel = "No messages yet",
            ),
            onAction = {}
        )
    }
}
