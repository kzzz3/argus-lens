package com.kzzz3.argus.lens.feature.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatReducerTest {

    @Test
    fun sendMessage_withDraft_appendsLocalMessageAndClearsDraft() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = listOf(
                ChatMessageItem(
                    id = "m1",
                    senderDisplayName = "Zhang San",
                    body = "Hello",
                    timestampLabel = "09:24",
                    isFromCurrentUser = false,
                )
            ),
            draftMessage = "Ship the next Android module today.",
        )

        val result = reduceChatState(state, ChatAction.SendMessage)

        assertEquals("", result.state.draftMessage)
        assertEquals(2, result.state.messages.size)
        assertEquals("Ship the next Android module today.", result.state.messages.last().body)
        assertTrue(result.state.messages.last().isFromCurrentUser)
        assertEquals(ChatMessageDeliveryStatus.Sending, result.state.messages.last().deliveryStatus)
        assertEquals(
            ChatEffect.DispatchOutgoingMessages(
                conversationId = "conv-1",
                messageIds = listOf(result.state.messages.last().id),
            ),
            result.effect
        )
    }

    @Test
    fun addImageAttachment_appendsDraftAttachment() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
        )

        val result = reduceChatState(state, ChatAction.AddImageAttachment)

        assertEquals(1, result.state.draftAttachments.size)
        assertEquals(ChatDraftAttachmentKind.Image, result.state.draftAttachments.first().kind)
    }

    @Test
    fun toggleVoiceDraft_twice_createsVoiceAttachmentAndStopsRecording() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
        )

        val armed = reduceChatState(state, ChatAction.ToggleVoiceDraft)
        val ticking = reduceChatState(armed.state, ChatAction.TickVoiceRecording)
        val finished = reduceChatState(ticking.state, ChatAction.ToggleVoiceDraft)

        assertTrue(armed.state.isVoiceRecording)
        assertEquals(1, ticking.state.voiceRecordingSeconds)
        assertFalse(finished.state.isVoiceRecording)
        assertEquals(0, finished.state.voiceRecordingSeconds)
        assertEquals(1, finished.state.draftAttachments.size)
        assertEquals(ChatDraftAttachmentKind.Voice, finished.state.draftAttachments.first().kind)
    }

    @Test
    fun cancelVoiceRecording_stopsRecordingAndClearsDuration() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
            isVoiceRecording = true,
            voiceRecordingSeconds = 4,
        )

        val result = reduceChatState(state, ChatAction.CancelVoiceRecording)

        assertFalse(result.state.isVoiceRecording)
        assertEquals(0, result.state.voiceRecordingSeconds)
    }

    @Test
    fun sendMessage_withAttachments_sendsMediaEntriesAndClearsDrafts() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
            draftAttachments = listOf(
                ChatDraftAttachment(
                    id = "draft-1",
                    kind = ChatDraftAttachmentKind.Video,
                    title = "Video draft 1",
                    summary = "Short local video placeholder ready to send",
                )
            ),
            isVoiceRecording = true,
        )

        val result = reduceChatState(state, ChatAction.SendMessage)

        assertEquals(1, result.state.messages.size)
        assertTrue(result.state.messages.first().body.startsWith("[Video]"))
        assertEquals(ChatMessageDeliveryStatus.Sending, result.state.messages.first().deliveryStatus)
        assertTrue(result.state.draftAttachments.isEmpty())
        assertFalse(result.state.isVoiceRecording)
    }

    @Test
    fun retryFailedMessage_marksMessageSendingAndEmitsDispatchEffect() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = listOf(
                ChatMessageItem(
                    id = "m1",
                    senderDisplayName = "Argus Tester",
                    body = "[Video] Video draft 1 · Short local video placeholder ready to send",
                    timestampLabel = "Now",
                    isFromCurrentUser = true,
                    deliveryStatus = ChatMessageDeliveryStatus.Failed,
                )
            ),
        )

        val result = reduceChatState(state, ChatAction.RetryFailedMessage("m1"))

        assertEquals(ChatMessageDeliveryStatus.Sending, result.state.messages.first().deliveryStatus)
        assertEquals(
            ChatEffect.DispatchOutgoingMessages(
                conversationId = "conv-1",
                messageIds = listOf("m1"),
            ),
            result.effect
        )
    }

    @Test
    fun sendMessage_withBlankDraft_keepsStateUnchanged() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
            draftMessage = "   ",
        )

        val result = reduceChatState(state, ChatAction.SendMessage)

        assertEquals(state, result.state)
        assertEquals(null, result.effect)
    }

    @Test
    fun navigateBack_requestsInboxNavigation() {
        val state = ChatState(
            conversationId = "conv-1",
            conversationTitle = "Zhang San",
            conversationSubtitle = "1:1 direct chat",
            currentUserDisplayName = "Argus Tester",
            messages = emptyList(),
        )

        val result = reduceChatState(state, ChatAction.NavigateBackToInbox)

        assertEquals(ChatEffect.NavigateBackToInbox, result.effect)
    }
}
