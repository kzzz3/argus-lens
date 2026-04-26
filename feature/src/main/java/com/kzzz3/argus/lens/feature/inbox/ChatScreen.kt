package com.kzzz3.argus.lens.feature.inbox

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kzzz3.argus.lens.ui.theme.ArguslensTheme
import com.kzzz3.argus.lens.ui.theme.ImBlue
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextMuted
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary

@Composable
fun ChatScreen(
    state: ChatUiState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val messageListState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        val lastMessageIndex = state.messages.lastIndex
        if (lastMessageIndex >= 0) {
            messageListState.animateScrollToItem(lastMessageIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ImSurfaceElevated.copy(alpha = 0.95f)
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
                    color = ImTextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.conversationSubtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ImTextSecondary
                )
                state.statusMessage?.let { message ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (state.isStatusError) Color(0x33FF6F61) else Color(0x297AF5C9),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImTextPrimary,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ComposerActionChip(
                        label = state.audioCallActionLabel,
                        accentColor = ImGreen,
                        onClick = { onAction(ChatAction.StartAudioCall) },
                        modifier = Modifier.weight(1f)
                    )
                    ComposerActionChip(
                        label = state.videoCallActionLabel,
                        accentColor = ImBlue,
                        onClick = { onAction(ChatAction.StartVideoCall) },
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    color = ImTextMuted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(
                state = messageListState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onAction = onAction,
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ImSurfaceElevated.copy(alpha = 0.92f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.composerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = ImTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = state.composerHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ComposerActionChip(
                        label = state.imageActionLabel,
                        accentColor = ImBlue,
                        onClick = { onAction(ChatAction.AddImageAttachment) },
                        modifier = Modifier.weight(1f)
                    )
                    ComposerActionChip(
                        label = state.videoActionLabel,
                        accentColor = Color(0xFF8F7BFF),
                        onClick = { onAction(ChatAction.AddVideoAttachment) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.draftAttachments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.draftAttachments.forEach { attachment ->
                            DraftAttachmentCard(
                                attachment = attachment,
                                onRemove = {
                                    onAction(ChatAction.RemoveDraftAttachment(attachment.id))
                                }
                            )
                        }
                    }
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
private fun ComposerActionChip(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = accentColor.copy(alpha = 0.14f),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DraftAttachmentCard(
    attachment: ChatDraftAttachment,
    onRemove: () -> Unit,
) {
    val accentColor = when (attachment.kind) {
        ChatDraftAttachmentKind.Image -> Color(0xFF83C9FF)
        ChatDraftAttachmentKind.Video -> Color(0xFFB59BFF)
        ChatDraftAttachmentKind.Voice -> Color(0xFF7AF5C9)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = attachment.title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = attachment.summary,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFAEC7DC)
            )
        }

        Text(
            text = "Remove",
            modifier = Modifier.clickable(onClick = onRemove),
            style = MaterialTheme.typography.labelLarge,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageItem,
    onAction: (ChatAction) -> Unit,
) {
    val bubbleColor = if (message.isFromCurrentUser) Color(0xFF1E785D) else Color(0x1F9AD0FF)
    val contentColor = if (message.isFromCurrentUser) Color.White else Color(0xFFEAF6FF)
    val horizontalAlignment = if (message.isFromCurrentUser) Alignment.End else Alignment.Start
    val richMediaMessage = message.attachment?.toRichMediaMessage() ?: parseRichMediaMessage(message.body)
    val downloadAction = richMediaMessage?.attachmentId?.let { attachmentId ->
        {
            onAction(
                ChatAction.DownloadAttachment(
                    attachmentId = attachmentId,
                    fileName = richMediaMessage.title,
                )
            )
        }
    }

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
            if (richMediaMessage == null) {
                Text(
                    text = message.body,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                )
            } else {
                RichMediaBubble(
                    media = richMediaMessage,
                    contentColor = contentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    onDownloadAttachment = downloadAction,
                )
            }
        }

        OutgoingStatusRow(
            message = message,
            onRetry = { onAction(ChatAction.RetryFailedMessage(message.id)) },
            onRecall = { onAction(ChatAction.RecallMessage(message.id)) },
        )
    }
}

@Composable
private fun OutgoingStatusRow(
    message: ChatMessageItem,
    onRetry: () -> Unit,
    onRecall: () -> Unit,
) {
    if (!message.isFromCurrentUser) return

    val (label, color) = when (message.deliveryStatus) {
        ChatMessageDeliveryStatus.Sending -> "Sending" to Color(0xFFAEC7DC)
        ChatMessageDeliveryStatus.Sent -> "Sent" to Color(0xFF7AF5C9)
        ChatMessageDeliveryStatus.Delivered -> "Delivered" to Color(0xFF7AF5C9)
        ChatMessageDeliveryStatus.Read -> "Read" to Color(0xFF7AF5C9)
        ChatMessageDeliveryStatus.Failed -> "Failed · Tap to retry" to Color(0xFFFF9A8B)
        ChatMessageDeliveryStatus.Recalled -> "Recalled" to Color(0xFFAAC9E3)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = if (message.deliveryStatus == ChatMessageDeliveryStatus.Failed) {
                Modifier.clickable(onClick = onRetry)
            } else {
                Modifier
            },
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )

        if (
            message.deliveryStatus == ChatMessageDeliveryStatus.Sent ||
            message.deliveryStatus == ChatMessageDeliveryStatus.Delivered ||
            message.deliveryStatus == ChatMessageDeliveryStatus.Read
        ) {
            Text(
                text = "Recall",
                modifier = Modifier.clickable(onClick = onRecall),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFFFD27A),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RichMediaBubble(
    media: RichMediaMessage,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onDownloadAttachment: (() -> Unit)? = null,
) {
    val accentColor = when (media.type) {
        "Image" -> Color(0xFF83C9FF)
        "Video" -> Color(0xFFB59BFF)
        else -> contentColor
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = media.type.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = media.title,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = media.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor.copy(alpha = 0.88f)
        )
        if (onDownloadAttachment != null) {
            Text(
                text = "Download / Save As",
                modifier = Modifier
                    .clickable(onClick = onDownloadAttachment)
                    .padding(vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = accentColor.copy(alpha = 0.18f)
        ) {
            Text(
                text = "Stage-1 local media placeholder",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}

private data class RichMediaMessage(
    val type: String,
    val title: String,
    val summary: String,
    val attachmentId: String? = null,
)

private fun ChatMessageAttachment.toRichMediaMessage(): RichMediaMessage {
    return RichMediaMessage(
        type = attachmentType.lowercase().replaceFirstChar { it.titlecase() },
        title = fileName,
        summary = if (contentLength > 0L) {
            "Structured attachment envelope ? ${contentLength} bytes"
        } else {
            "Structured attachment envelope"
        },
        attachmentId = attachmentId,
    )
}

private fun parseRichMediaMessage(
    body: String,
): RichMediaMessage? {
    val fileMatch = Regex("""^\[File] (Image|Video) · (.+?) · (.+?) · attachmentId=(\S+) · objectKey=(\S+) · uploadUrl=(.+)$""").matchEntire(body)
    if (fileMatch != null) {
        return RichMediaMessage(
            type = fileMatch.groupValues[1],
            title = fileMatch.groupValues[2],
            summary = fileMatch.groupValues[3],
            attachmentId = fileMatch.groupValues[4],
        )
    }

    val legacyMatch = Regex("""^\[(Image|Video|Voice)]\s+(.+?)\s+·\s+(.+)$""").matchEntire(body)
        ?: return null

    return RichMediaMessage(
        type = legacyMatch.groupValues[1],
        title = legacyMatch.groupValues[2],
        summary = legacyMatch.groupValues[3],
    )
}


@Preview(showBackground = true)
@Composable
private fun ChatScreenPreview() {
    ArguslensTheme {
        ChatScreen(
            state = ChatUiState(
                conversationTitle = "Zhang San",
                conversationSubtitle = "1:1 direct chat",
                statusMessage = null,
                isStatusError = false,
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
                draftAttachments = listOf(
                    ChatDraftAttachment(
                        id = "draft-image-1",
                        kind = ChatDraftAttachmentKind.Image,
                        title = "Image draft 1",
                        summary = "Local gallery placeholder ready to send",
                    )
                ),
                composerTitle = "Stage-1 media composer",
                composerHint = "Build a local draft with text or file attachments (image, video) before wiring the real media stack.",
                imageActionLabel = "Add image",
                videoActionLabel = "Add video",
                voiceActionLabel = "",
                voiceRecordingLabel = "",
                cancelVoiceActionLabel = "",
                isCancelVoiceVisible = false,
                audioCallActionLabel = "Audio call",
                videoCallActionLabel = "Video call",
                isSendEnabled = false,
                sendActionLabel = "Send draft",
                backActionLabel = "Back to inbox",
                emptyStateLabel = "No messages yet",
            ),
            onAction = {}
        )
    }
}
