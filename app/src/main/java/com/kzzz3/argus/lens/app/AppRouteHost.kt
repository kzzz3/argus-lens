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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.data.session.SessionCredentials
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
import com.kzzz3.argus.lens.feature.call.CallSessionRuntime
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
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
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.feature.wallet.withCurrentAccount
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
internal fun AppRouteHost(
    dependencies: AppDependencies,
    appSessionState: AppSessionState,
    currentRoute: AppRoute,
    authFormState: AuthFormState,
    registerFormState: RegisterFormState,
    callSessionState: CallSessionState,
    contactsState: ContactsState,
    walletStateModel: WalletState,
    friends: List<FriendEntry>,
    selectedConversationId: String,
    chatStatusMessage: String?,
    chatStatusError: Boolean,
    friendRequestsSnapshot: FriendRequestsSnapshot,
    friendRequestsStatusMessage: String?,
    friendRequestsStatusError: Boolean,
    hydratedConversationAccountId: String?,
    realtimeConnectionState: ConversationRealtimeConnectionState,
    realtimeLastEventId: String,
    realtimeReconnectGeneration: Int,
    onRouteChanged: (AppRoute) -> Unit,
    onAuthFormStateChanged: (AuthFormState) -> Unit,
    onRegisterFormStateChanged: (RegisterFormState) -> Unit,
    onCallSessionStateChanged: (CallSessionState) -> Unit,
    onContactsStateChanged: (ContactsState) -> Unit,
    onWalletStateChanged: (WalletState) -> Unit,
    onFriendsChanged: (List<FriendEntry>) -> Unit,
    onConversationOpened: (String) -> Unit,
    onConversationSelectionCleared: () -> Unit,
    onChatStatusChanged: (String?, Boolean) -> Unit,
    onChatStatusCleared: () -> Unit,
    onFriendRequestStatusChanged: (FriendRequestStatusState) -> Unit,
    onFriendRequestsSnapshotChanged: (FriendRequestsSnapshot) -> Unit,
    onFriendRequestStatusReset: () -> Unit,
    onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
    onAuthenticatedSessionApplied: (AppSessionState, SessionCredentials, String, Int) -> Unit,
    onSessionRefreshed: (AppSessionState) -> Unit,
    onSessionCleared: () -> Unit,
    onHydratedConversationAccountChanged: (String?) -> Unit,
    onRealtimeConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    onRealtimeEventIdRecorded: (String) -> Unit,
    onRealtimeLastEventIdReset: () -> Unit,
    onRealtimeReconnectIncremented: () -> Unit,
    onRealtimeReconnectIncrementedBy: (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val realtimeClient = dependencies.realtimeClient
    val appShellCoordinator = dependencies.appShellCoordinator
    val appSessionCoordinator = dependencies.appSessionCoordinator
    val authCoordinator = dependencies.authCoordinator
    val newFriendsCoordinator = dependencies.newFriendsCoordinator
    val contactsCoordinator = dependencies.contactsCoordinator
    val walletRequestCoordinator = dependencies.walletRequestCoordinator
    val chatCoordinator = dependencies.chatCoordinator
    val realtimeCoordinator = dependencies.realtimeCoordinator
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val previewThreadsState = remember {
        appShellCoordinator.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val initialSessionSnapshot = dependencies.initialSessionSnapshot
    val callSessionRuntime = remember(coroutineScope) { CallSessionRuntime(coroutineScope) }
    var realtimeSubscription by remember { mutableStateOf<ConversationRealtimeSubscription?>(null) }
    val realtimeReconnectRuntime = remember(coroutineScope) { RealtimeReconnectRuntime(coroutineScope) }
    val sessionRefreshRuntime = remember(coroutineScope, appSessionCoordinator, sessionCredentialsStore) {
        SessionRefreshRuntime(
            scope = coroutineScope,
            appSessionCoordinator = appSessionCoordinator,
            credentialsStore = sessionCredentialsStore,
        )
    }
    val walletRequestRuntime = remember(coroutineScope) { WalletRequestRuntime(coroutineScope) }
    var activeRealtimeConnectionId by remember { mutableStateOf("") }
    val realtimeEventMutex = remember { Mutex() }
    var conversationThreadsState by remember {
        mutableStateOf(previewThreadsState)
    }
    val startDestination = remember { currentRoute.name }
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
    val contactsUiState = remember(contactsState, friends, conversationThreads, appSessionState.accountId) {
        createContactsUiState(
            state = contactsState,
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
        appSessionState.isAuthenticated && sessionCredentialsStore.current.hasAccessToken
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

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId) {
        if (initialSessionSnapshot.isAuthenticated && initialSessionSnapshot.accountId.isNotBlank() && dependencies.initialSessionCredentials.hasAccessToken) {
            conversationThreadsState = appShellCoordinator.loadInitialAuthenticatedConversations(initialSessionSnapshot)
            onHydratedConversationAccountChanged(initialSessionSnapshot.accountId)
            onRouteChanged(AppRoute.Inbox)
            return@LaunchedEffect
        }

        val hydratedState = appShellCoordinator.hydrateAppState(previewThreadsState)
        onHydratedSessionApplied(hydratedState.session, hydratedState.hydratedConversationAccountId)
        conversationThreadsState = hydratedState.conversationThreadsState
    }

    LaunchedEffect(appSessionState) {
        appShellCoordinator.persistSession(appSessionState, sessionCredentialsStore.current)
    }

    LaunchedEffect(selectedConversationId) {
        onChatStatusCleared()
    }

    fun scheduleRealtimeReconnect() {
        realtimeReconnectRuntime.schedule(
            isEnabled = { latestRealtimeEnabled },
            markRecovering = { onRealtimeConnectionStateChanged(ConversationRealtimeConnectionState.RECOVERING) },
            incrementGeneration = onRealtimeReconnectIncremented,
        )
    }

    fun openTopLevelRoute(route: AppRoute) {
        if (route == AppRoute.Wallet) {
            onWalletStateChanged(walletStateModel.withCurrentAccount(appSessionState.accountId))
        }
        onRouteChanged(route)
    }

    fun openShellDestination(destination: ShellDestination) {
        when (destination) {
            ShellDestination.Inbox -> openTopLevelRoute(AppRoute.Inbox)
            ShellDestination.Contacts -> openTopLevelRoute(AppRoute.Contacts)
            ShellDestination.Wallet -> openTopLevelRoute(AppRoute.Wallet)
            ShellDestination.Me -> openTopLevelRoute(AppRoute.Me)
            ShellDestination.Secondary -> Unit
        }
    }

    fun openInboxConversation(conversationId: String) {
        val openResult = chatCoordinator.openConversation(
            state = conversationThreadsState,
            conversationId = conversationId,
        )
        conversationThreadsState = openResult.conversationThreadsState
        onConversationOpened(openResult.conversationId)
        coroutineScope.launch {
            conversationThreadsState = chatCoordinator.synchronizeConversation(
                state = conversationThreadsState,
                conversationId = conversationId,
            )
        }
    }

    fun launchWalletStateRequest(block: suspend (WalletState) -> WalletState) {
        walletRequestRuntime.launchStateRequest(
            requestSession = appSessionState,
            getCurrentSession = { appSessionState },
            getCurrentState = { walletStateModel },
            setState = onWalletStateChanged,
            block = block,
        )
    }

    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
    ) {
        val authenticatedSession = createSessionFromAuthSession(authResult.session)
        val authenticatedCredentials = createSessionCredentialsFromAuthSession(authResult.session)
        walletRequestRuntime.invalidate()
        onHydratedConversationAccountChanged(null)
        callSessionRuntime.cancel()
        val signedInState = appShellCoordinator.handleSignedIn(authenticatedSession)
        val postAuthUiState = createPostAuthUiState(
            signedInState = signedInState,
            accountId = authResult.session.accountId,
        )
        onCallSessionStateChanged(postAuthUiState.callSessionState)
        onWalletStateChanged(WalletState())
        conversationThreadsState = postAuthUiState.conversationThreadsState
        onAuthenticatedSessionApplied(
            authenticatedSession,
            authenticatedCredentials,
            postAuthUiState.hydratedConversationAccountId,
            postAuthUiState.realtimeReconnectIncrement,
        )
        onAuthFormStateChanged(if (keepSubmitMessageOnAuthForm) {
            postAuthUiState.nextAuthFormState.copy(submitResult = authResult.session.message)
        } else {
            postAuthUiState.nextAuthFormState
        })
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        onFriendRequestStatusChanged(statusState)
    }

    fun signOutToEntry(message: String? = null) {
        val signedOutAccountId = appSessionState.accountId
        val signedOutState = appShellCoordinator.createSignedOutState(
            previewThreadsState = previewThreadsState,
            signedOutAccountId = signedOutAccountId,
        )
        sessionRefreshRuntime.cancel()
        walletRequestRuntime.invalidate()
        onSessionCleared()
        onAuthFormStateChanged(signedOutState.authFormState.copy(submitResult = message))
        onRegisterFormStateChanged(signedOutState.registerFormState)
        onContactsStateChanged(signedOutState.contactsState)
        callSessionRuntime.cancel()
        onCallSessionStateChanged(signedOutState.callSessionState)
        onWalletStateChanged(WalletState())
        conversationThreadsState = signedOutState.conversationThreadsState
        onFriendsChanged(emptyList())
        onFriendRequestStatusReset()
    }

    suspend fun refreshSessionTokens(): AuthRepositoryResult {
        return sessionRefreshRuntime.refreshOnce(
            session = appSessionState,
            setSession = onSessionRefreshed,
        )
    }

    fun scheduleSessionRefreshLoop() {
        sessionRefreshRuntime.startLoopIfNeeded(
            getSession = { appSessionState },
            getConnectionState = { realtimeConnectionState },
            setSession = onSessionRefreshed,
            onUnauthorized = { signOutToEntry("Session expired or was revoked. Please sign in again.") },
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        if (appSessionState.isAuthenticated) {
            onHydratedConversationAccountChanged(null)
            val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
            conversationThreadsState = signedInState.conversationThreadsState
            onHydratedConversationAccountChanged(signedInState.hydratedConversationAccountId)
        } else {
            onHydratedConversationAccountChanged(null)
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, realtimeReconnectGeneration) {
        activeRealtimeConnectionId = ""
        realtimeSubscription?.close()
        realtimeSubscription = null

        val realtimeCredentials = sessionCredentialsStore.current
        val realtimeEnabled = appSessionState.isAuthenticated && realtimeCredentials.hasAccessToken
        if (!realtimeEnabled) {
            realtimeReconnectRuntime.disable()
            onRealtimeLastEventIdReset()
            onRealtimeConnectionStateChanged(ConversationRealtimeConnectionState.DISABLED)
            return@LaunchedEffect
        }

        val connectionId = "realtime-${appSessionState.accountId}-${realtimeReconnectGeneration}"
        activeRealtimeConnectionId = connectionId
        onRealtimeConnectionStateChanged(if (realtimeReconnectRuntime.currentAttempt > 0) {
            ConversationRealtimeConnectionState.RECOVERING
        } else {
            ConversationRealtimeConnectionState.CONNECTING
        })
        realtimeSubscription = realtimeClient.connect(
            accessToken = realtimeCredentials.accessToken,
            lastEventId = realtimeLastEventId.ifBlank { null },
            onConnected = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        onRealtimeConnectionStateChanged(ConversationRealtimeConnectionState.LIVE)
                        realtimeReconnectRuntime.markConnected()
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
                        onRealtimeEventIdRecorded(event.eventId)
                    }
                    when (realtimeCoordinator.classifyEvent(event)) {
                        RealtimeEventKind.StreamReady -> {
                            onRealtimeConnectionStateChanged(ConversationRealtimeConnectionState.LIVE)
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
                            isChatRouteActive = latestCurrentRoute == AppRoute.Chat,
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
                                    realtimeReconnectRuntime.markConnected()
                                    onRealtimeReconnectIncremented()
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
            realtimeReconnectRuntime.disable()
            sessionRefreshRuntime.cancel()
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
            contactsCoordinator.loadFriends()?.let(onFriendsChanged)
        }
        if (currentRoute == AppRoute.NewFriends && appSessionState.isAuthenticated) {
            applyFriendRequestStatus(newFriendsCoordinator.loadRequests(friendRequestsSnapshot))
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(currentRoute, currentBackStackEntry?.destination?.route) {
        if (currentBackStackEntry?.destination?.route != currentRoute.name) {
            navController.navigate(currentRoute.name) {
                launchSingleTop = true
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(AppRoute.AuthEntry.name) {
            AuthEntryScreen(
            state = authState,
            onAction = { action ->
                val result = reduceAuthFormState(
                    currentState = authFormState,
                    action = action,
                )
                onAuthFormStateChanged(result.formState)

                when (val effect = result.effect) {
                    AuthEntryEffect.NavigateBack -> Unit
                    AuthEntryEffect.NavigateToRegister -> onRouteChanged(AppRoute.RegisterEntry)
                    is AuthEntryEffect.SubmitPasswordLogin -> {
                        coroutineScope.launch {
                            when (val authResult = authCoordinator.login(
                                formState = authFormState,
                                account = effect.account,
                                password = effect.password,
                            )) {
                                is AuthSubmissionResult.Success -> {
                                    onAuthFormStateChanged(authResult.formState)
                                    applySuccessfulAuthResult(
                                        authResult = authResult.authResult,
                                        keepSubmitMessageOnAuthForm = true,
                                    )
                                }
                                is AuthSubmissionResult.Failure -> {
                                    onAuthFormStateChanged(authResult.formState)
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
            )
        }

        composable(AppRoute.RegisterEntry.name) {
            RegisterScreen(
            state = registerState,
            onAction = { action ->
                val result = reduceRegisterFormState(
                    currentState = registerFormState,
                    action = action,
                )
                onRegisterFormStateChanged(result.formState)

                when (val effect = result.effect) {
                    RegisterEffect.NavigateBackToLogin -> onRouteChanged(AppRoute.AuthEntry)
                    is RegisterEffect.SubmitRegistration -> {
                        coroutineScope.launch {
                            when (val authResult = authCoordinator.register(
                                formState = registerFormState,
                                displayName = effect.displayName,
                                account = effect.account,
                                password = effect.password,
                            )) {
                                is AuthSubmissionResult.Success -> {
                                    onRegisterFormStateChanged(authResult.formState)
                                    applySuccessfulAuthResult(
                                        authResult = authResult.authResult,
                                        keepSubmitMessageOnAuthForm = false,
                                    )
                                }
                                is AuthSubmissionResult.Failure -> {
                                    onRegisterFormStateChanged(authResult.formState)
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
            )
        }

        composable(AppRoute.Inbox.name) { AuthenticatedShell(
            currentDestination = currentRoute.toShellDestination(),
            onTabSelected = ::openShellDestination,
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
        } }

        composable(AppRoute.Contacts.name) { AuthenticatedShell(
            currentDestination = currentRoute.toShellDestination(),
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
        ContactsScreen(
                state = contactsUiState,
                onAction = { action ->
                    val result = reduceContactsState(
                        currentState = contactsState,
                        action = action,
                    )
                    onContactsStateChanged(result.state)

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
                                onConversationOpened(openResult.conversationId)
                            }
                        }

                        is ContactsEffect.AddFriend -> {
                            coroutineScope.launch {
                                val addFriendResult = contactsCoordinator.addFriend(
                                    currentContactsState = contactsState,
                                    friendAccountId = effect.friendAccountId,
                                )
                                onContactsStateChanged(addFriendResult.contactsState)
                                addFriendResult.friendRequestsSnapshot?.let(onFriendRequestsSnapshotChanged)
                            }
                        }
                        ContactsEffect.OpenNewFriends -> onRouteChanged(AppRoute.NewFriends)

                        ContactsEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        } }

        composable(AppRoute.NewFriends.name) { AuthenticatedShell(
            currentDestination = ShellDestination.Contacts,
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
            NewFriendsScreen(
                state = newFriendsUiState,
                onAction = { action ->
                    when (action) {
                        NewFriendsAction.NavigateBack -> onRouteChanged(AppRoute.Contacts)
                        is NewFriendsAction.Accept -> {
                            coroutineScope.launch {
                                val result = newFriendsCoordinator.accept(
                                    requestId = action.requestId,
                                    session = appSessionState,
                                    currentSnapshot = friendRequestsSnapshot,
                                    currentConversationState = conversationThreadsState,
                                )
                                applyFriendRequestStatus(result.status)
                                result.friends?.let(onFriendsChanged)
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
        } }
        composable(AppRoute.CallSession.name) { AuthenticatedShell(
            currentDestination = currentRoute.toShellDestination(),
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
            CallSessionScreen(
                state = callSessionUiState,
                onAction = { action ->
                    val nextCallSessionState = reduceCallSessionState(callSessionState, action)
                    onCallSessionStateChanged(nextCallSessionState)
                    if (action == CallSessionAction.EndCall) {
                        callSessionRuntime.endCall(
                            currentState = nextCallSessionState,
                            setState = onCallSessionStateChanged,
                            openChat = { onRouteChanged(AppRoute.Chat) },
                        )
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        } }

        composable(AppRoute.Wallet.name) { AuthenticatedShell(
            currentDestination = currentRoute.toShellDestination(),
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
            WalletScreen(
                state = walletUiState,
                permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
                onAction = { action ->
                    val result = reduceWalletState(
                        currentState = walletStateModel,
                        action = action,
                    )
                    onWalletStateChanged(result.state)

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
        } }

        composable(AppRoute.Me.name) { AuthenticatedShell(
            currentDestination = currentRoute.toShellDestination(),
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
            MeScreen(
                state = meUiState,
                onSignOut = ::signOutToEntry,
                modifier = Modifier.padding(innerPadding),
            )
        } }

        composable(AppRoute.Chat.name) {
            val resolvedChatUiState = chatUiState
            val resolvedChatState = chatState

            if (resolvedChatUiState == null || resolvedChatState == null) {
                AuthenticatedShell(
                    currentDestination = ShellDestination.Inbox,
                    onTabSelected = ::openShellDestination,
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
                    currentDestination = currentRoute.toShellDestination(),
                    onTabSelected = ::openShellDestination,
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
                                    callSessionRuntime.startCall(
                                        conversationId = effect.conversationId,
                                        contactDisplayName = effect.contactDisplayName,
                                        mode = if (effect.mode == ChatCallMode.Video) {
                                            CallSessionMode.Video
                                        } else {
                                            CallSessionMode.Audio
                                        },
                                        setState = onCallSessionStateChanged,
                                        openCallSession = { onRouteChanged(AppRoute.CallSession) },
                                        shouldKeepTicking = { latestCurrentRoute == AppRoute.CallSession },
                                    )
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
                                        val summary = dispatchResult.summary
                                        if (summary?.failureMessage != null) {
                                            onChatStatusChanged(summary.failureMessage, true)
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
                                    onChatStatusChanged(downloadResult.message, downloadResult.isError)
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

private fun AppRoute.toShellDestination(): ShellDestination {
    return when (this) {
        AppRoute.Inbox -> ShellDestination.Inbox
        AppRoute.Contacts -> ShellDestination.Contacts
        AppRoute.Wallet -> ShellDestination.Wallet
        AppRoute.Me -> ShellDestination.Me
        AppRoute.AuthEntry,
        AppRoute.RegisterEntry,
        AppRoute.NewFriends,
        AppRoute.CallSession,
        AppRoute.Chat,
        -> ShellDestination.Secondary
    }
}






