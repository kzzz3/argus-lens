package com.kzzz3.argus.lens.feature.inbox.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val InboxRoutePattern = "Inbox"
const val ChatRoutePattern = "Chat"
const val ChatThreadRoutePattern = "$ChatRoutePattern/{conversationId}"

@Serializable
@SerialName(InboxRoutePattern)
data object InboxRoute

@Serializable
@SerialName(ChatRoutePattern)
data class ChatThreadRoute(val conversationId: String)

data class InboxRoutes(
    val inboxShellDestination: ShellDestination,
    val chatShellDestination: ShellDestination,
    val missingChatShellDestination: ShellDestination,
    val onTabSelected: (ShellDestination) -> Unit,
    val inboxState: InboxUiState,
    val chatState: ChatState?,
    val chatUiState: ChatUiState?,
    val onInboxAction: (InboxAction) -> Unit,
    val onChatAction: (ChatAction) -> Unit,
)

fun NavGraphBuilder.inboxNavigation(
    routes: InboxRoutes,
) {
    composable<InboxRoute> {
        AuthenticatedFeatureRouteShell(
            currentDestination = routes.inboxShellDestination,
            onTabSelected = routes.onTabSelected,
        ) { contentModifier ->
            InboxScreen(
                state = routes.inboxState,
                onAction = routes.onInboxAction,
                modifier = contentModifier,
            )
        }
    }

    composable<ChatThreadRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<ChatThreadRoute>()
        if (routes.chatUiState == null || routes.chatState == null || routes.chatState.conversationId != route.conversationId) {
            AuthenticatedFeatureRouteShell(
                currentDestination = routes.missingChatShellDestination,
                onTabSelected = routes.onTabSelected,
            ) { contentModifier ->
                InboxScreen(
                    state = routes.inboxState,
                    onAction = routes.onInboxAction,
                    modifier = contentModifier,
                )
            }
        } else {
            AuthenticatedFeatureRouteShell(
                currentDestination = routes.chatShellDestination,
                onTabSelected = routes.onTabSelected,
            ) { contentModifier ->
                ChatScreen(
                    state = routes.chatUiState,
                    onAction = routes.onChatAction,
                    modifier = contentModifier,
                )
            }
        }
    }
}
