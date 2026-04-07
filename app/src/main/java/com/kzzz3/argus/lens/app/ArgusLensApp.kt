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
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionStatus
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.call.createInitialCallSessionState
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsScreen
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.contacts.reduceContactsState
import com.kzzz3.argus.lens.feature.home.HomeHudScreen
import com.kzzz3.argus.lens.feature.home.HomeHudUiState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxSampleThreads
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.inbox.inboxConversationThreadListSaver
import com.kzzz3.argus.lens.feature.inbox.reduceChatState
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    var contactsStateModel by rememberSaveable(stateSaver = ContactsState.Saver) {
        mutableStateOf(ContactsState())
    }
    var callSessionState by rememberSaveable(stateSaver = CallSessionState.Saver) {
        mutableStateOf(CallSessionState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    val authRepository = remember { createAuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    var callSessionJob by remember { mutableStateOf<Job?>(null) }
    val initialThreads = remember {
        createInboxSampleThreads(currentUserDisplayName = "Argus Tester")
    }
    var conversationThreads by rememberSaveable(stateSaver = inboxConversationThreadListSaver()) {
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
    val contactsState = remember(contactsStateModel, conversationThreads) {
        createContactsUiState(
            state = contactsStateModel,
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
                draftAttachments = conversation.draftAttachments,
                isVoiceRecording = conversation.isVoiceRecording,
            )
        }
    }
    val chatUiState = remember(chatState) {
        chatState?.let(::createChatUiState)
    }
    val callSessionUiState = remember(callSessionState) {
        createCallSessionUiState(callSessionState)
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
                                    callSessionJob?.cancel()
                                    callSessionState = CallSessionState()
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
                                    callSessionJob?.cancel()
                                    callSessionState = CallSessionState()
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

                    InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

                    InboxAction.SignOutToHud -> {
                        appSessionState = AppSessionState()
                        authFormState = AuthFormState()
                        registerFormState = RegisterFormState()
                        contactsStateModel = ContactsState()
                        callSessionJob?.cancel()
                        callSessionState = CallSessionState()
                        conversationThreads = initialThreads
                        selectedConversationId = ""
                        currentRoute = AppRoute.Home
                    }
                }
            }
        )

        AppRoute.Contacts -> ContactsScreen(
            state = contactsState,
            onAction = { action ->
                val result = reduceContactsState(
                    currentState = contactsStateModel,
                    action = action,
                )
                contactsStateModel = result.state

                when (val effect = result.effect) {
                    is ContactsEffect.OpenConversation -> {
                        conversationThreads = markConversationAsRead(
                            threads = conversationThreads,
                            conversationId = effect.conversationId,
                        )
                        selectedConversationId = effect.conversationId
                        currentRoute = AppRoute.Chat
                    }

                    is ContactsEffect.CreateConversation -> {
                        val nextThreads = ensureConversationThread(
                            threads = conversationThreads,
                            displayName = effect.displayName,
                        )
                        conversationThreads = nextThreads
                        selectedConversationId = resolveConversationId(
                            threads = nextThreads,
                            displayName = effect.displayName,
                        )
                        currentRoute = AppRoute.Chat
                    }

                    ContactsEffect.NavigateBackToInbox -> currentRoute = AppRoute.Inbox
                    null -> Unit
                }
            }
        )

        AppRoute.CallSession -> CallSessionScreen(
            state = callSessionUiState,
            onAction = { action ->
                callSessionState = reduceCallSessionState(callSessionState, action)
                if (action == CallSessionAction.EndCall) {
                    callSessionJob?.cancel()
                    coroutineScope.launch {
                        delay(300)
                        currentRoute = AppRoute.Chat
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

                            InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

                            InboxAction.SignOutToHud -> {
                                appSessionState = AppSessionState()
                                authFormState = AuthFormState()
                                registerFormState = RegisterFormState()
                                contactsStateModel = ContactsState()
                                callSessionJob?.cancel()
                                callSessionState = CallSessionState()
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
                            is ChatEffect.StartCall -> {
                                callSessionState = createInitialCallSessionState(
                                    conversationId = result.effect.conversationId,
                                    contactDisplayName = result.effect.contactDisplayName,
                                    mode = if (result.effect.mode == ChatCallMode.Video) {
                                        CallSessionMode.Video
                                    } else {
                                        CallSessionMode.Audio
                                    },
                                )
                                callSessionJob?.cancel()
                                callSessionJob = coroutineScope.launch {
                                    delay(800)
                                    if (callSessionState.status == CallSessionStatus.Connecting) {
                                        callSessionState = callSessionState.copy(status = CallSessionStatus.Active)
                                    }
                                    while (callSessionState.status == CallSessionStatus.Active && currentRoute == AppRoute.CallSession) {
                                        delay(1000)
                                        callSessionState = callSessionState.copy(
                                            durationLabel = incrementCallDurationLabel(callSessionState.durationLabel)
                                        )
                                    }
                                }
                                currentRoute = AppRoute.CallSession
                            }
                            is ChatEffect.DispatchOutgoingMessages -> {
                                coroutineScope.launch {
                                    delay(350)
                                    conversationThreads = resolveOutgoingMessageStatuses(
                                        threads = conversationThreads,
                                        conversationId = result.effect.conversationId,
                                        messageIds = result.effect.messageIds,
                                    )
                                }
                            }
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
                draftAttachments = updatedState.draftAttachments,
                isVoiceRecording = updatedState.isVoiceRecording,
            )
        } else {
            thread
        }
    }
}

private fun resolveOutgoingMessageStatuses(
    threads: List<InboxConversationThread>,
    conversationId: String,
    messageIds: List<String>,
): List<InboxConversationThread> {
    return threads.map { thread ->
        if (thread.id == conversationId) {
            thread.copy(
                messages = thread.messages.map { message ->
                    if (message.id in messageIds) {
                        message.copy(
                            deliveryStatus = if (shouldFailOutgoingMessage(message)) {
                                ChatMessageDeliveryStatus.Failed
                            } else {
                                ChatMessageDeliveryStatus.Sent
                            }
                        )
                    } else {
                        message
                    }
                }
            )
        } else {
            thread
        }
    }
}

private fun shouldFailOutgoingMessage(
    message: com.kzzz3.argus.lens.feature.inbox.ChatMessageItem,
): Boolean {
    return message.body.startsWith("[Video]") || message.body.contains("#fail", ignoreCase = true)
}

private fun ensureConversationThread(
    threads: List<InboxConversationThread>,
    displayName: String,
): List<InboxConversationThread> {
    val normalizedName = displayName.trim()
    if (normalizedName.isEmpty()) return threads

    val existingThread = threads.firstOrNull { it.title.equals(normalizedName, ignoreCase = true) }
    if (existingThread != null) {
        return threads
    }

    val nextThread = InboxConversationThread(
        id = createConversationId(normalizedName, threads.size + 1),
        title = normalizedName,
        subtitle = "New local conversation",
        unreadCount = 0,
        messages = emptyList(),
    )
    return listOf(nextThread) + threads
}

private fun resolveConversationId(
    threads: List<InboxConversationThread>,
    displayName: String,
): String {
    val normalizedName = displayName.trim()
    return threads.firstOrNull { it.title.equals(normalizedName, ignoreCase = true) }?.id.orEmpty()
}

private fun createConversationId(
    displayName: String,
    ordinal: Int,
): String {
    val slug = displayName
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifEmpty { "local-contact" }
    return "conv-$slug-$ordinal"
}

private fun incrementCallDurationLabel(
    currentLabel: String,
): String {
    val parts = currentLabel.split(":")
    if (parts.size != 2) return "00:01"

    val minutes = parts[0].toIntOrNull() ?: 0
    val seconds = parts[1].toIntOrNull() ?: 0
    val totalSeconds = minutes * 60 + seconds + 1
    val nextMinutes = totalSeconds / 60
    val nextSeconds = totalSeconds % 60
    return "%02d:%02d".format(nextMinutes, nextSeconds)
}
