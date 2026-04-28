package com.kzzz3.argus.lens.app.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.runtime.AppSessionBoundaryCallbacks
import com.kzzz3.argus.lens.app.runtime.RestoreAppSessionCallbacks
import com.kzzz3.argus.lens.app.runtime.RestoreAppSessionRequest
import com.kzzz3.argus.lens.app.runtime.SessionBoundaryHandler
import com.kzzz3.argus.lens.app.state.resolvePreviewDisplayName
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionCredentialsStore
import com.kzzz3.argus.lens.feature.auth.navigation.AuthGraphRoutePattern
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureState
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadCallbacks
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadRequest
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadTarget
import com.kzzz3.argus.lens.feature.inbox.ChatStateHolder
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatThreadRoute
import com.kzzz3.argus.lens.feature.inbox.navigation.ChatThreadRoutePattern
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureState
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.realtime.buildRealtimeStatusLabel
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionRequest
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.navigation.MainGraphRoutePattern

internal data class AppShellEffectDependencies(
    val initialSessionSnapshot: AppSessionState,
    val initialSessionCredentials: SessionCredentials,
    val sessionCredentialsStore: SessionCredentialsStore,
    val realtimeClient: ConversationRealtimeClient,
)

@Composable
internal fun AppShellEffects(
    navController: NavHostController,
    effectDependencies: AppShellEffectDependencies,
    state: AppShellState,
    contactsFeatureState: ContactsFeatureState,
    inboxChatFeatureState: InboxChatFeatureState,
    callbacks: AppShellCallbacks,
    featureCallbacks: AppFeatureCallbacks,
    routeHandlers: AppRouteHandlers,
    chatStateHolder: ChatStateHolder,
    inboxStateHolder: InboxStateHolder,
    previewThreadsState: ConversationThreadsState,
    sessionBoundaryHandler: SessionBoundaryHandler,
    sessionBoundaryCallbacks: AppSessionBoundaryCallbacks,
    actionDispatcher: AppActionDispatcher,
    getLatestConversationThreadsState: () -> ConversationThreadsState,
    getLatestActiveChatConversationId: () -> String,
    getLatestAppSessionState: () -> AppSessionState,
    getLatestCurrentRoute: () -> AppRoute,
    isRealtimeEnabled: () -> Boolean,
) {
    val appSessionState = state.appSessionState
    val conversationThreadsState = inboxChatFeatureState.conversationThreadsState
    val currentRoute = state.currentRoute
    val activeChatConversationId = state.activeChatConversationId
    val chatStatusMessage = inboxChatFeatureState.chatStatusMessage
    val chatStatusError = inboxChatFeatureState.chatStatusError
    val realtimeConnectionState = state.realtimeConnectionState
    val friendRequestsSnapshot = contactsFeatureState.friendRequestsSnapshot
    val hydratedConversationAccountId = state.hydratedConversationAccountId
    val realtimeLastEventId = state.realtimeLastEventId
    val realtimeReconnectGeneration = state.realtimeReconnectGeneration
    val initialSessionSnapshot = effectDependencies.initialSessionSnapshot
    val sessionCredentialsStore = effectDependencies.sessionCredentialsStore
    val realtimeClient = effectDependencies.realtimeClient

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId) {
        routeHandlers.restoreAppSessionUseCase.hydrate(
            request = RestoreAppSessionRequest(
                initialSession = initialSessionSnapshot,
                initialCredentials = effectDependencies.initialSessionCredentials,
                previewThreadsState = previewThreadsState,
                restorableEntryContext = state.restorableEntryContext,
            ),
            callbacks = RestoreAppSessionCallbacks(
                onConversationThreadsChanged = featureCallbacks.onConversationThreadsChanged,
                onHydratedConversationAccountChanged = callbacks.onHydratedConversationAccountChanged,
                onRouteChanged = callbacks.onRouteChanged,
                onHydratedSessionApplied = callbacks.onHydratedSessionApplied,
                onActiveChatConversationChanged = callbacks.onActiveChatConversationChanged,
                onRestorableEntryContextCleared = callbacks.onRestorableEntryContextCleared,
            ),
        )
    }

    LaunchedEffect(appSessionState) {
        routeHandlers.persistAppStateUseCase.persistSession(appSessionState, sessionCredentialsStore.current)
    }

    LaunchedEffect(activeChatConversationId) {
        featureCallbacks.onChatStatusCleared()
    }

    LaunchedEffect(
        appSessionState.displayName,
        conversationThreadsState,
        activeChatConversationId,
        chatStatusMessage,
        chatStatusError,
    ) {
        chatStateHolder.replaceInputs(
            currentUserDisplayName = resolvePreviewDisplayName(appSessionState.displayName),
            threadsState = conversationThreadsState,
            activeChatConversationId = activeChatConversationId,
            statusMessage = chatStatusMessage,
            isStatusError = chatStatusError,
        )
    }

    LaunchedEffect(appSessionState, conversationThreadsState, realtimeConnectionState) {
        inboxStateHolder.replaceInputs(
            sessionState = appSessionState,
            threadsState = conversationThreadsState,
            realtimeStatusLabel = buildRealtimeStatusLabel(realtimeConnectionState),
            shellStatusLabel = resolveShellStatusLabel(appSessionState, realtimeConnectionState),
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        sessionBoundaryHandler.applySessionBoundary(
            session = appSessionState,
            callbacks = sessionBoundaryCallbacks,
        )
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, realtimeReconnectGeneration) {
        routeHandlers.realtimeConnectionManager.connect(
            request = RealtimeConnectionRequest(
                isAuthenticated = appSessionState.isAuthenticated,
                accountId = appSessionState.accountId,
                credentials = sessionCredentialsStore.current,
                lastEventId = realtimeLastEventId,
                reconnectGeneration = realtimeReconnectGeneration,
                isRealtimeEnabled = isRealtimeEnabled,
                getSession = getLatestAppSessionState,
                getConversationThreadsState = getLatestConversationThreadsState,
                getActiveChatConversationId = getLatestActiveChatConversationId,
                isChatRouteActive = { getLatestCurrentRoute() == AppRoute.Chat },
            ),
            callbacks = actionDispatcher.realtimeConnectionCallbacks(),
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            routeHandlers.realtimeConnectionManager.dispose(actionDispatcher.realtimeConnectionCallbacks())
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        routeHandlers.persistAppStateUseCase.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        routeHandlers.contactsRouteLoadHandler.loadForTarget(
            request = ContactsRouteLoadRequest(
                target = currentRoute.contactsRouteLoadTarget,
                isAuthenticated = appSessionState.isAuthenticated,
                friendRequestsSnapshot = friendRequestsSnapshot,
            ),
            callbacks = ContactsRouteLoadCallbacks(
                onFriendsChanged = featureCallbacks.onFriendsChanged,
                onFriendRequestStatusChanged = actionDispatcher::applyFriendRequestStatus,
            ),
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestinationRoute = currentBackStackEntry?.destination?.route
    val currentNavigationRouteKey = if (currentDestinationRoute == ChatThreadRoutePattern) {
        currentBackStackEntry
            ?.toRoute<ChatThreadRoute>()
            ?.conversationId
            ?.let(routeHandlers.appRouteNavigator::chatThreadRouteKey)
    } else {
        currentDestinationRoute
    }
    val previousGraphRoute = currentBackStackEntry
        ?.destination
        ?.hierarchy
        ?.firstOrNull { destination -> destination.route == AuthGraphRoutePattern || destination.route == MainGraphRoutePattern }
        ?.route
    val targetNavigation = routeHandlers.appRouteNavigator.buildNavigationTarget(
        route = currentRoute,
        activeChatConversationId = activeChatConversationId,
    )
    LaunchedEffect(currentRoute, targetNavigation, currentNavigationRouteKey, previousGraphRoute) {
        if (currentNavigationRouteKey != targetNavigation.routeKey) {
            navController.navigate(targetNavigation.route) {
                launchSingleTop = true
                if (previousGraphRoute != null && previousGraphRoute != targetNavigation.graphRoute) {
                    popUpTo(previousGraphRoute) {
                        inclusive = true
                    }
                }
            }
        }
    }
}

private val AppRoute.contactsRouteLoadTarget: ContactsRouteLoadTarget
    get() = when (this) {
        AppRoute.Contacts -> ContactsRouteLoadTarget.Contacts
        AppRoute.NewFriends -> ContactsRouteLoadTarget.NewFriends
        else -> ContactsRouteLoadTarget.None
    }
