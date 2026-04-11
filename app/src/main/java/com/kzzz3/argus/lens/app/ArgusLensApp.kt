package com.kzzz3.argus.lens.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.auth.createAuthRepository
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.createConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.friend.createFriendRepository
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.data.session.createLocalSessionStore
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
import com.kzzz3.argus.lens.feature.contacts.ConversationCreationMode
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
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
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
    var currentRoute by rememberSaveable { mutableStateOf(AppRoute.Home) }
    var authFormState by rememberSaveable {
        mutableStateOf(AuthFormState())
    }
    var registerFormState by rememberSaveable {
        mutableStateOf(RegisterFormState())
    }
    var appSessionState by rememberSaveable {
        mutableStateOf(AppSessionState())
    }
    var contactsStateModel by rememberSaveable {
        mutableStateOf(ContactsState())
    }
    var callSessionState by rememberSaveable {
        mutableStateOf(CallSessionState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    val authRepository = remember { createAuthRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val sessionRepository: SessionRepository = remember(context) { createLocalSessionStore(context) }
    val conversationRepository: ConversationRepository = remember(context, sessionRepository) {
        createConversationRepository(context = context, sessionRepository = sessionRepository)
    }
    val friendRepository: FriendRepository = remember(sessionRepository) {
        createFriendRepository(sessionRepository)
    }
    val appShellCoordinator = remember(conversationRepository, sessionRepository) {
        AppShellCoordinator(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
        )
    }
    var callSessionJob by remember { mutableStateOf<Job?>(null) }
    var hydratedConversationAccountId by remember { mutableStateOf<String?>(null) }
    var hydratedSession by remember { mutableStateOf(false) }
    var friends by remember { mutableStateOf<List<FriendEntry>>(emptyList()) }
    val previewThreadsState = remember {
        conversationRepository.createPreviewState(currentUserDisplayName = "Argus Tester")
    }
    var conversationThreadsState by remember {
        mutableStateOf(previewThreadsState)
    }
    val conversationThreads = conversationThreadsState.threads

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
    val contactsState = remember(contactsStateModel, friends, conversationThreads) {
        createContactsUiState(
            state = contactsStateModel,
            friends = friends,
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
                voiceRecordingSeconds = conversation.voiceRecordingSeconds,
            )
        }
    }
    val chatUiState = remember(chatState) {
        chatState?.let(::createChatUiState)
    }
    val callSessionUiState = remember(callSessionState) {
        createCallSessionUiState(callSessionState)
    }

    LaunchedEffect(Unit) {
        val hydratedState = appShellCoordinator.hydrateAppState(previewThreadsState)
        appSessionState = hydratedState.session
        conversationThreadsState = hydratedState.conversationThreadsState
        hydratedConversationAccountId = hydratedState.hydratedConversationAccountId
        if (hydratedState.session.isAuthenticated) {
            currentRoute = AppRoute.Inbox
        }
        hydratedSession = true
    }

    LaunchedEffect(hydratedSession, appSessionState) {
        appShellCoordinator.persistSession(hydratedSession, appSessionState)
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        if (hydratedSession && appSessionState.isAuthenticated) {
            hydratedConversationAccountId = null
            val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
            conversationThreadsState = signedInState.conversationThreadsState
            hydratedConversationAccountId = signedInState.hydratedConversationAccountId
        } else {
            hydratedConversationAccountId = null
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        appShellCoordinator.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        if (currentRoute == AppRoute.Contacts && appSessionState.isAuthenticated) {
            when (val friendResult = friendRepository.listFriends()) {
                is FriendRepositoryResult.Success -> friends = friendResult.friends
                is FriendRepositoryResult.Failure -> Unit
            }
        }
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
                                    hydratedConversationAccountId = null
                                    callSessionJob?.cancel()
                                    val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
                                    callSessionState = signedInState.callSessionState
                                    conversationThreadsState = signedInState.conversationThreadsState
                                    hydratedConversationAccountId = signedInState.hydratedConversationAccountId
                                    selectedConversationId = signedInState.selectedConversationId
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
                                    hydratedConversationAccountId = null
                                    callSessionJob?.cancel()
                                    val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
                                    callSessionState = signedInState.callSessionState
                                    conversationThreadsState = signedInState.conversationThreadsState
                                    hydratedConversationAccountId = signedInState.hydratedConversationAccountId
                                    selectedConversationId = signedInState.selectedConversationId
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
                        conversationThreadsState = conversationRepository.markConversationAsRead(
                            state = conversationThreadsState,
                            conversationId = action.conversationId,
                        )
                        selectedConversationId = action.conversationId
                        currentRoute = AppRoute.Chat
                        coroutineScope.launch {
                            conversationThreadsState = conversationRepository.markConversationReadRemote(
                                state = conversationThreadsState,
                                conversationId = action.conversationId,
                            )
                            conversationThreadsState = conversationRepository.refreshConversationMessages(
                                state = conversationThreadsState,
                                conversationId = action.conversationId,
                            )
                            val visibleRemoteMessageIds = conversationThreadsState.threads
                                .firstOrNull { it.id == action.conversationId }
                                ?.messages
                                ?.filter { !it.isFromCurrentUser && it.deliveryStatus != ChatMessageDeliveryStatus.Read }
                                ?.map { it.id }
                                .orEmpty()
                            visibleRemoteMessageIds.forEach { messageId ->
                                conversationThreadsState = conversationRepository.acknowledgeMessageRead(
                                    state = conversationThreadsState,
                                    conversationId = action.conversationId,
                                    messageId = messageId,
                                )
                            }
                        }
                    }

                    InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

                    InboxAction.SignOutToHud -> {
                        val signedOutState = appShellCoordinator.createSignedOutState(previewThreadsState)
                        appSessionState = AppSessionState()
                        authFormState = signedOutState.authFormState
                        registerFormState = signedOutState.registerFormState
                        contactsStateModel = signedOutState.contactsState
                        callSessionJob?.cancel()
                        callSessionState = signedOutState.callSessionState
                        hydratedConversationAccountId = null
                        conversationThreadsState = signedOutState.conversationThreadsState
                        selectedConversationId = signedOutState.selectedConversationId
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
                        val existingThread = conversationThreads.firstOrNull { it.id == effect.conversationId }
                        val resolvedConversationId = if (existingThread != null) {
                            conversationThreadsState = conversationRepository.markConversationAsRead(
                                state = conversationThreadsState,
                                conversationId = effect.conversationId,
                            )
                            effect.conversationId
                        } else {
                            val matchingFriend = friends.firstOrNull { it.accountId == effect.conversationId }
                            val displayName = matchingFriend?.displayName ?: effect.conversationId
                            conversationThreadsState = conversationRepository.createConversation(
                                state = conversationThreadsState,
                                displayName = displayName,
                                mode = ConversationCreationMode.Direct,
                            )
                            conversationRepository.resolveConversationId(
                                state = conversationThreadsState,
                                displayName = displayName,
                            )
                        }
                        selectedConversationId = resolvedConversationId
                        currentRoute = AppRoute.Chat
                        coroutineScope.launch {
                            conversationThreadsState = conversationRepository.markConversationReadRemote(
                                state = conversationThreadsState,
                                conversationId = resolvedConversationId,
                            )
                            conversationThreadsState = conversationRepository.refreshConversationMessages(
                                state = conversationThreadsState,
                                conversationId = resolvedConversationId,
                            )
                            val visibleRemoteMessageIds = conversationThreadsState.threads
                                .firstOrNull { it.id == resolvedConversationId }
                                ?.messages
                                ?.filter { !it.isFromCurrentUser && it.deliveryStatus != ChatMessageDeliveryStatus.Read }
                                ?.map { it.id }
                                .orEmpty()
                            visibleRemoteMessageIds.forEach { messageId ->
                                conversationThreadsState = conversationRepository.acknowledgeMessageRead(
                                    state = conversationThreadsState,
                                    conversationId = resolvedConversationId,
                                    messageId = messageId,
                                )
                            }
                        }
                    }

                    is ContactsEffect.CreateConversation -> {
                        conversationThreadsState = conversationRepository.createConversation(
                            state = conversationThreadsState,
                            displayName = effect.displayName,
                            mode = effect.mode,
                        )
                        selectedConversationId = conversationRepository.resolveConversationId(
                            state = conversationThreadsState,
                            displayName = effect.displayName,
                        )
                        currentRoute = AppRoute.Chat
                    }

                    is ContactsEffect.AddFriend -> {
                        coroutineScope.launch {
                            when (val friendResult = friendRepository.addFriend(effect.friendAccountId)) {
                                is FriendRepositoryResult.Success -> {
                                    val refreshed = friendRepository.listFriends()
                                    if (refreshed is FriendRepositoryResult.Success) {
                                        friends = refreshed.friends
                                    } else {
                                        friends = friends + friendResult.friends
                                    }
                                }

                                is FriendRepositoryResult.Failure -> Unit
                            }
                        }
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
                                conversationThreadsState = conversationRepository.markConversationAsRead(
                                    state = conversationThreadsState,
                                    conversationId = action.conversationId,
                                )
                                selectedConversationId = action.conversationId
                                currentRoute = AppRoute.Chat
                                coroutineScope.launch {
                                    conversationThreadsState = conversationRepository.markConversationReadRemote(
                                        state = conversationThreadsState,
                                        conversationId = action.conversationId,
                                    )
                                    conversationThreadsState = conversationRepository.refreshConversationMessages(
                                        state = conversationThreadsState,
                                        conversationId = action.conversationId,
                                    )
                                    val visibleRemoteMessageIds = conversationThreadsState.threads
                                        .firstOrNull { it.id == action.conversationId }
                                        ?.messages
                                        ?.filter { !it.isFromCurrentUser && it.deliveryStatus != ChatMessageDeliveryStatus.Read }
                                        ?.map { it.id }
                                        .orEmpty()
                                    visibleRemoteMessageIds.forEach { messageId ->
                                        conversationThreadsState = conversationRepository.acknowledgeMessageRead(
                                            state = conversationThreadsState,
                                            conversationId = action.conversationId,
                                            messageId = messageId,
                                        )
                                    }
                                }
                            }

                            InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

                            InboxAction.SignOutToHud -> {
                                val signedOutState = appShellCoordinator.createSignedOutState(previewThreadsState)
                                appSessionState = AppSessionState()
                                authFormState = signedOutState.authFormState
                                registerFormState = signedOutState.registerFormState
                                contactsStateModel = signedOutState.contactsState
                                callSessionJob?.cancel()
                                callSessionState = signedOutState.callSessionState
                                hydratedConversationAccountId = null
                                conversationThreadsState = signedOutState.conversationThreadsState
                                selectedConversationId = signedOutState.selectedConversationId
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
                        conversationThreadsState = conversationRepository.updateConversationFromChatState(
                            state = conversationThreadsState,
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
                                    val latestMessage = result.state.messages
                                        .filter { it.id in result.effect.messageIds }
                                        .lastOrNull()
                                    val latestBody = result.state.messages
                                        .filter { it.id in result.effect.messageIds }
                                        .lastOrNull()
                                        ?.body
                                    if (result.effect.messageIds.size == 1 && latestBody != null && latestMessage != null) {
                                        conversationThreadsState = conversationRepository.sendMessage(
                                            state = conversationThreadsState,
                                            conversationId = result.effect.conversationId,
                                            localMessageId = latestMessage.id,
                                            body = latestBody,
                                        )
                                    } else {
                                        delay(350)
                                        conversationThreadsState = conversationRepository.resolveOutgoingMessages(
                                            state = conversationThreadsState,
                                            conversationId = result.effect.conversationId,
                                            messageIds = result.effect.messageIds,
                                        )
                                        delay(700)
                                        conversationThreadsState = conversationRepository.resolveDeliveredMessages(
                                            state = conversationThreadsState,
                                            conversationId = result.effect.conversationId,
                                            messageIds = result.effect.messageIds,
                                        )
                                    }
                                }
                            }
                            null -> Unit
                        }

                        if (action is ChatAction.RecallMessage) {
                            coroutineScope.launch {
                                conversationThreadsState = conversationRepository.recallMessage(
                                    state = conversationThreadsState,
                                    conversationId = resolvedChatState.conversationId,
                                    messageId = action.messageId,
                                )
                            }
                        }
                    }
                )
            }
        }
    }
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
