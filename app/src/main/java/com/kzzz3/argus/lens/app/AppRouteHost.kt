package com.kzzz3.argus.lens.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.reduceContactsState
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppRouteHost(
    dependencies: AppDependencies,
    runtimeScope: CoroutineScope,
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
) {
    val navController = rememberNavController()
    val routeRuntimes = rememberAppRouteRuntimes(dependencies, runtimeScope)
    val appShellCoordinator = dependencies.appShellCoordinator
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val realtimeClient = dependencies.realtimeClient
    val callSessionRuntime = routeRuntimes.callSessionRuntime
    val callSessionRouteRuntime = routeRuntimes.callSessionRouteRuntime
    val sessionRefreshRuntime = routeRuntimes.sessionRefreshRuntime
    val walletRequestRuntime = routeRuntimes.walletRequestRuntime
    val contactsRouteRuntime = routeRuntimes.contactsRouteRuntime
    val chatRouteRuntime = routeRuntimes.chatRouteRuntime
    val inboxRouteRuntime = routeRuntimes.inboxRouteRuntime
    val inboxActionRouteRuntime = routeRuntimes.inboxActionRouteRuntime
    val entryRouteRuntime = routeRuntimes.entryRouteRuntime
    val walletRouteRuntime = routeRuntimes.walletRouteRuntime
    val realtimeConnectionRuntime = routeRuntimes.realtimeConnectionRuntime
    val appPersistenceRuntime = routeRuntimes.appPersistenceRuntime
    val appInitialHydrationRuntime = routeRuntimes.appInitialHydrationRuntime
    val appRouteLoadRuntime = routeRuntimes.appRouteLoadRuntime
    val appRouteNavigationRuntime = routeRuntimes.appRouteNavigationRuntime
    val previewThreadsState = remember {
        appShellCoordinator.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val initialSessionSnapshot = dependencies.initialSessionSnapshot
    val startDestination = remember { currentRoute.name }
    val routeUiState = rememberAppRouteUiState(
        appSessionState = appSessionState,
        conversationThreadsState = conversationThreadsState,
        realtimeConnectionState = realtimeConnectionState,
        authFormState = authFormState,
        registerFormState = registerFormState,
        callSessionState = callSessionState,
        contactsState = contactsState,
        walletStateModel = walletStateModel,
        friends = friends,
        selectedConversationId = selectedConversationId,
        chatStatusMessage = chatStatusMessage,
        chatStatusError = chatStatusError,
        friendRequestsSnapshot = friendRequestsSnapshot,
        friendRequestsStatusMessage = friendRequestsStatusMessage,
        friendRequestsStatusError = friendRequestsStatusError,
    )
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

    val contactsActionRouteRuntime = ContactsActionRouteRuntime(
        reduceAction = ::reduceContactsState,
        handleEffect = { effect ->
            contactsRouteRuntime.handleContactsEffect(
                effect = effect,
                request = contactsRouteRequest(),
                callbacks = contactsRouteCallbacks(),
            )
        },
    )

    val walletActionRouteRuntime = WalletActionRouteRuntime(
        reduceAction = ::reduceWalletState,
        handleEffect = { effect, currentState ->
            walletRouteRuntime.handleEffect(
                effect = effect,
                request = WalletRouteRequest(
                    session = appSessionState,
                    currentState = currentState,
                ),
                callbacks = WalletRouteCallbacks(
                    getCurrentSession = { appSessionState },
                    getCurrentState = { walletStateModel },
                    onRouteChanged = ::openTopLevelRoute,
                    onStateChanged = onWalletStateChanged,
                ),
            )
        },
    )

    fun signOutToEntry(message: String? = null) {
        sessionBoundaryRuntime.signOutToEntry(
            currentSession = appSessionState,
            previewThreadsState = previewThreadsState,
            message = message,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    fun inboxActionRouteCallbacks(): InboxActionRouteCallbacks {
        return InboxActionRouteCallbacks(
            openConversation = ::openInboxConversation,
            openTopLevelRoute = ::openTopLevelRoute,
            signOutToEntry = { signOutToEntry() },
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

    AppRouteNavGraph(
        navController = navController,
        startDestination = startDestination,
        currentRoute = currentRoute,
        routeUiState = routeUiState,
        walletStateModel = walletStateModel,
        appRouteNavigationRuntime = appRouteNavigationRuntime,
        onShellDestinationSelected = ::openShellDestination,
        onAuthAction = { action ->
            entryRouteRuntime.handleAuthAction(
                action = action,
                request = entryRouteRequest(),
                callbacks = entryRouteCallbacks(),
            )
        },
        onRegisterAction = { action ->
            entryRouteRuntime.handleRegisterAction(
                action = action,
                request = entryRouteRequest(),
                callbacks = entryRouteCallbacks(),
            )
        },
        onInboxAction = { action ->
            inboxActionRouteRuntime.handleAction(action, inboxActionRouteCallbacks())
        },
        onContactsAction = { action ->
            contactsActionRouteRuntime.handleAction(
                action = action,
                request = ContactsActionRouteRequest(currentState = contactsState),
                callbacks = ContactsActionRouteCallbacks(
                    onContactsStateChanged = onContactsStateChanged,
                ),
            )
        },
        onNewFriendsAction = { action ->
            contactsRouteRuntime.handleNewFriendsAction(
                action = action,
                request = contactsRouteRequest(),
                callbacks = contactsRouteCallbacks(),
            )
        },
        onCallSessionAction = { action ->
            callSessionRouteRuntime.handleAction(
                action = action,
                request = CallSessionRouteRequest(currentState = callSessionState),
                callbacks = CallSessionRouteCallbacks(
                    onCallSessionStateChanged = onCallSessionStateChanged,
                    onRouteChanged = onRouteChanged,
                ),
            )
        },
        onWalletAction = { action ->
            walletActionRouteRuntime.handleAction(
                action = action,
                request = WalletActionRouteRequest(currentState = walletStateModel),
                callbacks = WalletActionRouteCallbacks(
                    onWalletStateChanged = onWalletStateChanged,
                ),
            )
        },
        onSignOut = { signOutToEntry() },
        onChatAction = { action ->
            routeUiState.chatState?.let { resolvedChatState ->
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
            }
        },
    )
}
