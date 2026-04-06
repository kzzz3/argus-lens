package com.kzzz3.argus.lens.feature.inbox

fun createInboxSampleThreads(
    currentUserDisplayName: String,
): List<InboxConversationThread> {
    val resolvedCurrentUserDisplayName = currentUserDisplayName.ifBlank { "Argus Tester" }

    return listOf(
        InboxConversationThread(
            id = "conv-zhang-san",
            title = "Zhang San",
            subtitle = "1:1 direct chat",
            unreadCount = 2,
            messages = listOf(
                ChatMessageItem(
                    id = "conv-zhang-san-1",
                    senderDisplayName = "Zhang San",
                    body = "Let me know when the stage-1 IM shell is ready.",
                    timestampLabel = "09:24",
                    isFromCurrentUser = false,
                ),
                ChatMessageItem(
                    id = "conv-zhang-san-2",
                    senderDisplayName = resolvedCurrentUserDisplayName,
                    body = "Auth is done. I am turning the inbox placeholder into a real local chat shell now.",
                    timestampLabel = "09:28",
                    isFromCurrentUser = true,
                ),
            ),
        ),
        InboxConversationThread(
            id = "conv-project-group",
            title = "Project Group",
            subtitle = "3 members",
            unreadCount = 0,
            messages = listOf(
                ChatMessageItem(
                    id = "conv-project-group-1",
                    senderDisplayName = "Project Group",
                    body = "We can wire real message sync after the local timeline is stable.",
                    timestampLabel = "Yesterday",
                    isFromCurrentUser = false,
                ),
            ),
        ),
        InboxConversationThread(
            id = "conv-li-si",
            title = "Li Si",
            subtitle = "Feature review",
            unreadCount = 1,
            messages = listOf(
                ChatMessageItem(
                    id = "conv-li-si-1",
                    senderDisplayName = "Li Si",
                    body = "The next useful Android module is a real chat timeline with local send.",
                    timestampLabel = "Mon",
                    isFromCurrentUser = false,
                ),
            ),
        ),
    )
}
