package com.kzzz3.argus.lens.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.buildDirectConversationId
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.data.media.FinalizedAttachmentMetadata
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
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
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.inbox.reduceChatState
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.scan.ScanAction
import com.kzzz3.argus.lens.feature.scan.ScanEffect
import com.kzzz3.argus.lens.feature.scan.ScanScreen
import com.kzzz3.argus.lens.feature.scan.ScanState
import com.kzzz3.argus.lens.feature.scan.createScanUiState
import com.kzzz3.argus.lens.feature.scan.reduceScanState
import com.kzzz3.argus.lens.feature.scan.withConfirmFailure
import com.kzzz3.argus.lens.feature.scan.withConfirmedPayment
import com.kzzz3.argus.lens.feature.scan.withResolveFailure
import com.kzzz3.argus.lens.feature.scan.withResolvedPayment
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.text.Charsets

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
    var scanStateModel by rememberSaveable {
        mutableStateOf(ScanState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    var chatStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var chatStatusError by rememberSaveable { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val dependencies = rememberAppDependencies(context)
    val authRepository = dependencies.authRepository
    val sessionRepository = dependencies.sessionRepository
    val conversationRepository: ConversationRepository = dependencies.conversationRepository
    val friendRepository: FriendRepository = dependencies.friendRepository
    val mediaRepository: MediaRepository = dependencies.mediaRepository
    val paymentRepository: PaymentRepository = dependencies.paymentRepository
    val realtimeClient = dependencies.realtimeClient
    val appShellCoordinator = dependencies.appShellCoordinator
    var callSessionJob by remember { mutableStateOf<Job?>(null) }
    var hydratedConversationAccountId by remember { mutableStateOf<String?>(null) }
    var hydratedSession by remember { mutableStateOf(false) }
    var realtimeSubscription by remember { mutableStateOf<ConversationRealtimeSubscription?>(null) }
    var realtimeConnectionState by remember { mutableStateOf(ConversationRealtimeConnectionState.DISABLED) }
    var realtimeLastEventId by rememberSaveable { mutableStateOf("") }
    var realtimeReconnectAttempt by remember { mutableStateOf(0) }
    var realtimeReconnectGeneration by remember { mutableStateOf(0) }
    var realtimeReconnectJob by remember { mutableStateOf<Job?>(null) }
    var activeRealtimeConnectionId by remember { mutableStateOf("") }
    val realtimeEventMutex = remember { Mutex() }
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
    val inboxState = remember(appSessionState, conversationThreads, realtimeConnectionState) {
        createInboxUiState(
            sessionState = appSessionState,
            threads = conversationThreads,
            realtimeStatusLabel = buildRealtimeStatusLabel(realtimeConnectionState),
        )
    }
    val contactsState = remember(contactsStateModel, friends, conversationThreads, appSessionState.accountId) {
        createContactsUiState(
            state = contactsStateModel,
            friends = friends,
            threads = conversationThreads,
            currentAccountId = appSessionState.accountId,
        )
    }
    val selectedConversation = remember(selectedConversationId, conversationThreads) {
        conversationThreads.firstOrNull { it.id == selectedConversationId }
    }
    val latestConversationThreadsState by rememberUpdatedState(conversationThreadsState)
    val latestSelectedConversationId by rememberUpdatedState(selectedConversationId)
    val latestAppSessionState by rememberUpdatedState(appSessionState)
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestRealtimeEnabled by rememberUpdatedState(
        hydratedSession && appSessionState.isAuthenticated && appSessionState.accessToken.isNotBlank()
    )
    val chatState = remember(selectedConversation, sessionDisplayName) {
        selectedConversation?.let { conversation ->
            ChatState(
                conversationId = conversation.id,
                conversationTitle = conversation.title,
                conversationSubtitle = conversation.subtitle,
                memberSummary = if (conversation.subtitle.contains("members")) conversation.subtitle else "",
                draftMemberAccountId = "",
                currentUserDisplayName = sessionDisplayName,
                messages = conversation.messages,
                draftMessage = conversation.draftMessage,
                draftAttachments = conversation.draftAttachments,
                isVoiceRecording = conversation.isVoiceRecording,
                voiceRecordingSeconds = conversation.voiceRecordingSeconds,
            )
        }
    }
    val chatUiState = remember(chatState, chatStatusMessage, chatStatusError) {
        chatState?.let {
            createChatUiState(
                state = it,
                statusMessage = chatStatusMessage,
                isStatusError = chatStatusError,
            )
        }
    }
    val callSessionUiState = remember(callSessionState) {
        createCallSessionUiState(callSessionState)
    }
    val scanUiState = remember(scanStateModel) {
        createScanUiState(scanStateModel)
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

    LaunchedEffect(selectedConversationId) {
        chatStatusMessage = null
        chatStatusError = false
    }

    fun scheduleRealtimeReconnect() {
        if (!latestRealtimeEnabled) return
        if (realtimeReconnectJob?.isActive == true) return
        val nextAttempt = realtimeReconnectAttempt + 1
        realtimeReconnectAttempt = nextAttempt
        realtimeConnectionState = ConversationRealtimeConnectionState.RECOVERING
        realtimeReconnectJob = coroutineScope.launch {
            delay(realtimeReconnectDelayMillis(nextAttempt))
            if (latestRealtimeEnabled) {
                realtimeReconnectGeneration += 1
            }
        }
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

    LaunchedEffect(hydratedSession, appSessionState.isAuthenticated, appSessionState.accessToken, realtimeReconnectGeneration) {
        activeRealtimeConnectionId = ""
        realtimeSubscription?.close()
        realtimeSubscription = null

        val realtimeEnabled = hydratedSession && appSessionState.isAuthenticated && appSessionState.accessToken.isNotBlank()
        if (!realtimeEnabled) {
            realtimeReconnectJob?.cancel()
            realtimeReconnectJob = null
            realtimeReconnectAttempt = 0
            realtimeLastEventId = ""
            realtimeConnectionState = ConversationRealtimeConnectionState.DISABLED
            return@LaunchedEffect
        }

        val connectionId = "realtime-${appSessionState.accountId}-${realtimeReconnectGeneration}"
        activeRealtimeConnectionId = connectionId
        realtimeConnectionState = if (realtimeReconnectAttempt > 0) {
            ConversationRealtimeConnectionState.RECOVERING
        } else {
            ConversationRealtimeConnectionState.CONNECTING
        }
        realtimeSubscription = realtimeClient.connect(
            accessToken = appSessionState.accessToken,
            lastEventId = realtimeLastEventId.ifBlank { null },
            onConnected = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        realtimeConnectionState = ConversationRealtimeConnectionState.LIVE
                        realtimeReconnectAttempt = 0
                        realtimeReconnectJob?.cancel()
                        realtimeReconnectJob = null
                    }
                }
            },
            onClosed = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        scheduleRealtimeReconnect()
                    }
                }
            },
            onEvent = { event ->
                coroutineScope.launch {
                    if (activeRealtimeConnectionId != connectionId) return@launch
                    if (event.eventId.isNotBlank()) {
                        realtimeLastEventId = event.eventId
                    }
                    if (event.eventType == REALTIME_EVENT_STREAM_READY) {
                        realtimeConnectionState = ConversationRealtimeConnectionState.LIVE
                        return@launch
                    }
                    if (event.eventType == REALTIME_EVENT_HEARTBEAT) {
                        return@launch
                    }
                    realtimeEventMutex.withLock {
                        conversationThreadsState = handleConversationRealtimeEvent(
                            event = event,
                            conversationRepository = conversationRepository,
                            session = latestAppSessionState,
                            currentState = conversationThreadsState,
                            selectedConversationId = latestSelectedConversationId,
                            isChatRouteActive = latestCurrentRoute == AppRoute.Chat,
                        )
                    }
                }
            },
            onError = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        scheduleRealtimeReconnect()
                    }
                }
            },
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            activeRealtimeConnectionId = ""
            realtimeReconnectJob?.cancel()
            realtimeReconnectJob = null
            realtimeSubscription?.close()
            realtimeSubscription = null
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
                                    scanStateModel = ScanState()
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
                                    scanStateModel = ScanState()
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
                            conversationThreadsState = synchronizeActiveConversation(
                                state = conversationThreadsState,
                                conversationId = action.conversationId,
                                conversationRepository = conversationRepository,
                            )
                        }
                    }

					InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

					InboxAction.OpenScan -> currentRoute = AppRoute.Scan

					InboxAction.SignOutToHud -> {
						val signedOutState = appShellCoordinator.createSignedOutState(previewThreadsState)
                        appSessionState = AppSessionState()
                        authFormState = signedOutState.authFormState
                        registerFormState = signedOutState.registerFormState
						contactsStateModel = signedOutState.contactsState
						callSessionJob?.cancel()
						callSessionState = signedOutState.callSessionState
						scanStateModel = ScanState()
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
                        coroutineScope.launch {
                            val existingThread = conversationThreads.firstOrNull { it.id == effect.conversationId }
                            val resolvedConversationId = if (existingThread != null) {
                                conversationThreadsState = conversationRepository.markConversationAsRead(
                                    state = conversationThreadsState,
                                    conversationId = effect.conversationId,
                                )
                                effect.conversationId
                            } else {
                                val matchingFriend = friends.firstOrNull { friend ->
                                    friend.accountId == effect.conversationId ||
                                        buildDirectConversationId(appSessionState.accountId, friend.accountId) == effect.conversationId
                                }
                                val preferredConversationId = matchingFriend?.let { friend ->
                                    buildDirectConversationId(appSessionState.accountId, friend.accountId)
                                }
                                val refreshedConversationId = if (appSessionState.isAuthenticated && matchingFriend != null) {
                                    conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                                        accountId = appSessionState.accountId,
                                        currentUserDisplayName = appSessionState.displayName,
                                    )
                                    conversationThreadsState.threads.firstOrNull { thread ->
                                        thread.id == preferredConversationId
                                    }?.id
                                } else {
                                    null
                                }
                                if (refreshedConversationId != null) {
                                    conversationThreadsState = conversationRepository.markConversationAsRead(
                                        state = conversationThreadsState,
                                        conversationId = refreshedConversationId,
                                    )
                                    refreshedConversationId
                                } else {
                                    val displayName = matchingFriend?.displayName ?: effect.conversationId
                                    val deterministicConversationId = preferredConversationId ?: effect.conversationId
                                    conversationThreadsState = ensureDirectConversationPlaceholder(
                                        state = conversationThreadsState,
                                        conversationId = deterministicConversationId,
                                        title = displayName,
                                    )
                                    deterministicConversationId
                                }
                            }
                            selectedConversationId = resolvedConversationId
                            currentRoute = AppRoute.Chat
                            conversationThreadsState = synchronizeActiveConversation(
                                state = conversationThreadsState,
                                conversationId = resolvedConversationId,
                                conversationRepository = conversationRepository,
                            )
                        }
                    }

                    is ContactsEffect.CreateConversation -> {
                        coroutineScope.launch {
                            conversationThreadsState = if (effect.mode == ConversationCreationMode.Group) {
                                conversationRepository.createConversationRemote(
                                    state = conversationThreadsState,
                                    displayName = effect.displayName,
                                    mode = effect.mode,
                                )
                            } else {
                                conversationRepository.createConversation(
                                    state = conversationThreadsState,
                                    displayName = effect.displayName,
                                    mode = effect.mode,
                                )
                            }
                            selectedConversationId = conversationRepository.resolveConversationId(
                                state = conversationThreadsState,
                                displayName = effect.displayName,
                            )
                            contactsStateModel = contactsStateModel.copy(
                                statusMessage = if (effect.mode == ConversationCreationMode.Group) {
                                    "Group created successfully."
                                } else {
                                    "Direct draft created locally."
                                },
                                isStatusError = false,
                            )
                            currentRoute = AppRoute.Chat
                        }
                    }

                    is ContactsEffect.AddFriend -> {
                        coroutineScope.launch {
                            when (val friendResult = friendRepository.addFriend(effect.friendAccountId)) {
                                is FriendRepositoryResult.Success -> {
                                    val refreshed = friendRepository.listFriends()
                                    contactsStateModel = contactsStateModel.copy(
                                        statusMessage = friendResult.message ?: "Friend added.",
                                        isStatusError = false,
                                    )
                                    if (refreshed is FriendRepositoryResult.Success) {
                                        friends = refreshed.friends
                                    } else {
                                        friends = friends + friendResult.friends
                                    }
                                    if (appSessionState.isAuthenticated) {
                                        conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                                            accountId = appSessionState.accountId,
                                            currentUserDisplayName = appSessionState.displayName,
                                        )
                                    }
                                }

                                is FriendRepositoryResult.Failure -> {
                                    contactsStateModel = contactsStateModel.copy(
                                        statusMessage = friendResult.message,
                                        isStatusError = true,
                                    )
                                }
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

        AppRoute.Scan -> ScanScreen(
            state = scanUiState,
            permissionRequestPending = scanStateModel.shouldRequestCameraPermission,
            onAction = { action ->
                val result = reduceScanState(
                    currentState = scanStateModel,
                    action = action,
                )
                scanStateModel = result.state

                when (val effect = result.effect) {
                    ScanEffect.NavigateBack -> currentRoute = AppRoute.Inbox
                    is ScanEffect.ResolvePayload -> {
                        coroutineScope.launch {
                            when (val paymentResult = paymentRepository.resolveScanPayload(effect.payload)) {
                                is PaymentRepositoryResult.ResolutionSuccess -> {
                                    scanStateModel = scanStateModel.withResolvedPayment(paymentResult.resolution)
                                }
                                is PaymentRepositoryResult.Failure -> {
                                    scanStateModel = scanStateModel.withResolveFailure(paymentResult.message)
                                }
                                else -> Unit
                            }
                        }
                    }
                    is ScanEffect.ConfirmPayment -> {
                        coroutineScope.launch {
                            val amount = effect.amountInput?.toDoubleOrNull()
                            if (effect.amountInput != null && amount == null) {
                                scanStateModel = scanStateModel.withConfirmFailure("Amount must be a valid decimal value.")
                                return@launch
                            }
                            when (
                                val paymentResult = paymentRepository.confirmPayment(
                                    sessionId = effect.sessionId,
                                    amount = amount,
                                    note = effect.note,
                                )
                            ) {
                                is PaymentRepositoryResult.ConfirmationSuccess -> {
                                    scanStateModel = scanStateModel.withConfirmedPayment(paymentResult.confirmation)
                                }
                                is PaymentRepositoryResult.Failure -> {
                                    scanStateModel = scanStateModel.withConfirmFailure(paymentResult.message)
                                }
                                else -> Unit
                            }
                        }
                    }
                    is ScanEffect.OpenConversation -> {
                        coroutineScope.launch {
                            if (appSessionState.isAuthenticated) {
                                conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                                    accountId = appSessionState.accountId,
                                    currentUserDisplayName = appSessionState.displayName,
                                )
                            }
                            conversationThreadsState = conversationRepository.markConversationAsRead(
                                state = conversationThreadsState,
                                conversationId = effect.conversationId,
                            )
                            selectedConversationId = effect.conversationId
                            currentRoute = AppRoute.Chat
                            conversationThreadsState = synchronizeActiveConversation(
                                state = conversationThreadsState,
                                conversationId = effect.conversationId,
                                conversationRepository = conversationRepository,
                            )
                        }
                    }
                    null -> Unit
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
                                    conversationThreadsState = synchronizeActiveConversation(
                                        state = conversationThreadsState,
                                        conversationId = action.conversationId,
                                        conversationRepository = conversationRepository,
                                    )
                                }
                            }

							InboxAction.OpenContacts -> currentRoute = AppRoute.Contacts

							InboxAction.OpenScan -> currentRoute = AppRoute.Scan

							InboxAction.SignOutToHud -> {
								val signedOutState = appShellCoordinator.createSignedOutState(previewThreadsState)
                                appSessionState = AppSessionState()
                                authFormState = signedOutState.authFormState
                                registerFormState = signedOutState.registerFormState
								contactsStateModel = signedOutState.contactsState
								callSessionJob?.cancel()
								callSessionState = signedOutState.callSessionState
								scanStateModel = ScanState()
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
                            is ChatEffect.AddMember -> {
                                coroutineScope.launch {
                                    conversationThreadsState = conversationRepository.addConversationMember(
                                        state = conversationThreadsState,
                                        conversationId = result.effect.conversationId,
                                        memberAccountId = result.effect.memberAccountId,
                                    )
                                    conversationThreadsState = conversationRepository.refreshConversationDetail(
                                        state = conversationThreadsState,
                                        conversationId = result.effect.conversationId,
                                    )
                                }
                            }
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
                                    val outgoingMessages = result.state.messages
                                        .filter { it.id in result.effect.messageIds }
                                    val conversationId = result.effect.conversationId
                                    var firstFailureMessage: String? = null

                                    outgoingMessages.forEach { outgoingMessage ->
                                        val sendResult = dispatchOutgoingChatMessage(
                                            state = conversationThreadsState,
                                            conversationId = conversationId,
                                            message = outgoingMessage,
                                            conversationRepository = conversationRepository,
                                            mediaRepository = mediaRepository,
                                        )
                                        conversationThreadsState = sendResult.state
                                        if (firstFailureMessage == null) {
                                            firstFailureMessage = sendResult.failureMessage
                                        }
                                    }

                                    if (firstFailureMessage != null) {
                                        chatStatusMessage = firstFailureMessage
                                        chatStatusError = true
                                    }
                                }
                            }
                            null -> Unit
                        }

                        if (action is ChatAction.DownloadAttachment) {
                            coroutineScope.launch {
                                when (val downloadResult = mediaRepository.downloadAttachment(
                                    action.attachmentId,
                                    action.fileName,
                                )) {
                                    is MediaRepositoryResult.DownloadSuccess -> {
                                        chatStatusMessage = "Saved to ${downloadResult.savedPath}"
                                        chatStatusError = false
                                    }
                                    is MediaRepositoryResult.Failure -> {
                                        chatStatusMessage = downloadResult.message
                                        chatStatusError = true
                                    }
                                    else -> Unit
                                }
                            }
                        }

                        if (action is ChatAction.RecallMessage) {
                            val recallableMessage = resolvedChatState.messages.firstOrNull { message ->
                                message.id == action.messageId &&
                                    message.isFromCurrentUser &&
                                    (message.deliveryStatus == ChatMessageDeliveryStatus.Sent ||
                                        message.deliveryStatus == ChatMessageDeliveryStatus.Delivered)
                            }
                            if (recallableMessage != null) {
                                coroutineScope.launch {
                                    conversationThreadsState = conversationRepository.recallMessage(
                                        state = conversationThreadsState,
                                        conversationId = resolvedChatState.conversationId,
                                        messageId = action.messageId,
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun markOutgoingMessagesFailed(
    state: ConversationThreadsState,
    conversationId: String,
    messageIds: List<String>,
): ConversationThreadsState {
    return state.copy(
        threads = state.threads.map { thread ->
            if (thread.id == conversationId) {
                thread.copy(
                    messages = thread.messages.map { message ->
                        if (message.id in messageIds) {
                            message.copy(deliveryStatus = ChatMessageDeliveryStatus.Failed)
                        } else {
                            message
                        }
                    }
                )
            } else {
                thread
            }
        }
    )
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

private fun isMediaPlaceholderBody(body: String): Boolean {
    return body.startsWith("[Image]") || body.startsWith("[Video]")
}

private fun mediaAttachmentKindFromPlaceholder(body: String): ChatDraftAttachmentKind? {
    return when {
        body.startsWith("[Image]") -> ChatDraftAttachmentKind.Image
        body.startsWith("[Video]") -> ChatDraftAttachmentKind.Video
        else -> null
    }
}

private fun mediaContentTypeFor(kind: ChatDraftAttachmentKind): String {
    return when (kind) {
        ChatDraftAttachmentKind.Image -> "image/jpeg"
        ChatDraftAttachmentKind.Video -> "video/mp4"
        ChatDraftAttachmentKind.Voice -> "audio/mpeg"
    }
}

private fun FinalizedAttachmentMetadata.toChatMessageAttachment(): ChatMessageAttachment {
    return ChatMessageAttachment(
        attachmentId = attachmentId,
        attachmentType = attachmentType,
        fileName = fileName,
        contentType = contentType,
        contentLength = contentLength,
    )
}

private data class OutgoingDispatchResult(
    val state: ConversationThreadsState,
    val failureMessage: String? = null,
)

private suspend fun dispatchOutgoingChatMessage(
    state: ConversationThreadsState,
    conversationId: String,
    message: ChatMessageItem,
    conversationRepository: ConversationRepository,
    mediaRepository: MediaRepository,
): OutgoingDispatchResult {
    val attachment = message.attachment
    if (attachment == null) {
        return OutgoingDispatchResult(
            state = conversationRepository.sendMessage(
                state = state,
                conversationId = conversationId,
                localMessageId = message.id,
                body = message.body,
                attachment = null,
            )
        )
    }

    val finalizedAttachment = if (attachment.attachmentId.isNullOrBlank()) {
        val attachmentKind = attachment.toDraftAttachmentKind()
        val fileName = attachment.fileName.ifBlank {
            buildMediaPlaceholderFileName(conversationId, message.id, attachmentKind)
        }
        when (val uploadSessionResult = mediaRepository.createUploadSession(
            conversationId = conversationId,
            attachmentKind = attachmentKind,
            fileName = fileName,
            contentType = attachment.contentType.ifBlank { mediaContentTypeFor(attachmentKind) },
            contentLength = attachment.contentLength,
            durationSeconds = null,
        )) {
            is MediaRepositoryResult.Success -> {
                val session = uploadSessionResult.session
                val placeholderBytes = buildMediaPlaceholderBytes(fileName, attachmentKind)
                when (val uploadResult = mediaRepository.uploadContent(session, placeholderBytes)) {
                    is MediaRepositoryResult.UploadSuccess -> {
                        when (val finalizeResult = mediaRepository.finalizeUploadSession(
                            sessionId = session.uploadSessionId,
                            conversationId = conversationId,
                            fileName = fileName,
                            contentType = session.contentType,
                            contentLength = placeholderBytes.size.toLong(),
                            objectKey = session.objectKey,
                        )) {
                            is MediaRepositoryResult.FinalizeSuccess -> finalizeResult.metadata.toChatMessageAttachment()
                            is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                                state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                                failureMessage = finalizeResult.message,
                            )
                            else -> null
                        }
                    }
                    is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                        state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                        failureMessage = uploadResult.message,
                    )
                    else -> null
                }
            }
            is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                failureMessage = uploadSessionResult.message,
            )
            else -> null
        }
    } else {
        attachment
    }

    if (finalizedAttachment == null) {
        return OutgoingDispatchResult(
            state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
            failureMessage = "File upload failed.",
        )
    }

    return OutgoingDispatchResult(
        state = conversationRepository.sendMessage(
            state = state,
            conversationId = conversationId,
            localMessageId = message.id,
            body = finalizedAttachment.fileName,
            attachment = finalizedAttachment,
        )
    )
}

private fun ChatMessageAttachment.toDraftAttachmentKind(): ChatDraftAttachmentKind {
    return when (attachmentType.uppercase()) {
        "IMAGE" -> ChatDraftAttachmentKind.Image
        "VIDEO" -> ChatDraftAttachmentKind.Video
        else -> ChatDraftAttachmentKind.Voice
    }
}

private fun buildMediaPlaceholderBytes(fileName: String, kind: ChatDraftAttachmentKind): ByteArray {
    return "$fileName|${kind.name}".toByteArray(Charsets.UTF_8)
}

private fun buildMediaPlaceholderFileName(conversationId: String, localMessageId: String, kind: ChatDraftAttachmentKind): String {
    val extension = when (kind) {
        ChatDraftAttachmentKind.Image -> "jpg"
        ChatDraftAttachmentKind.Video -> "mp4"
        else -> "bin"
    }
    return "$conversationId-${kind.name.lowercase()}-$localMessageId.$extension"
}

private fun buildFileMessageBodyWithFinalizedAttachment(
    kind: ChatDraftAttachmentKind,
    metadata: FinalizedAttachmentMetadata,
): String {
    return buildString {
        append("[File] ")
        append(kind.name)
        append(" · ")
        append(metadata.fileName)
        append(" · Download or Save As")
        append(" · attachmentId=")
        append(metadata.attachmentId)
        append(" · objectKey=")
        append(metadata.objectKey)
        append(" · uploadUrl=")
        append(metadata.uploadUrl)
    }
}
