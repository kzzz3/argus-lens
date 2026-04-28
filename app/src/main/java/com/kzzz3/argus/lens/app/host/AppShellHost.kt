package com.kzzz3.argus.lens.app.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kzzz3.argus.lens.app.composition.AppDependencies
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.runtime.AppSessionBoundaryCallbacks
import com.kzzz3.argus.lens.app.runtime.SessionBoundaryHandler
import com.kzzz3.argus.lens.app.state.DEFAULT_PREVIEW_DISPLAY_NAME
import com.kzzz3.argus.lens.feature.auth.navigation.AuthRoutes
import com.kzzz3.argus.lens.feature.call.navigation.CallSessionRoutes
import com.kzzz3.argus.lens.feature.contacts.navigation.ContactsRoutes
import com.kzzz3.argus.lens.feature.inbox.navigation.InboxRoutes
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureController
import com.kzzz3.argus.lens.feature.me.navigation.MeRoutes
import com.kzzz3.argus.lens.feature.wallet.navigation.WalletRoutes
import com.kzzz3.argus.lens.navigation.ArgusNavRoutes
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.navigation.ArgusNavHost
import com.kzzz3.argus.lens.navigation.MainRoutes
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun AppShellHost(
    dependencies: AppDependencies,
    appScope: CoroutineScope,
    featureStateHolders: AppFeatureStateHolders,
    state: AppShellState,
    callbacks: AppShellCallbacks,
) {
    val appSessionState = state.appSessionState
    val currentRoute = state.currentRoute
    val activeChatConversationId = state.activeChatConversationId
    val realtimeConnectionState = state.realtimeConnectionState
    val onHydratedSessionApplied = callbacks.onHydratedSessionApplied
    val onAuthenticatedSessionApplied = callbacks.onAuthenticatedSessionApplied
    val onSessionRefreshed = callbacks.onSessionRefreshed
    val onSessionCleared = callbacks.onSessionCleared
    val onHydratedConversationAccountChanged = callbacks.onHydratedConversationAccountChanged
    val authStateHolder = featureStateHolders.authStateHolder
    val contactsFeatureStateHolder = featureStateHolders.contactsFeatureStateHolder
    val callSessionStateHolder = featureStateHolders.callSessionStateHolder
    val inboxChatFeatureStateHolder = featureStateHolders.inboxChatFeatureStateHolder
    val chatStateHolder = featureStateHolders.chatStateHolder
    val inboxStateHolder = featureStateHolders.inboxStateHolder
    val walletStateHolder = featureStateHolders.walletStateHolder
    val navController = rememberNavController()
    val routeHandlers = rememberAppRouteHandlers(dependencies, appScope)
    val appShellUseCases = dependencies.appShellUseCases
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val callSessionTimer = routeHandlers.callSessionTimer
    val sessionRefreshScheduler = routeHandlers.sessionRefreshScheduler
    val authStateModel by authStateHolder.state.collectAsStateWithLifecycle()
    val contactsFeatureState by contactsFeatureStateHolder.state.collectAsStateWithLifecycle()
    val callSessionState by callSessionStateHolder.state.collectAsStateWithLifecycle()
    val inboxChatFeatureState by inboxChatFeatureStateHolder.state.collectAsStateWithLifecycle()
    val chatStateModel by chatStateHolder.state.collectAsStateWithLifecycle()
    val inboxStateModel by inboxStateHolder.state.collectAsStateWithLifecycle()
    val walletStateModel by walletStateHolder.state.collectAsStateWithLifecycle()
    val conversationThreadsState = inboxChatFeatureState.conversationThreadsState
    val appRouteNavigator = routeHandlers.appRouteNavigator
    val previewThreadsState = remember {
        appShellUseCases.createPreviewConversationThreads(
            currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME,
        )
    }
    val startDestination = remember { appRouteNavigator.buildGraphRoute(currentRoute) }
    val routeUiState = rememberAppShellUiState(
        appSessionState = appSessionState,
        conversationThreadsState = conversationThreadsState,
        realtimeConnectionState = realtimeConnectionState,
        inboxUiState = inboxStateModel.uiState,
        chatState = chatStateModel.chatState,
        chatUiState = chatStateModel.uiState,
        authFormState = authStateModel.authFormState,
        registerFormState = authStateModel.registerFormState,
        callSessionState = callSessionState,
        contactsState = contactsFeatureState.contactsState,
        walletStateModel = walletStateModel,
        friends = contactsFeatureState.friends,
        chatStatusMessage = inboxChatFeatureState.chatStatusMessage,
        chatStatusError = inboxChatFeatureState.chatStatusError,
        friendRequestsSnapshot = contactsFeatureState.friendRequestsSnapshot,
        friendRequestsStatusMessage = contactsFeatureState.friendRequestsStatusMessage,
        friendRequestsStatusError = contactsFeatureState.friendRequestsStatusError,
    )
    val latestConversationThreadsState by rememberUpdatedState(conversationThreadsState)
    val latestActiveChatConversationId by rememberUpdatedState(activeChatConversationId)
    val latestAppSessionState by rememberUpdatedState(appSessionState)
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestRealtimeConnectionState by rememberUpdatedState(realtimeConnectionState)
    val latestRealtimeEnabled by rememberUpdatedState(
        appSessionState.isAuthenticated && sessionCredentialsStore.current.hasAccessToken
    )
    val featureCallbacks = featureStateHolders.asFeatureCallbacks()
    val sessionBoundaryHandler = SessionBoundaryHandler(
        appShellUseCases = appShellUseCases,
        refreshSessionOnce = { session, setSession ->
            sessionRefreshScheduler.refreshOnce(
                session = session,
                setSession = setSession,
            )
        },
        startSessionRefreshLoop = { onUnauthorized ->
            sessionRefreshScheduler.startLoopIfNeeded(
                getSession = { latestAppSessionState },
                isRefreshLoopActive = { latestRealtimeConnectionState == ConversationRealtimeConnectionState.LIVE },
                setSession = onSessionRefreshed,
                onUnauthorized = onUnauthorized,
            )
        },
        cancelSessionRefreshLoop = sessionRefreshScheduler::cancel,
        invalidateWalletRequests = walletStateHolder::invalidate,
        cancelCallSession = callSessionTimer::cancel,
    )
    val sessionBoundaryCallbacks = AppSessionBoundaryCallbacks(
        onHydratedConversationAccountChanged = onHydratedConversationAccountChanged,
        onCallSessionStateChanged = featureCallbacks.onCallSessionStateChanged,
        onWalletStateChanged = walletStateHolder::replaceState,
        onConversationThreadsChanged = featureCallbacks.onConversationThreadsChanged,
        onAuthenticatedSessionApplied = onAuthenticatedSessionApplied,
        onAuthFormStateChanged = authStateHolder::replaceAuthFormState,
        onSessionCleared = onSessionCleared,
        onRegisterFormStateChanged = authStateHolder::replaceRegisterFormState,
        onContactsStateChanged = featureCallbacks.onContactsStateChanged,
        onFriendsChanged = featureCallbacks.onFriendsChanged,
        onFriendRequestStatusReset = featureCallbacks.onFriendRequestStatusReset,
    )
    val inboxChatFeatureController = remember(
        inboxStateHolder,
        routeHandlers.inboxRouteHandler,
        routeHandlers.chatRouteHandler,
    ) {
        InboxChatFeatureController(
            inboxStateHolder = inboxStateHolder,
            inboxRouteHandler = routeHandlers.inboxRouteHandler,
            chatRouteHandler = routeHandlers.chatRouteHandler,
        )
    }
    val actionDispatcher = AppActionDispatcher(
        state = state,
        contactsFeatureState = contactsFeatureState,
        callSessionState = callSessionState,
        inboxChatFeatureState = inboxChatFeatureState,
        authStateHolder = authStateHolder,
        inboxChatFeatureController = inboxChatFeatureController,
        walletStateHolder = walletStateHolder,
        callbacks = callbacks,
        featureCallbacks = featureCallbacks,
        routeUiState = routeUiState,
        routeHandlers = routeHandlers,
        previewThreadsState = previewThreadsState,
        sessionBoundaryHandler = sessionBoundaryHandler,
        sessionBoundaryCallbacks = sessionBoundaryCallbacks,
        getLatestCurrentRoute = { latestCurrentRoute },
    )
    AppShellEffects(
        navController = navController,
        effectDependencies = AppShellEffectDependencies(
            initialSessionSnapshot = dependencies.initialSessionSnapshot,
            initialSessionCredentials = dependencies.initialSessionCredentials,
            sessionCredentialsStore = dependencies.sessionCredentialsStore,
            realtimeClient = dependencies.realtimeClient,
        ),
        state = state,
        contactsFeatureState = contactsFeatureState,
        inboxChatFeatureState = inboxChatFeatureState,
        callbacks = callbacks,
        featureCallbacks = featureCallbacks,
        routeHandlers = routeHandlers,
        chatStateHolder = chatStateHolder,
        inboxStateHolder = inboxStateHolder,
        previewThreadsState = previewThreadsState,
        sessionBoundaryHandler = sessionBoundaryHandler,
        sessionBoundaryCallbacks = sessionBoundaryCallbacks,
        actionDispatcher = actionDispatcher,
        getLatestConversationThreadsState = { latestConversationThreadsState },
        getLatestActiveChatConversationId = { latestActiveChatConversationId },
        getLatestAppSessionState = { latestAppSessionState },
        getLatestCurrentRoute = { latestCurrentRoute },
        isRealtimeEnabled = { latestRealtimeEnabled },
    )

    ArgusNavHost(
        navController = navController,
        startDestination = startDestination,
        routes = ArgusNavRoutes(
            auth = AuthRoutes(
                authState = routeUiState.authState,
                registerState = routeUiState.registerState,
                onAuthAction = actionDispatcher::handleAuthAction,
                onRegisterAction = actionDispatcher::handleRegisterAction,
            ),
            main = MainRoutes(
                inbox = InboxRoutes(
                    inboxShellDestination = appRouteNavigator.toShellDestination(AppRoute.Inbox),
                    chatShellDestination = appRouteNavigator.toShellDestination(currentRoute),
                    missingChatShellDestination = appRouteNavigator.resolveRouteShellDestination(
                        route = AppRoute.Chat,
                        hasActiveChatConversation = false,
                    ),
                    onTabSelected = actionDispatcher::openShellDestination,
                    inboxState = routeUiState.inboxState,
                    chatState = routeUiState.chatState,
                    chatUiState = routeUiState.chatUiState,
                    onInboxAction = actionDispatcher::handleInboxAction,
                    onChatAction = actionDispatcher::handleChatAction,
                ),
                contacts = ContactsRoutes(
                    contactsShellDestination = appRouteNavigator.toShellDestination(AppRoute.Contacts),
                    newFriendsShellDestination = appRouteNavigator.resolveRouteShellDestination(AppRoute.NewFriends),
                    onTabSelected = actionDispatcher::openShellDestination,
                    contactsState = routeUiState.contactsUiState,
                    newFriendsState = routeUiState.newFriendsUiState,
                    onContactsAction = actionDispatcher::handleContactsAction,
                    onNewFriendsAction = actionDispatcher::handleNewFriendsAction,
                ),
                call = CallSessionRoutes(
                    callShellDestination = appRouteNavigator.toShellDestination(currentRoute),
                    onTabSelected = actionDispatcher::openShellDestination,
                    callSessionState = routeUiState.callSessionUiState,
                    onCallSessionAction = actionDispatcher::handleCallSessionAction,
                ),
                wallet = WalletRoutes(
                    walletShellDestination = appRouteNavigator.toShellDestination(AppRoute.Wallet),
                    onTabSelected = actionDispatcher::openShellDestination,
                    walletState = routeUiState.walletUiState,
                    permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
                    onWalletAction = actionDispatcher::handleWalletAction,
                ),
                me = MeRoutes(
                    meShellDestination = appRouteNavigator.toShellDestination(AppRoute.Me),
                    onTabSelected = actionDispatcher::openShellDestination,
                    meState = routeUiState.meUiState,
                    onSignOut = { actionDispatcher.signOutToEntry() },
                ),
            ),
        ),
    )
}
