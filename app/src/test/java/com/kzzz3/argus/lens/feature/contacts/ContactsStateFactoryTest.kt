package com.kzzz3.argus.lens.feature.contacts

import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import org.junit.Assert.assertEquals
import org.junit.Test

class ContactsStateFactoryTest {

    @Test
    fun createContactsUiState_prefersStableDirectConversationIdOverDisplayNameMatching() {
        val uiState = createContactsUiState(
            state = ContactsState(),
            friends = listOf(
                FriendEntry(
                    accountId = "lisi",
                    displayName = "Li Si",
                    note = "Remote friend",
                )
            ),
            threads = listOf(
                InboxConversationThread(
                    id = "conv-direct-lisi-tester",
                    title = "Product Review Group",
                    subtitle = "Direct friend conversation",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-1",
                            senderDisplayName = "Li Si",
                            body = "Synced by stable direct id",
                            timestampLabel = "10:00",
                            isFromCurrentUser = false,
                        )
                    ),
                )
            ),
            currentAccountId = "tester",
        )

        val contact = uiState.contacts.single()
        assertEquals("conv-direct-lisi-tester", contact.conversationId)
        assertEquals("Synced by stable direct id", contact.lastSeenPreview)
    }

    @Test
    fun createContactsUiState_doesNotMatchOnlyByDisplayName() {
        val uiState = createContactsUiState(
            state = ContactsState(),
            friends = listOf(
                FriendEntry(
                    accountId = "lisi",
                    displayName = "Li Si",
                    note = "Remote friend",
                )
            ),
            threads = listOf(
                InboxConversationThread(
                    id = "conv-group-review",
                    title = "Li Si",
                    subtitle = "Group chat",
                    unreadCount = 0,
                    messages = listOf(
                        ChatMessageItem(
                            id = "m-group",
                            senderDisplayName = "Li Si",
                            body = "Wrong thread by title only",
                            timestampLabel = "10:05",
                            isFromCurrentUser = false,
                        )
                    ),
                )
            ),
            currentAccountId = "tester",
        )

        val contact = uiState.contacts.single()
        assertEquals("conv-direct-lisi-tester", contact.conversationId)
        assertEquals("No local messages yet", contact.lastSeenPreview)
    }

    @Test
    fun createContactsUiState_fallsBackToDeterministicDirectConversationIdWhenThreadMissing() {
        val uiState = createContactsUiState(
            state = ContactsState(),
            friends = listOf(
                FriendEntry(
                    accountId = "zhangsan",
                    displayName = "Zhang San",
                    note = "Remote friend",
                )
            ),
            threads = emptyList(),
            currentAccountId = "tester",
        )

        val contact = uiState.contacts.single()
        assertEquals("conv-direct-tester-zhangsan", contact.conversationId)
        assertEquals("No local messages yet", contact.lastSeenPreview)
    }
}
