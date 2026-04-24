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
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryEffect
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.AuthSubmissionResult
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionStatus
import com.kzzz3.argus.lens.feature.call.activateConnectingCallSession
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
import com.kzzz3.argus.lens.feature.call.startCallSession
import com.kzzz3.argus.lens.feature.call.tickActiveCallSession
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsScreen
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsScreen
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.contacts.reduceContactsState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.me.MeScreen
import com.kzzz3.argus.lens.feature.me.createMeUiState
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.realtime.buildRealtimeStatusLabel
import com.kzzz3.argus.lens.feature.realtime.isSseAuthFailure
import com.kzzz3.argus.lens.feature.realtime.RealtimeEventKind
import com.kzzz3.argus.lens.feature.realtime.realtimeReconnectDelayMillis
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.feature.wallet.withCurrentAccount
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
internal fun AppRouteHost(dependencies: AppDependencies) {
    val coroutineScope = rememberCoroutineScope()
    val realtimeClient = dependencies.realtimeClient
    val appShellCoordinator = dependencies.appShellCoordinator
    val appSessionCoordinator = dependencies.appSessionCoordinator
    val authCoordinator = dependencies.authCoordinator
    val newFriendsCoordinator = dependencies.newFriendsCoordinator
    val contactsCoordinator = dependencies.contactsCoordinator
    val walletRequestCoordinator = dependencies.walletRequestCoordinator
    val chatCoordinator = dependencies.chatCoordinator
    val realtimeCoordinator = dependencies.realtimeCoordinator
    val previewThreadsState = remember {
        appShellCoordinator.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val initialSessionSnapshot = dependencies.initialSessionSnapshot
    var currentRoute by rememberSaveable {
        mutableStateOf(
            if (initialSessionSnapshot.isAuthenticated && initialSessionSnapshot.accessToken.isNotBlank()) AppRoute.Inbox else AppRoute.AuthEntry
        )
    }
    var authFormState by rememberSaveable {
        mutableStateOf(AuthFormState())
    }
    var registerFormState by rememberSaveable {
        mutableStateOf(RegisterFormState())
    }
    var appSessionState by rememberSaveable {
        mutableStateOf(initialSessionSnapshot)
    }
    var contactsStateModel by rememberSaveable {
        mutableStateOf(ContactsState())
    }
    var callSessionState by rememberSaveable {
        mutableStateOf(CallSessionState())
    }
    var walletStateModel by rememberSaveable {
        mutableStateOf(WalletState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    var chatStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var chatStatusError by rememberSaveable { mutableStateOf(false) }
    var friendRequestsSnapshot by remember { mutableStateOf(FriendRequestsSnapshot(emptyList(), emptyList())) }
    var friendRequestsStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var friendRequestsStatusError by rememberSaveable { mutableStateOf(false) }
    var callSessionJob by remember { mutableStateOf<Job?>(null) }
    var hydratedConversationAccountId by remember {
        mutableStateOf(if (initialSessionSnapshot.isAuthenticated) initialSessionSnapshot.accountId else null)
    }
    var hydratedSession by remember { mutableStateOf(true) }
    var realtimeSubscription by remember { mutableStateOf<ConversationRealtimeSubscription?>(null) }
    var realtimeConnectionState by remember { mutableStateOf(ConversationRealtimeConnectionState.DISABLED) }
    var realtimeLastEventId by rememberSaveable { mutableStateOf("") }
    var realtimeReconnectAttempt by remember { mutableStateOf(0) }
    var realtimeReconnectGeneration by remember { mutableStateOf(0) }
    var realtimeReconnectJob by remember { mutableStateOf<Job?>(null) }
    var sessionRefreshJob by remember { mutableStateOf<Job?>(null) }
    var walletRequestGeneration by remember { mutableStateOf(0) }
    var activeRealtimeConnectionId by remember { mutableStateOf("") }
    val realtimeEventMutex = remember { Mutex() }
    val walletRequestJobs = remember { mutableSetOf<Job>() }
    var friends by remember { mutableStateOf<List<FriendEntry>>(emptyList()) }
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
        resolvePreviewDisplayName(appSessionState.displayName)
    }
    val shellStatusLabel = remember(appSessionState.isAuthenticated, realtimeConnectionState) {
        when {
            !appSessionState.isAuthenticated -> "Signed out"
            realtimeConnectionState == ConversationRealtimeConnectionState.LIVE -> "Online"
            realtimeConnectionState == ConversationRealtimeConnectionState.RECOVERING -> "Reconnecting"
            realtimeConnectionState == ConversationRealtimeConnectionState.CONNECTING -> "Connecting"
            else -> "Offline"
        }
    }
    val shellStatusSummary = remember(appSessionState.isAuthenticated, realtimeConnectionState) {
        when {
            !appSessionState.isAuthenticated -> "Sign in to enter the Argus IM shell."
            realtimeConnectionState == ConversationRealtimeConnectionState.LIVE -> "Realtime channel connected and syncing now."
            realtimeConnectionState == ConversationRealtimeConnectionState.RECOVERING -> "Network unavailable or connection interrupted. Reconnecting now."
            realtimeConnectionState == ConversationRealtimeConnectionState.CONNECTING -> "Connecting secure realtime channel..."
            else -> "Cached shell is available offline. Sign in again if your session was revoked or wait for the network to recover."
        }
    }
    val inboxState = remember(appSessionState, conversationThreads, realtimeConnectionState, shellStatusLabel) {
        createInboxUiState(
            sessionState = appSessionState,
            threads = conversationThreads,
            realtimeStatusLabel = buildRealtimeStatusLabel(realtimeConnectionState),
            shellStatusLabel = shellStatusLabel,
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
    val walletUiState = remember(walletStateModel) {
        createWalletUiState(walletStateModel)
    }
    val meUiState = remember(
        appSessionState,
        walletStateModel.summary,
        friends,
        conversationThreads,
        shellStatusLabel,
        shellStatusSummary,
    ) {
        createMeUiState(
            sessionState = appSessionState,
            walletState = walletStateModel,
            friends = friends,
            conversationThreads = conversationThreads,
            shellStatusLabel = shellStatusLabel,
            shellStatusSummary = shellStatusSummary,
        )
    }
    val newFriendsUiState = remember(friendRequestsSnapshot, friendRequestsStatusMessage, friendRequestsStatusError) {
        NewFriendsUiState(
            title = "New Friends",
            subtitle = "Review incoming requests and track the status of requests you have sent.",
            isLoading = false,
            statusMessage = friendRequestsStatusMessage,
            isStatusError = friendRequestsStatusError,
            incoming = friendRequestsSnapshot.incoming,
            outgoing = friendRequestsSnapshot.outgoing,
        )
    }

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId, initialSessionSnapshot.accessToken) {
        if (initialSessionSnapshot.isAuthenticated && initialSessionSnapshot.accountId.isNotBlank()) {
            conversationThreadsState = appShellCoordinator.loadInitialAuthenticatedConversations(initialSessionSnapshot)
            hydratedConversationAccountId = initialSessionSnapshot.accountId
            currentRoute = AppRoute.Inbox
            return@LaunchedEffect
        }

        val hydratedState = appShellCoordinator.hydrateAppState(previewThreadsState)
        appSessionState = hydratedState.session
        conversationThreadsState = hydratedState.conversationThreadsState
        hydratedConversationAccountId = hydratedState.hydratedConversationAccountId
        if (hydratedState.session.isAuthenticated) {
            currentRoute = AppRoute.Inbox
        }
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

    fun openTopLevelRoute(route: AppRoute) {
        if (route == AppRoute.Wallet) {
            walletStateModel = walletStateModel.withCurrentAccount(appSessionState.accountId)
        }
        currentRoute = route
    }

    fun openInboxConversation(conversationId: String) {
        val openResult = chatCoordinator.openConversation(
            state = conversationThreadsState,
            conversationId = conversationId,
        )
        conversationThreadsState = openResult.conversationThreadsState
        selectedConversationId = openResult.conversationId
        currentRoute = AppRoute.Chat
        coroutineScope.launch {
            conversationThreadsState = chatCoordinator.synchronizeConversation(
                state = conversationThreadsState,
                conversationId = conversationId,
            )
        }
    }

    fun invalidateWalletRequests() {
        walletRequestGeneration += 1
        walletRequestJobs.toList().forEach { it.cancel() }
        walletRequestJobs.clear()
    }

    fun isActiveWalletRequest(accountId: String, generation: Int): Boolean {
        return shouldApplyWalletRequestResult(
            currentSession = appSessionState,
            requestAccountId = accountId,
            requestGeneration = generation,
            activeGeneration = walletRequestGeneration,
        )
    }

    fun launchWalletRequest(block: suspend (String, Int) -> Unit) {
        val requestAccountId = appSessionState.accountId
        val requestGeneration = walletRequestGeneration
        val job = coroutineScope.launch {
            block(requestAccountId, requestGeneration)
        }
        walletRequestJobs += job
        job.invokeOnCompletion {
            walletRequestJobs.remove(job)
        }
    }

    fun launchWalletStateRequest(block: suspend (WalletState) -> WalletState) {
        launchWalletRequest { requestAccountId, requestGeneration ->
            val nextState = block(walletStateModel)
            walletStateModel = applyWalletRequestResult(
                currentState = walletStateModel,
                isActive = isActiveWalletRequest(requestAccountId, requestGeneration),
            ) { nextState }
        }
    }

    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
    ) {
        appSessionState = createSessionFromAuthSession(authResult.session)
        invalidateWalletRequests()
        hydratedConversationAccountId = null
        callSessionJob?.cancel()
        val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
        val postAuthUiState = createPostAuthUiState(
            signedInState = signedInState,
            accountId = authResult.session.accountId,
        )
        callSessionState = postAuthUiState.callSessionState
        walletStateModel = WalletState()
        conversationThreadsState = postAuthUiState.conversationThreadsState
        hydratedConversationAccountId = postAuthUiState.hydratedConversationAccountId
        selectedConversationId = postAuthUiState.selectedConversationId
        authFormState = if (keepSubmitMessageOnAuthForm) {
            postAuthUiState.nextAuthFormState.copy(submitResult = authResult.session.message)
        } else {
            postAuthUiState.nextAuthFormState
        }
        realtimeReconnectGeneration += postAuthUiState.realtimeReconnectIncrement
        currentRoute = AppRoute.Inbox
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        friendRequestsSnapshot = statusState.snapshot
        friendRequestsStatusMessage = statusState.message
        friendRequestsStatusError = statusState.isError
    }

    fun signOutToEntry(message: String? = null) {
        val signedOutAccountId = appSessionState.accountId
        val signedOutState = appShellCoordinator.createSignedOutState(
            previewThreadsState = previewThreadsState,
            signedOutAccountId = signedOutAccountId,
        )
        sessionRefreshJob?.cancel()
        sessionRefreshJob = null
        invalidateWalletRequests()
        appSessionState = AppSessionState()
        authFormState = signedOutState.authFormState.copy(submitResult = message)
        registerFormState = signedOutState.registerFormState
        contactsStateModel = signedOutState.contactsState
        callSessionJob?.cancel()
        callSessionState = signedOutState.callSessionState
        walletStateModel = WalletState()
        hydratedConversationAccountId = null
        conversationThreadsState = signedOutState.conversationThreadsState
        selectedConversationId = signedOutState.selectedConversationId
        friends = emptyList()
        friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList())
        friendRequestsStatusMessage = null
        friendRequestsStatusError = false
        currentRoute = AppRoute.AuthEntry
    }

    suspend fun refreshSessionTokens(): AuthRepositoryResult {
        val refreshResult = appSessionCoordinator.refreshSession(appSessionState)
        appSessionState = refreshResult.session
        return refreshResult.repositoryResult
    }

    fun scheduleSessionRefreshLoop() {
        if (sessionRefreshJob?.isActive == true) return
        if (!appSessionState.isAuthenticated || appSessionState.refreshToken.isBlank()) return
        sessionRefreshJob = coroutineScope.launch {
            while (appSessionState.isAuthenticated && realtimeConnectionState == ConversationRealtimeConnectionState.LIVE) {
                delay(60 * 60 * 1000L)
                if (!appSessionState.isAuthenticated || appSessionState.refreshToken.isBlank()) {
                    break
                }
                val refreshResult = appSessionCoordinator.refreshSessionWithToken(
                    session = appSessionState,
                    refreshToken = appSessionState.refreshToken,
                )
                appSessionState = refreshResult.session
                when (val repositoryResult = refreshResult.repositoryResult) {
                    is AuthRepositoryResult.Success -> {
                        Unit
                    }
                    is AuthRepositoryResult.Failure -> {
                        if (repositoryResult.kind == AuthFailureKind.UNAUTHORIZED) {
                            signOutToEntry("Session expired or was revoked. Please sign in again.")
                            break
                        }
                    }
                }
            }
            sessionRefreshJob = null
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
                        scheduleSessionRefreshLoop()
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
                    when (realtimeCoordinator.classifyEvent(event)) {
                        RealtimeEventKind.StreamReady -> {
                            realtimeConnectionState = ConversationRealtimeConnectionState.LIVE
                            return@launch
                        }
                        RealtimeEventKind.Heartbeat -> return@launch
                        RealtimeEventKind.DomainEvent -> Unit
                    }
                    realtimeEventMutex.withLock {
                        conversationThreadsState = realtimeCoordinator.applyEvent(
                            event = event,
                            session = latestAppSessionState,
                            currentState = conversationThreadsState,
                            selectedConversationId = latestSelectedConversationId,
                            currentRoute = latestCurrentRoute,
                        )
                    }
                }
            },
            onError = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        if (isSseAuthFailure(it)) {
                            when (val refreshResult = refreshSessionTokens()) {
                                is AuthRepositoryResult.Success -> {
                                    realtimeReconnectAttempt = 0
                                    realtimeReconnectGeneration += 1
                                }
                                is AuthRepositoryResult.Failure -> {
                                    if (refreshResult.kind == AuthFailureKind.UNAUTHORIZED) {
                                        signOutToEntry("Session expired or was revoked. Please sign in again.")
                                    } else {
                                        scheduleRealtimeReconnect()
                                    }
                                }
                            }
                        } else {
                            scheduleRealtimeReconnect()
                        }
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
            sessionRefreshJob?.cancel()
            sessionRefreshJob = null
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
            contactsCoordinator.loadFriends()?.let { friends = it }
        }
        if (currentRoute == AppRoute.NewFriends && appSessionState.isAuthenticated) {
            applyFriendRequestStatus(newFriendsCoordinator.loadRequests(friendRequestsSnapshot))
        }
    }

    if (!hydratedSession) {
        AppLaunchPlaceholder()
        return
    }

    when (currentRoute) {
        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onAction = { action ->
                val result = reduceAuthFormState(
                    currentState = authFormState,
                    action = action,
                )
                authFormState = result.formState

                when (result.effect) {
                    AuthEntryEffect.NavigateBack -> Unit
                    AuthEntryEffect.NavigateToRegister -> currentRoute = AppRoute.RegisterEntry
                    is AuthEntryEffect.SubmitPasswordLogin -> {
                        coroutineScope.launch {
                            when (val authResult = authCoordinator.login(
                                formState = authFormState,
                                account = result.effect.account,
                                password = result.effect.password,
                            )) {
                                is AuthSubmissionResult.Success -> {
                                    authFormState = authResult.formState
                                    applySuccessfulAuthResult(
                                        authResult = authResult.authResult,
                                        keepSubmitMessageOnAuthForm = true,
                                    )
                                }
                                is AuthSubmissionResult.Failure -> {
                                    authFormState = authResult.formState
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
                            when (val authResult = authCoordinator.register(
                                formState = registerFormState,
                                displayName = result.effect.displayName,
                                account = result.effect.account,
                                password = result.effect.password,
                            )) {
                                is AuthSubmissionResult.Success -> {
                                    registerFormState = authResult.formState
                                    applySuccessfulAuthResult(
                                        authResult = authResult.authResult,
                                        keepSubmitMessageOnAuthForm = false,
                                    )
                                }
                                is AuthSubmissionResult.Failure -> {
                                    registerFormState = authResult.formState
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.Inbox -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            InboxScreen(
                state = inboxState,
                onAction = { action ->
                    when (action) {
                        is InboxAction.OpenConversation -> openInboxConversation(action.conversationId)

                        InboxAction.OpenContacts -> openTopLevelRoute(AppRoute.Contacts)
                        InboxAction.OpenWallet -> openTopLevelRoute(AppRoute.Wallet)
                        InboxAction.SignOutToHud -> signOutToEntry()
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Contacts -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            ContactsScreen(
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
                                val openResult = contactsCoordinator.openConversation(
                                    session = appSessionState,
                                    requestedConversationId = effect.conversationId,
                                    friends = friends,
                                    state = conversationThreadsState,
                                )
                                conversationThreadsState = openResult.conversationThreadsState
                                selectedConversationId = openResult.conversationId
                                currentRoute = AppRoute.Chat
                            }
                        }

                        is ContactsEffect.AddFriend -> {
                            coroutineScope.launch {
                                val addFriendResult = contactsCoordinator.addFriend(
                                    currentContactsState = contactsStateModel,
                                    friendAccountId = effect.friendAccountId,
                                )
                                contactsStateModel = addFriendResult.contactsState
                                addFriendResult.friendRequestsSnapshot?.let { friendRequestsSnapshot = it }
                            }
                        }
                        ContactsEffect.OpenNewFriends -> currentRoute = AppRoute.NewFriends

                        ContactsEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.NewFriends -> AuthenticatedShell(
            currentRoute = AppRoute.Contacts,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            NewFriendsScreen(
                state = newFriendsUiState,
                onAction = { action ->
                    when (action) {
                        NewFriendsAction.NavigateBack -> currentRoute = AppRoute.Contacts
                        is NewFriendsAction.Accept -> {
                            coroutineScope.launch {
                                val result = newFriendsCoordinator.accept(
                                    requestId = action.requestId,
                                    session = appSessionState,
                                    currentSnapshot = friendRequestsSnapshot,
                                    currentConversationState = conversationThreadsState,
                                )
                                applyFriendRequestStatus(result.status)
                                result.friends?.let { friends = it }
                                result.conversationThreadsState?.let { conversationThreadsState = it }
                            }
                        }
                        is NewFriendsAction.Reject -> {
                            coroutineScope.launch {
                                val result = newFriendsCoordinator.reject(
                                    requestId = action.requestId,
                                    currentSnapshot = friendRequestsSnapshot,
                                )
                                applyFriendRequestStatus(result.status)
                            }
                        }
                        is NewFriendsAction.Ignore -> {
                            coroutineScope.launch {
                                val result = newFriendsCoordinator.ignore(
                                    requestId = action.requestId,
                                    currentSnapshot = friendRequestsSnapshot,
                                )
                                applyFriendRequestStatus(result.status)
                            }
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }
        AppRoute.CallSession -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            CallSessionScreen(
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
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Wallet -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            WalletScreen(
                state = walletUiState,
                permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
                onAction = { action ->
                    val result = reduceWalletState(
                        currentState = walletStateModel,
                        action = action,
                    )
                    walletStateModel = result.state

                    when (val effect = result.effect) {
                        WalletEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                        WalletEffect.LoadWalletSummary -> {
                            launchWalletStateRequest { currentState ->
                                walletRequestCoordinator.loadWalletSummary(currentState)
                            }
                        }
                        is WalletEffect.ResolvePayload -> {
                            launchWalletStateRequest { currentState ->
                                walletRequestCoordinator.resolvePayload(currentState, effect.payload)
                            }
                        }
                        is WalletEffect.ConfirmPayment -> {
                            launchWalletStateRequest { currentState ->
                                walletRequestCoordinator.confirmPayment(
                                    currentState = currentState,
                                    sessionId = effect.sessionId,
                                    amountInput = effect.amountInput,
                                    note = effect.note,
                                )
                            }
                        }
                        WalletEffect.LoadPaymentHistory -> {
                            launchWalletStateRequest { currentState ->
                                walletRequestCoordinator.loadPaymentHistory(currentState)
                            }
                        }
                        is WalletEffect.LoadPaymentReceipt -> {
                            launchWalletStateRequest { currentState ->
                                walletRequestCoordinator.loadPaymentReceipt(currentState, effect.paymentId)
                            }
                        }
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Me -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            MeScreen(
                state = meUiState,
                onSignOut = ::signOutToEntry,
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Chat -> {
            val resolvedChatUiState = chatUiState
            val resolvedChatState = chatState

            if (resolvedChatUiState == null || resolvedChatState == null) {
                AuthenticatedShell(
                    currentRoute = AppRoute.Inbox,
                    onTabSelected = ::openTopLevelRoute,
                ) { innerPadding ->
                    InboxScreen(
                        state = inboxState,
                        onAction = { action ->
                            when (action) {
                                is InboxAction.OpenConversation -> openInboxConversation(action.conversationId)

                                InboxAction.OpenContacts -> openTopLevelRoute(AppRoute.Contacts)
                                InboxAction.OpenWallet -> openTopLevelRoute(AppRoute.Wallet)
                                InboxAction.SignOutToHud -> signOutToEntry()
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            } else {
                AuthenticatedShell(
                    currentRoute = currentRoute,
                    onTabSelected = ::openTopLevelRoute,
                ) { innerPadding ->
                    ChatScreen(
                        state = resolvedChatUiState,
                        onAction = { action ->
                            val result = chatCoordinator.reduceAction(
                                currentThreadsState = conversationThreadsState,
                                currentChatState = resolvedChatState,
                                action = action,
                            )
                            conversationThreadsState = result.conversationThreadsState

                            when (val effect = result.effect) {
                                ChatEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                                is ChatEffect.StartCall -> {
                                    callSessionState = startCallSession(
                                        conversationId = effect.conversationId,
                                        contactDisplayName = effect.contactDisplayName,
                                        mode = if (effect.mode == ChatCallMode.Video) {
                                            CallSessionMode.Video
                                        } else {
                                            CallSessionMode.Audio
                                        },
                                    )
                                    callSessionJob?.cancel()
                                    callSessionJob = coroutineScope.launch {
                                        delay(800)
                                        callSessionState = activateConnectingCallSession(callSessionState)
                                        while (callSessionState.status == CallSessionStatus.Active && currentRoute == AppRoute.CallSession) {
                                            delay(1000)
                                            callSessionState = tickActiveCallSession(callSessionState)
                                        }
                                    }
                                    currentRoute = AppRoute.CallSession
                                }
                                is ChatEffect.DispatchOutgoingMessages -> {
                                    coroutineScope.launch {
                                        val outgoingMessages = result.chatState.messages
                                            .filter { it.id in effect.messageIds }
                                        val dispatchResult = chatCoordinator.dispatchOutgoingMessages(
                                            state = conversationThreadsState,
                                            conversationId = effect.conversationId,
                                            messages = outgoingMessages,
                                        )
                                        conversationThreadsState = dispatchResult.conversationThreadsState
                                        if (dispatchResult.summary?.failureMessage != null) {
                                            chatStatusMessage = dispatchResult.summary.failureMessage
                                            chatStatusError = true
                                        }
                                    }
                                }
                                null -> Unit
                            }

                            if (action is ChatAction.DownloadAttachment) {
                                coroutineScope.launch {
                                    val downloadResult = chatCoordinator.downloadAttachment(
                                        attachmentId = action.attachmentId,
                                        fileName = action.fileName,
                                    )
                                    chatStatusMessage = downloadResult.message
                                    chatStatusError = downloadResult.isError
                                }
                            }

                            if (action is ChatAction.RecallMessage) {
                                coroutineScope.launch {
                                    conversationThreadsState = chatCoordinator.recallMessage(
                                        state = conversationThreadsState,
                                        chatState = resolvedChatState,
                                        messageId = action.messageId,
                                    )
                                }
                            }
                        },                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}






