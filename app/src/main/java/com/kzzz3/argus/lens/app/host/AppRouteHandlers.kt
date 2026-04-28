package com.kzzz3.argus.lens.app.host

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kzzz3.argus.lens.app.composition.AppDependencies
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigator
import com.kzzz3.argus.lens.app.runtime.PersistAppStateUseCase
import com.kzzz3.argus.lens.app.runtime.RestoreAppSessionUseCase
import com.kzzz3.argus.lens.feature.call.CallSessionFeatureController
import com.kzzz3.argus.lens.feature.call.CallSessionRouteHandler
import com.kzzz3.argus.lens.feature.call.CallSessionTimer
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureController
import com.kzzz3.argus.lens.feature.contacts.ContactsOpenConversationResult
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteHandler
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadHandler
import com.kzzz3.argus.lens.feature.inbox.ChatRouteHandler
import com.kzzz3.argus.lens.feature.inbox.InboxRouteHandler
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionManager
import com.kzzz3.argus.lens.feature.realtime.RealtimeReconnectScheduler
import com.kzzz3.argus.lens.session.SessionRefreshScheduler
import kotlinx.coroutines.CoroutineScope

internal data class AppRouteHandlers(
    val callSessionTimer: CallSessionTimer,
    val callSessionFeatureController: CallSessionFeatureController,
    val realtimeReconnectScheduler: RealtimeReconnectScheduler,
    val sessionRefreshScheduler: SessionRefreshScheduler,
    val contactsFeatureController: ContactsFeatureController,
    val chatRouteHandler: ChatRouteHandler,
    val inboxRouteHandler: InboxRouteHandler,
    val realtimeConnectionManager: RealtimeConnectionManager,
    val persistAppStateUseCase: PersistAppStateUseCase,
    val restoreAppSessionUseCase: RestoreAppSessionUseCase,
    val contactsRouteLoadHandler: ContactsRouteLoadHandler,
    val appRouteNavigator: AppRouteNavigator,
)

@Composable
internal fun rememberAppRouteHandlers(
    dependencies: AppDependencies,
    coroutineScope: CoroutineScope,
): AppRouteHandlers {
    val appShellUseCases = dependencies.appShellUseCases
    val appSessionRefresher = dependencies.appSessionRefresher
    val friendRequestUseCases = dependencies.friendRequestUseCases
    val contactsUseCases = dependencies.contactsUseCases
    val chatUseCases = dependencies.chatUseCases
    val applyRealtimeConversationEventUseCase = dependencies.applyRealtimeConversationEventUseCase
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val realtimeClient = dependencies.realtimeClient

    val callSessionTimer = remember(coroutineScope) { CallSessionTimer(coroutineScope) }
    val callSessionRouteHandler = remember(callSessionTimer) {
        CallSessionRouteHandler(
            reduceAction = ::reduceCallSessionState,
            endCall = callSessionTimer::endCall,
        )
    }
    val callSessionFeatureController = remember(callSessionRouteHandler) {
        CallSessionFeatureController(callSessionRouteHandler)
    }
    val realtimeReconnectScheduler = remember(coroutineScope) { RealtimeReconnectScheduler(coroutineScope) }
    val sessionRefreshScheduler = remember(coroutineScope, appSessionRefresher, sessionCredentialsStore) {
        SessionRefreshScheduler(
            scope = coroutineScope,
            sessionRefresher = appSessionRefresher,
            credentialsStore = sessionCredentialsStore,
        )
    }
    val contactsRouteHandler = remember(coroutineScope, contactsUseCases, friendRequestUseCases) {
        ContactsRouteHandler(
            scope = coroutineScope,
            openConversation = { request, conversationId ->
                val result = contactsUseCases.openConversation(
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
            addFriend = contactsUseCases::addFriend,
            acceptFriendRequest = friendRequestUseCases::accept,
            rejectFriendRequest = friendRequestUseCases::reject,
            ignoreFriendRequest = friendRequestUseCases::ignore,
        )
    }
    val chatRouteHandler = remember(coroutineScope, chatUseCases, callSessionTimer) {
        ChatRouteHandler(
            scope = coroutineScope,
            reduceAction = chatUseCases::reduceAction,
            startCall = callSessionTimer::startCall,
            dispatchOutgoingMessages = chatUseCases::dispatchOutgoingMessages,
            downloadAttachment = chatUseCases::downloadAttachment,
            recallMessage = chatUseCases::recallMessage,
        )
    }
    val contactsFeatureController = remember(contactsRouteHandler) {
        ContactsFeatureController(contactsRouteHandler)
    }
    val inboxRouteHandler = remember(coroutineScope, chatUseCases) {
        InboxRouteHandler(
            scope = coroutineScope,
            openConversation = chatUseCases::openConversation,
            synchronizeConversation = chatUseCases::synchronizeConversation,
        )
    }
    val realtimeConnectionManager = remember(coroutineScope, realtimeClient, applyRealtimeConversationEventUseCase, realtimeReconnectScheduler) {
        RealtimeConnectionManager(
            scope = coroutineScope,
            realtimeClient = realtimeClient,
            applyRealtimeConversationEvent = applyRealtimeConversationEventUseCase,
            reconnectScheduler = realtimeReconnectScheduler,
        )
    }
    val persistAppStateUseCase = remember(appShellUseCases) {
        PersistAppStateUseCase(appShellUseCases)
    }
    val restoreAppSessionUseCase = remember(appShellUseCases) {
        RestoreAppSessionUseCase(
            loadInitialAuthenticatedConversations = appShellUseCases::loadInitialAuthenticatedConversations,
            hydrateAppState = appShellUseCases::hydrateAppState,
        )
    }
    val contactsRouteLoadHandler = remember(contactsUseCases, friendRequestUseCases) {
        ContactsRouteLoadHandler(
            loadFriends = contactsUseCases::loadFriends,
            loadRequests = friendRequestUseCases::loadRequests,
        )
    }
    val appRouteNavigator = remember { AppRouteNavigator() }

    return AppRouteHandlers(
        callSessionTimer = callSessionTimer,
        callSessionFeatureController = callSessionFeatureController,
        realtimeReconnectScheduler = realtimeReconnectScheduler,
        sessionRefreshScheduler = sessionRefreshScheduler,
        contactsFeatureController = contactsFeatureController,
        chatRouteHandler = chatRouteHandler,
        inboxRouteHandler = inboxRouteHandler,
        realtimeConnectionManager = realtimeConnectionManager,
        persistAppStateUseCase = persistAppStateUseCase,
        restoreAppSessionUseCase = restoreAppSessionUseCase,
        contactsRouteLoadHandler = contactsRouteLoadHandler,
        appRouteNavigator = appRouteNavigator,
    )
}
