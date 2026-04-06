package com.kzzz3.argus.lens.feature.inbox

data class ChatReducerResult(
    val state: ChatState,
    val effect: ChatEffect?,
)

fun reduceChatState(
    currentState: ChatState,
    action: ChatAction,
): ChatReducerResult {
    return when (action) {
        ChatAction.NavigateBackToInbox -> ChatReducerResult(
            state = currentState,
            effect = ChatEffect.NavigateBackToInbox,
        )

        ChatAction.AddImageAttachment -> ChatReducerResult(
            state = currentState.copy(
                draftAttachments = currentState.draftAttachments + createDraftAttachment(
                    conversationId = currentState.conversationId,
                    nextIndex = currentState.draftAttachments.size + 1,
                    kind = ChatDraftAttachmentKind.Image,
                )
            ),
            effect = null,
        )

        ChatAction.AddVideoAttachment -> ChatReducerResult(
            state = currentState.copy(
                draftAttachments = currentState.draftAttachments + createDraftAttachment(
                    conversationId = currentState.conversationId,
                    nextIndex = currentState.draftAttachments.size + 1,
                    kind = ChatDraftAttachmentKind.Video,
                )
            ),
            effect = null,
        )

        ChatAction.ToggleVoiceDraft -> {
            if (currentState.isVoiceRecording) {
                ChatReducerResult(
                    state = currentState.copy(
                        isVoiceRecording = false,
                        draftAttachments = currentState.draftAttachments + createDraftAttachment(
                            conversationId = currentState.conversationId,
                            nextIndex = currentState.draftAttachments.size + 1,
                            kind = ChatDraftAttachmentKind.Voice,
                        )
                    ),
                    effect = null,
                )
            } else {
                ChatReducerResult(
                    state = currentState.copy(isVoiceRecording = true),
                    effect = null,
                )
            }
        }

        is ChatAction.RemoveDraftAttachment -> ChatReducerResult(
            state = currentState.copy(
                draftAttachments = currentState.draftAttachments.filterNot { it.id == action.attachmentId }
            ),
            effect = null,
        )

        is ChatAction.RetryFailedMessage -> {
            val retriedMessages = currentState.messages.map { message ->
                if (message.id == action.messageId) {
                    message.copy(deliveryStatus = ChatMessageDeliveryStatus.Sending)
                } else {
                    message
                }
            }

            ChatReducerResult(
                state = currentState.copy(messages = retriedMessages),
                effect = ChatEffect.DispatchOutgoingMessages(
                    conversationId = currentState.conversationId,
                    messageIds = listOf(action.messageId),
                ),
            )
        }

        ChatAction.SendMessage -> {
            val trimmedDraft = currentState.draftMessage.trim()
            val sentMessages = buildList {
                if (trimmedDraft.isNotEmpty()) {
                    add(
                        ChatMessageItem(
                            id = "${currentState.conversationId}-local-text-${currentState.messages.size + size + 1}",
                            senderDisplayName = currentState.currentUserDisplayName,
                            body = trimmedDraft,
                            timestampLabel = "Now",
                            isFromCurrentUser = true,
                            deliveryStatus = ChatMessageDeliveryStatus.Sending,
                        )
                    )
                }

                currentState.draftAttachments.forEach { attachment ->
                    add(
                        ChatMessageItem(
                            id = "${currentState.conversationId}-local-media-${currentState.messages.size + size + 1}",
                            senderDisplayName = currentState.currentUserDisplayName,
                            body = draftAttachmentMessageBody(attachment),
                            timestampLabel = "Now",
                            isFromCurrentUser = true,
                            deliveryStatus = ChatMessageDeliveryStatus.Sending,
                        )
                    )
                }
            }

            if (sentMessages.isEmpty()) {
                ChatReducerResult(
                    state = currentState,
                    effect = null,
                )
            } else {
                ChatReducerResult(
                    state = currentState.copy(
                        messages = currentState.messages + sentMessages,
                        draftMessage = "",
                        draftAttachments = emptyList(),
                        isVoiceRecording = false,
                    ),
                    effect = ChatEffect.DispatchOutgoingMessages(
                        conversationId = currentState.conversationId,
                        messageIds = sentMessages.map { it.id },
                    ),
                )
            }
        }

        is ChatAction.UpdateDraftMessage -> ChatReducerResult(
            state = currentState.copy(draftMessage = action.value),
            effect = null,
        )
    }
}

private fun createDraftAttachment(
    conversationId: String,
    nextIndex: Int,
    kind: ChatDraftAttachmentKind,
): ChatDraftAttachment {
    val title = when (kind) {
        ChatDraftAttachmentKind.Image -> "Image draft $nextIndex"
        ChatDraftAttachmentKind.Video -> "Video draft $nextIndex"
        ChatDraftAttachmentKind.Voice -> "Voice clip $nextIndex"
    }
    val summary = when (kind) {
        ChatDraftAttachmentKind.Image -> "Local gallery placeholder ready to send"
        ChatDraftAttachmentKind.Video -> "Short local video placeholder ready to send"
        ChatDraftAttachmentKind.Voice -> "Local recorded voice placeholder ready to send"
    }

    return ChatDraftAttachment(
        id = "$conversationId-draft-${kind.name.lowercase()}-$nextIndex",
        kind = kind,
        title = title,
        summary = summary,
    )
}

private fun draftAttachmentMessageBody(
    attachment: ChatDraftAttachment,
): String {
    return when (attachment.kind) {
        ChatDraftAttachmentKind.Image -> "[Image] ${attachment.title} · ${attachment.summary}"
        ChatDraftAttachmentKind.Video -> "[Video] ${attachment.title} · ${attachment.summary}"
        ChatDraftAttachmentKind.Voice -> "[Voice] ${attachment.title} · ${attachment.summary}"
    }
}
