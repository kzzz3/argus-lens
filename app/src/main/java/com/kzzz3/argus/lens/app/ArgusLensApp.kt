package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.auth.createAuthRepository
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
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import kotlinx.coroutines.launch

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
    var registerFormState by rememberSaveable(stateSaver = RegisterFormState.Saver) {
        mutableStateOf(RegisterFormState())
    }
    var appSessionState by rememberSaveable(stateSaver = AppSessionState.Saver) {
        mutableStateOf(AppSessionState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    val authRepository = remember { createAuthRepository() }
    val coroutineScope = rememberCoroutineScope()

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
    val registerState = remember(registerFormState) {
        createRegisterUiState(registerFormState)
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
                "Account ID: ${appSessionState.accountId}. Access token is cached locally and the stage-1 auth API is now wired."
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
                    AuthEntryEffect.NavigateToRegister -> currentRoute = AppRoute.RegisterEntry
                    is AuthEntryEffect.SubmitPasswordLogin -> {
                        coroutineScope.launch {
                            when (
                                val authResult = authRepository.login(
                                    account = result.effect.account,
                                    password = result.effect.password,
                                )
                            ) {
                                is AuthRepositoryResult.Success -> {
                                    authFormState = authFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.session.message,
                                    )
                                    appSessionState = createAuthenticatedSession(
                                        accountId = authResult.session.accountId,
                                        displayName = authResult.session.displayName,
                                        accessToken = authResult.session.accessToken,
                                    )
                                    selectedConversationId = ""
                                    currentRoute = AppRoute.InboxPlaceholder
                                }

                                is AuthRepositoryResult.Failure -> {
                                    authFormState = authFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.message,
                                    )
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.RegisterEntry -> RegisterScreen(
            state = registerState,
            onAction = { action ->
                val result = reduceRegisterFormState(
                    currentState = registerFormState,
                    action = action,
                )
                registerFormState = result.formState

                when (result.effect) {
                    RegisterEffect.NavigateBackToLogin -> currentRoute = AppRoute.AuthEntry
                    is RegisterEffect.SubmitRegistration -> {
                        coroutineScope.launch {
                            when (
                                val authResult = authRepository.register(
                                    displayName = result.effect.displayName,
                                    account = result.effect.account,
                                    password = result.effect.password,
                                )
                            ) {
                                is AuthRepositoryResult.Success -> {
                                    registerFormState = registerFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.session.message,
                                    )
                                    appSessionState = createAuthenticatedSession(
                                        accountId = authResult.session.accountId,
                                        displayName = authResult.session.displayName,
                                        accessToken = authResult.session.accessToken,
                                    )
                                    selectedConversationId = ""
                                    authFormState = AuthFormState(account = authResult.session.accountId)
                                    currentRoute = AppRoute.InboxPlaceholder
                                }

                                is AuthRepositoryResult.Failure -> {
                                    registerFormState = registerFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.message,
                                    )
                                }
                            }
                        }
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
                        registerFormState = RegisterFormState()
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
