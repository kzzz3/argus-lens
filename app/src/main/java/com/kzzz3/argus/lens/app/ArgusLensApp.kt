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
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxSampleThreads
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.inbox.reduceChatState
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
            primaryHint = "Current module: local inbox + chat shell"
        )
    }
    var currentRoute by rememberSaveable(stateSaver = AppRoute.Saver) { mutableStateOf(AppRoute.Home) }
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
    val initialThreads = remember {
        createInboxSampleThreads(currentUserDisplayName = "Argus Tester")
    }
    var conversationThreads by remember {
        mutableStateOf(initialThreads)
    }

    val authState = remember(authFormState) {
        createAuthEntryUiState(
            formState = authFormState,
        )
    }
    val registerState = remember(registerFormState) {
        createRegisterUiState(registerFormState)
    }
    val sessionDisplayName = remember(appSessionState.displayName) {
        appSessionState.displayName.ifBlank { "Argus Tester" }
    }
    val inboxState = remember(appSessionState, conversationThreads) {
        createInboxUiState(
            sessionState = appSessionState,
            threads = conversationThreads,
        )
    }
    val selectedConversation = remember(selectedConversationId, conversationThreads) {
        conversationThreads.firstOrNull { it.id == selectedConversationId }
    }
    val chatState = remember(selectedConversation, sessionDisplayName) {
        selectedConversation?.let { conversation ->
            ChatState(
                conversationId = conversation.id,
                conversationTitle = conversation.title,
                conversationSubtitle = conversation.subtitle,
                currentUserDisplayName = sessionDisplayName,
                messages = conversation.messages,
                draftMessage = conversation.draftMessage,
            )
        }
    }
    val chatUiState = remember(chatState) {
        chatState?.let(::createChatUiState)
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
                                    conversationThreads = createInboxSampleThreads(
                                        currentUserDisplayName = authResult.session.displayName,
                                    )
                                    selectedConversationId = ""
                                    currentRoute = AppRoute.Inbox
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
                                    conversationThreads = createInboxSampleThreads(
                                        currentUserDisplayName = authResult.session.displayName,
                                    )
                                    selectedConversationId = ""
                                    authFormState = AuthFormState(account = authResult.session.accountId)
                                    currentRoute = AppRoute.Inbox
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

        AppRoute.Inbox -> InboxScreen(
            state = inboxState,
            onAction = { action ->
                when (action) {
                    is InboxAction.OpenConversation -> {
                        conversationThreads = markConversationAsRead(
                            threads = conversationThreads,
                            conversationId = action.conversationId,
                        )
                        selectedConversationId = action.conversationId
                        currentRoute = AppRoute.Chat
                    }

                    InboxAction.SignOutToHud -> {
                        appSessionState = AppSessionState()
                        authFormState = AuthFormState()
                        registerFormState = RegisterFormState()
                        conversationThreads = initialThreads
                        selectedConversationId = ""
                        currentRoute = AppRoute.Home
                    }
                }
            }
        )

        AppRoute.Chat -> {
            val resolvedChatUiState = chatUiState
            val resolvedChatState = chatState

            if (resolvedChatUiState == null || resolvedChatState == null) {
                InboxScreen(
                    state = inboxState,
                    onAction = { action ->
                        when (action) {
                            is InboxAction.OpenConversation -> {
                                conversationThreads = markConversationAsRead(
                                    threads = conversationThreads,
                                    conversationId = action.conversationId,
                                )
                                selectedConversationId = action.conversationId
                            }

                            InboxAction.SignOutToHud -> {
                                appSessionState = AppSessionState()
                                authFormState = AuthFormState()
                                registerFormState = RegisterFormState()
                                conversationThreads = initialThreads
                                selectedConversationId = ""
                                currentRoute = AppRoute.Home
                            }
                        }
                    }
                )
            } else {
                ChatScreen(
                    state = resolvedChatUiState,
                    onAction = { action ->
                        val result = reduceChatState(
                            currentState = resolvedChatState,
                            action = action,
                        )
                        conversationThreads = updateConversationThread(
                            threads = conversationThreads,
                            updatedState = result.state,
                        )

                        when (result.effect) {
                            ChatEffect.NavigateBackToInbox -> currentRoute = AppRoute.Inbox
                            null -> Unit
                        }
                    }
                )
            }
        }
    }
}

private fun markConversationAsRead(
    threads: List<InboxConversationThread>,
    conversationId: String,
): List<InboxConversationThread> {
    return threads.map { thread ->
        if (thread.id == conversationId) {
            thread.copy(unreadCount = 0)
        } else {
            thread
        }
    }
}

private fun updateConversationThread(
    threads: List<InboxConversationThread>,
    updatedState: ChatState,
): List<InboxConversationThread> {
    return threads.map { thread ->
        if (thread.id == updatedState.conversationId) {
            thread.copy(
                messages = updatedState.messages,
                draftMessage = updatedState.draftMessage,
            )
        } else {
            thread
        }
    }
}
