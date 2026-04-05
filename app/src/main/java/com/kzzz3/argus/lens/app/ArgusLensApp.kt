package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createPlaceholderSession
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryEffect
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.home.HomeHudScreen
import com.kzzz3.argus.lens.feature.home.HomeHudUiState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatPlaceholderScreen
import com.kzzz3.argus.lens.feature.inbox.ChatPlaceholderUiState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationItem
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxPlaceholderScreen
import com.kzzz3.argus.lens.feature.inbox.InboxPlaceholderUiState

@Composable
fun ArgusLensApp() {
    val homeState = remember {
        HomeHudUiState(
            deviceLabel = "Android Glasses Simulator",
            syncStatus = "Stage 1 Baseline Ready",
            activeMode = "IM Foundation",
            primaryHint = "Next module: login + session bootstrap"
        )
    }
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var authFormState by rememberSaveable(stateSaver = AuthFormState.Saver) {
        mutableStateOf(AuthFormState())
    }
    var appSessionState by rememberSaveable(stateSaver = AppSessionState.Saver) {
        mutableStateOf(AppSessionState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }

    val fakeConversations = remember {
        listOf(
            InboxConversationItem(
                id = "conv-zhang-san",
                title = "Zhang San",
                preview = "Let me know when the stage-1 IM shell is ready.",
                timestampLabel = "09:24",
                unreadCount = 2,
            ),
            InboxConversationItem(
                id = "conv-project-group",
                title = "Project Group",
                preview = "We can wire real message sync after the inbox UI stabilizes.",
                timestampLabel = "Yesterday",
                unreadCount = 0,
            ),
            InboxConversationItem(
                id = "conv-li-si",
                title = "Li Si",
                preview = "The auth flow is ready for the next module.",
                timestampLabel = "Mon",
                unreadCount = 1,
            )
        )
    }

    val authState = remember(authFormState) {
        createAuthEntryUiState(
            formState = authFormState,
        )
    }
    val inboxState = remember(appSessionState) {
        InboxPlaceholderUiState(
            title = "Login success",
            subtitle = "You have entered the stage-1 inbox placeholder.",
            sessionLabel = if (appSessionState.isAuthenticated) {
                "Signed in as ${appSessionState.displayName}"
            } else {
                "No active session"
            },
            sessionSummary = if (appSessionState.isAuthenticated) {
                "Account ID: ${appSessionState.accountId}. Session placeholder is active and ready for future backend integration."
            } else {
                "Session placeholder is empty."
            },
            conversations = fakeConversations,
            primaryActionLabel = "Sign out to HUD"
        )
    }
    val selectedConversation = remember(selectedConversationId, fakeConversations) {
        fakeConversations.firstOrNull { it.id == selectedConversationId }
    }
    val chatState = remember(selectedConversation) {
        ChatPlaceholderUiState(
            conversationTitle = selectedConversation?.title ?: "Conversation",
            conversationSubtitle = "Stage-1 chat placeholder",
            messagePreview = selectedConversation?.preview
                ?: "Next step: render a real message timeline here.",
            primaryActionLabel = "Back to inbox"
        )
    }

    when (currentRoute) {
        AppRoute.Home -> HomeHudScreen(
            state = homeState,
            onPrimaryActionClick = { currentRoute = AppRoute.AuthEntry }
        )

        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onAction = { action ->
                val result = reduceAuthFormState(
                    currentState = authFormState,
                    action = action,
                )
                authFormState = result.formState

                when (result.effect) {
                    AuthEntryEffect.NavigateBack -> currentRoute = AppRoute.Home
                    AuthEntryEffect.NavigateToInboxPlaceholder -> {
                        appSessionState = createPlaceholderSession(authFormState.account)
                        currentRoute = AppRoute.InboxPlaceholder
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.InboxPlaceholder -> InboxPlaceholderScreen(
            state = inboxState,
            onAction = { action ->
                when (action) {
                    is InboxAction.OpenConversation -> {
                        selectedConversationId = action.conversationId
                        currentRoute = AppRoute.ChatPlaceholder
                    }

                    InboxAction.SignOutToHud -> {
                        appSessionState = AppSessionState()
                        authFormState = AuthFormState()
                        selectedConversationId = ""
                        currentRoute = AppRoute.Home
                    }
                }
            }
        )

        AppRoute.ChatPlaceholder -> ChatPlaceholderScreen(
            state = chatState,
            onAction = { action ->
                when (action) {
                    ChatAction.NavigateBackToInbox -> currentRoute = AppRoute.InboxPlaceholder
                }
            }
        )
    }
}
