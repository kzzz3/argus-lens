package com.kzzz3.argus.lens.feature.inbox.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatUiState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.InboxUiState
import com.kzzz3.argus.lens.feature.navigation.AuthenticatedFeatureRouteShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

const val InboxRoute = "Inbox"
const val ChatRoute = "Chat"

fun NavGraphBuilder.inboxNavigation(
    inboxShellDestination: ShellDestination,
    chatShellDestination: ShellDestination,
    missingChatShellDestination: ShellDestination,
    onTabSelected: (ShellDestination) -> Unit,
    inboxState: InboxUiState,
    chatState: ChatState?,
    chatUiState: ChatUiState?,
    onInboxAction: (InboxAction) -> Unit,
    onChatAction: (ChatAction) -> Unit,
) {
    composable(InboxRoute) {
        AuthenticatedFeatureRouteShell(
            currentDestination = inboxShellDestination,
            onTabSelected = onTabSelected,
        ) { contentModifier ->
            InboxScreen(
                state = inboxState,
                onAction = onInboxAction,
                modifier = contentModifier,
            )
        }
    }

    composable(ChatRoute) {
        if (chatUiState == null || chatState == null) {
            AuthenticatedFeatureRouteShell(
                currentDestination = missingChatShellDestination,
                onTabSelected = onTabSelected,
            ) { contentModifier ->
                InboxScreen(
                    state = inboxState,
                    onAction = onInboxAction,
                    modifier = contentModifier,
                )
            }
        } else {
            AuthenticatedFeatureRouteShell(
                currentDestination = chatShellDestination,
                onTabSelected = onTabSelected,
            ) { contentModifier ->
                ChatScreen(
                    state = chatUiState,
                    onAction = onChatAction,
                    modifier = contentModifier,
                )
            }
        }
    }
}
