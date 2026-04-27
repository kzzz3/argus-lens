package com.kzzz3.argus.lens.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
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
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.realtime.buildRealtimeStatusLabel
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import com.kzzz3.argus.lens.ui.shell.ShellDestination

@Composable
internal fun AppRouteHost(
    dependencies: AppDependencies,
    appSessionState: AppSessionState,
    conversationThreadsState: ConversationThreadsState,
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
    onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
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
    val callSessionRouteRuntime = remember(callSessionRuntime) {
        CallSessionRouteRuntime(
            reduceAction = ::reduceCallSessionState,
            endCall = callSessionRuntime::endCall,
        )
    }
    val realtimeReconnectRuntime = remember(coroutineScope) { RealtimeReconnectRuntime(coroutineScope) }
    val sessionRefreshRuntime = remember(coroutineScope, appSessionCoordinator, sessionCredentialsStore) {
        SessionRefreshRuntime(
            scope = coroutineScope,
            appSessionCoordinator = appSessionCoordinator,
            credentialsStore = sessionCredentialsStore,
        )
    }
    val walletRequestRuntime = remember(coroutineScope) { WalletRequestRuntime(coroutineScope) }
    val contactsRouteRuntime = remember(coroutineScope, contactsCoordinator, newFriendsCoordinator) {
        ContactsRouteRuntime(
            scope = coroutineScope,
            openConversation = { request, conversationId ->
                val result = contactsCoordinator.openConversation(
                    session = request.session,
                    requestedConversationId = conversationId,
                    friends = request.friends,
                    state = request.conversationThreadsState,
                )
                ContactsOpenConversationResult(
                    conversationThreadsState = result.conversationThreadsState,
                    conversationId = result.conversationId,
                )
            },
            addFriend = contactsCoordinator::addFriend,
            acceptFriendRequest = newFriendsCoordinator::accept,
            rejectFriendRequest = newFriendsCoordinator::reject,
            ignoreFriendRequest = newFriendsCoordinator::ignore,
        )
    }
    val chatRouteRuntime = remember(coroutineScope, chatCoordinator, callSessionRuntime) {
        ChatRouteRuntime(
            scope = coroutineScope,
            reduceAction = chatCoordinator::reduceAction,
            startCall = callSessionRuntime::startCall,
            dispatchOutgoingMessages = chatCoordinator::dispatchOutgoingMessages,
            downloadAttachment = chatCoordinator::downloadAttachment,
            recallMessage = chatCoordinator::recallMessage,
        )
    }
    val inboxRouteRuntime = remember(coroutineScope, chatCoordinator) {
        InboxRouteRuntime(
            scope = coroutineScope,
            openConversation = chatCoordinator::openConversation,
            synchronizeConversation = chatCoordinator::synchronizeConversation,
        )
    }
    val entryRouteRuntime = remember(coroutineScope, authCoordinator) {
        EntryRouteRuntime(
            scope = coroutineScope,
            reduceAuthAction = ::reduceAuthFormState,
            reduceRegisterAction = ::reduceRegisterFormState,
            login = authCoordinator::login,
            register = authCoordinator::register,
        )
    }
    val walletRouteRuntime = remember(walletRequestRuntime, walletRequestCoordinator) {
        WalletRouteRuntime(
            requestRuntime = walletRequestRuntime,
            loadWalletSummary = walletRequestCoordinator::loadWalletSummary,
            resolvePayload = walletRequestCoordinator::resolvePayload,
            confirmPayment = walletRequestCoordinator::confirmPayment,
            loadPaymentHistory = walletRequestCoordinator::loadPaymentHistory,
            loadPaymentReceipt = walletRequestCoordinator::loadPaymentReceipt,
        )
    }
    val realtimeConnectionRuntime = remember(coroutineScope, realtimeClient, realtimeCoordinator, realtimeReconnectRuntime) {
        RealtimeConnectionRuntime(
            scope = coroutineScope,
            realtimeClient = realtimeClient,
            realtimeCoordinator = realtimeCoordinator,
            reconnectRuntime = realtimeReconnectRuntime,
        )
    }
    val appPersistenceRuntime = remember(appShellCoordinator) {
        AppPersistenceRuntime(appShellCoordinator)
    }
    val appInitialHydrationRuntime = remember(appShellCoordinator) {
        AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = appShellCoordinator::loadInitialAuthenticatedConversations,
            hydrateAppState = appShellCoordinator::hydrateAppState,
        )
    }
    val appRouteLoadRuntime = remember(contactsCoordinator, newFriendsCoordinator) {
        AppRouteLoadRuntime(
            loadFriends = contactsCoordinator::loadFriends,
            loadRequests = newFriendsCoordinator::loadRequests,
        )
    }
    val appRouteNavigationRuntime = remember { AppRouteNavigationRuntime() }
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
    val latestRealtimeConnectionState by rememberUpdatedState(realtimeConnectionState)
    val latestRealtimeEnabled by rememberUpdatedState(
        appSessionState.isAuthenticated && sessionCredentialsStore.current.hasAccessToken
    )
    val sessionBoundaryRuntime = AppSessionBoundaryRuntime(
        appShellCoordinator = appShellCoordinator,
        refreshSessionOnce = { session, setSession ->
            sessionRefreshRuntime.refreshOnce(
                session = session,
                setSession = setSession,
            )
        },
        startSessionRefreshLoop = { onUnauthorized ->
            sessionRefreshRuntime.startLoopIfNeeded(
                getSession = { latestAppSessionState },
                getConnectionState = { latestRealtimeConnectionState },
                setSession = onSessionRefreshed,
                onUnauthorized = onUnauthorized,
            )
        },
        cancelSessionRefreshLoop = sessionRefreshRuntime::cancel,
        invalidateWalletRequests = walletRequestRuntime::invalidate,
        cancelCallSession = callSessionRuntime::cancel,
    )
    val sessionBoundaryCallbacks = AppSessionBoundaryCallbacks(
        onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
        onCallSessionStateChanged = onCallSessionStateChanged,
        onWalletStateChanged = onWalletStateChanged,
        onConversationThreadsChanged = onConversationThreadsChanged,
        onAuthenticatedSessionApplied = onAuthenticatedSessionApplied,
        onAuthFormStateChanged = onAuthFormStateChanged,
        onSessionCleared = onSessionCleared,
        onRegisterFormStateChanged = onRegisterFormStateChanged,
        onContactsStateChanged = onContactsStateChanged,
        onFriendsChanged = onFriendsChanged,
        onFriendRequestStatusReset = onFriendRequestStatusReset,
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
        appInitialHydrationRuntime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSessionSnapshot,
                initialCredentials = dependencies.initialSessionCredentials,
                previewThreadsState = previewThreadsState,
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = onConversationThreadsChanged,
                onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
                onRouteChanged = onRouteChanged,
                onHydratedSessionApplied = onHydratedSessionApplied,
            ),
        )
    }

    LaunchedEffect(appSessionState) {
        appPersistenceRuntime.persistSession(appSessionState, sessionCredentialsStore.current)
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
        appRouteNavigationRuntime.openTopLevelRoute(
            route = route,
            request = AppRouteNavigationRequest(
                accountId = appSessionState.accountId,
                walletState = walletStateModel,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = onWalletStateChanged,
                onRouteChanged = onRouteChanged,
            ),
        )
    }

    fun openShellDestination(destination: ShellDestination) {
        appRouteNavigationRuntime.openShellDestination(
            destination = destination,
            request = AppRouteNavigationRequest(
                accountId = appSessionState.accountId,
                walletState = walletStateModel,
            ),
            callbacks = AppRouteNavigationCallbacks(
                onWalletStateChanged = onWalletStateChanged,
                onRouteChanged = onRouteChanged,
            ),
        )
    }

    fun openInboxConversation(conversationId: String) {
        inboxRouteRuntime.openConversation(
            conversationId = conversationId,
            request = InboxRouteRequest(threadsState = conversationThreadsState),
            callbacks = InboxRouteCallbacks(
                onConversationThreadsChanged = onConversationThreadsChanged,
                onConversationOpened = onConversationOpened,
            ),
        )
    }

    suspend fun applySuccessfulAuthResult(
        authResult: AuthRepositoryResult.Success,
        keepSubmitMessageOnAuthForm: Boolean,
    ) {
        sessionBoundaryRuntime.applySuccessfulAuthResult(
            authResult = authResult,
            keepSubmitMessageOnAuthForm = keepSubmitMessageOnAuthForm,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    fun applyFriendRequestStatus(statusState: FriendRequestStatusState) {
        onFriendRequestStatusChanged(statusState)
    }

    fun entryRouteRequest(): EntryRouteRequest {
        return EntryRouteRequest(
            authFormState = authFormState,
            registerFormState = registerFormState,
        )
    }

    fun entryRouteCallbacks(): EntryRouteCallbacks {
        return EntryRouteCallbacks(
            onRouteChanged = onRouteChanged,
            onAuthFormStateChanged = onAuthFormStateChanged,
            onRegisterFormStateChanged = onRegisterFormStateChanged,
            applySuccessfulAuthResult = ::applySuccessfulAuthResult,
        )
    }

    fun contactsRouteRequest(contactsStateSnapshot: ContactsState = contactsState): ContactsRouteRequest {
        return ContactsRouteRequest(
            session = appSessionState,
            contactsState = contactsStateSnapshot,
            friends = friends,
            conversationThreadsState = conversationThreadsState,
            friendRequestsSnapshot = friendRequestsSnapshot,
        )
    }

    fun contactsRouteCallbacks(): ContactsRouteCallbacks {
        return ContactsRouteCallbacks(
            onRouteChanged = onRouteChanged,
            onContactsStateChanged = onContactsStateChanged,
            onFriendRequestsSnapshotChanged = onFriendRequestsSnapshotChanged,
            onConversationThreadsChanged = onConversationThreadsChanged,
            onConversationOpened = onConversationOpened,
            onFriendRequestStatusChanged = ::applyFriendRequestStatus,
            onFriendsChanged = onFriendsChanged,
        )
    }

    fun signOutToEntry(message: String? = null) {
        sessionBoundaryRuntime.signOutToEntry(
            currentSession = appSessionState,
            previewThreadsState = previewThreadsState,
            message = message,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    suspend fun refreshSessionTokens(): AuthRepositoryResult {
        return sessionBoundaryRuntime.refreshSessionTokens(
            session = appSessionState,
            setSession = onSessionRefreshed,
        )
    }

    fun scheduleSessionRefreshLoop() {
        sessionBoundaryRuntime.scheduleSessionRefreshLoop(
            onUnauthorized = { signOutToEntry("Session expired or was revoked. Please sign in again.") },
        )
    }

    fun realtimeConnectionCallbacks(): RealtimeConnectionCallbacks {
        return RealtimeConnectionCallbacks(
            onConnectionStateChanged = onRealtimeConnectionStateChanged,
            onEventIdRecorded = onRealtimeEventIdRecorded,
            onLastEventIdReset = onRealtimeLastEventIdReset,
            onConversationThreadsChanged = onConversationThreadsChanged,
            onReconnectGenerationIncremented = onRealtimeReconnectIncremented,
            onScheduleSessionRefreshLoop = { scheduleSessionRefreshLoop() },
            onCancelSessionRefreshLoop = sessionRefreshRuntime::cancel,
            refreshSessionTokens = { refreshSessionTokens() },
            signOutToEntry = { message -> signOutToEntry(message) },
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        sessionBoundaryRuntime.applySessionBoundary(
            session = appSessionState,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, realtimeReconnectGeneration) {
        realtimeConnectionRuntime.connect(
            request = RealtimeConnectionRequest(
                isAuthenticated = appSessionState.isAuthenticated,
                accountId = appSessionState.accountId,
                credentials = sessionCredentialsStore.current,
                lastEventId = realtimeLastEventId,
                reconnectGeneration = realtimeReconnectGeneration,
                isRealtimeEnabled = { latestRealtimeEnabled },
                getSession = { latestAppSessionState },
                getConversationThreadsState = { latestConversationThreadsState },
                getSelectedConversationId = { latestSelectedConversationId },
                getCurrentRoute = { latestCurrentRoute },
            ),
            callbacks = realtimeConnectionCallbacks(),
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            realtimeConnectionRuntime.dispose(realtimeConnectionCallbacks())
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        appPersistenceRuntime.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        appRouteLoadRuntime.loadForRoute(
            request = AppRouteLoadRequest(
                route = currentRoute,
                isAuthenticated = appSessionState.isAuthenticated,
                friendRequestsSnapshot = friendRequestsSnapshot,
            ),
            callbacks = AppRouteLoadCallbacks(
                onFriendsChanged = onFriendsChanged,
                onFriendRequestStatusChanged = ::applyFriendRequestStatus,
            ),
        )
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
                entryRouteRuntime.handleAuthAction(
                    action = action,
                    request = entryRouteRequest(),
                    callbacks = entryRouteCallbacks(),
                )
            }
            )
        }

        composable(AppRoute.RegisterEntry.name) {
            RegisterScreen(
            state = registerState,
            onAction = { action ->
                entryRouteRuntime.handleRegisterAction(
                    action = action,
                    request = entryRouteRequest(),
                    callbacks = entryRouteCallbacks(),
                )
            }
            )
        }

        composable(AppRoute.Inbox.name) { AuthenticatedShell(
            currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
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
            currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
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

                    contactsRouteRuntime.handleContactsEffect(
                        effect = result.effect,
                        request = contactsRouteRequest(),
                        callbacks = contactsRouteCallbacks(),
                    )
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
                    contactsRouteRuntime.handleNewFriendsAction(
                        action = action,
                        request = contactsRouteRequest(),
                        callbacks = contactsRouteCallbacks(),
                    )
                },
                modifier = Modifier.padding(innerPadding),
            )
        } }
        composable(AppRoute.CallSession.name) { AuthenticatedShell(
            currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
            onTabSelected = ::openShellDestination,
        ) { innerPadding ->
            CallSessionScreen(
                state = callSessionUiState,
                onAction = { action ->
                    callSessionRouteRuntime.handleAction(
                        action = action,
                        request = CallSessionRouteRequest(currentState = callSessionState),
                        callbacks = CallSessionRouteCallbacks(
                            onCallSessionStateChanged = onCallSessionStateChanged,
                            onRouteChanged = onRouteChanged,
                        ),
                    )
                },
                modifier = Modifier.padding(innerPadding),
            )
        } }

        composable(AppRoute.Wallet.name) { AuthenticatedShell(
            currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
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

                    walletRouteRuntime.handleEffect(
                        effect = result.effect,
                        request = WalletRouteRequest(
                            session = appSessionState,
                            currentState = result.state,
                        ),
                        callbacks = WalletRouteCallbacks(
                            getCurrentSession = { appSessionState },
                            getCurrentState = { walletStateModel },
                            onRouteChanged = ::openTopLevelRoute,
                            onStateChanged = onWalletStateChanged,
                        ),
                    )
                },
                modifier = Modifier.padding(innerPadding),
            )
        } }

        composable(AppRoute.Me.name) { AuthenticatedShell(
            currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
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
                    currentDestination = appRouteNavigationRuntime.toShellDestination(currentRoute),
                    onTabSelected = ::openShellDestination,
                ) { innerPadding ->
                    ChatScreen(
                        state = resolvedChatUiState,
                        onAction = { action ->
                            chatRouteRuntime.handleAction(
                                action = action,
                                request = ChatRouteRequest(
                                    threadsState = conversationThreadsState,
                                    chatState = resolvedChatState,
                                ),
                                callbacks = ChatRouteCallbacks(
                                    onRouteChanged = ::openTopLevelRoute,
                                    onConversationThreadsChanged = onConversationThreadsChanged,
                                    onChatStatusChanged = onChatStatusChanged,
                                    onCallSessionStateChanged = onCallSessionStateChanged,
                                    getCurrentRoute = { latestCurrentRoute },
                                ),
                            )
                        },                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}






