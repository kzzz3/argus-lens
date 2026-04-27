package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionRuntime
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletRequestRunner
import kotlinx.coroutines.CoroutineScope

internal data class AppRouteRuntimes(
    val callSessionRuntime: CallSessionRuntime,
    val callSessionRouteRuntime: CallSessionRouteRuntime,
    val realtimeReconnectRuntime: RealtimeReconnectRuntime,
    val sessionRefreshRuntime: SessionRefreshRuntime,
    val walletRequestRunner: WalletRequestRunner,
    val contactsRouteRuntime: ContactsRouteRuntime,
    val chatRouteRuntime: ChatRouteRuntime,
    val inboxRouteRuntime: InboxRouteRuntime,
    val inboxActionRouteRuntime: InboxActionRouteRuntime,
    val entryRouteRuntime: EntryRouteRuntime,
    val walletRouteRuntime: WalletRouteRuntime,
    val realtimeConnectionRuntime: RealtimeConnectionRuntime,
    val appPersistenceRuntime: AppPersistenceRuntime,
    val appInitialHydrationRuntime: AppInitialHydrationRuntime,
    val appRouteLoadRuntime: AppRouteLoadRuntime,
    val appRouteNavigationRuntime: AppRouteNavigationRuntime,
)

@Composable
internal fun rememberAppRouteRuntimes(
    dependencies: AppDependencies,
    coroutineScope: CoroutineScope,
): AppRouteRuntimes {
    val appShellCoordinator = dependencies.appShellCoordinator
    val appSessionCoordinator = dependencies.appSessionCoordinator
    val authCoordinator = dependencies.authCoordinator
    val newFriendsCoordinator = dependencies.newFriendsCoordinator
    val contactsCoordinator = dependencies.contactsCoordinator
    val walletRequestCoordinator = dependencies.walletRequestCoordinator
    val chatCoordinator = dependencies.chatCoordinator
    val realtimeCoordinator = dependencies.realtimeCoordinator
    val sessionCredentialsStore = dependencies.sessionCredentialsStore
    val realtimeClient = dependencies.realtimeClient

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
    val walletRequestRunner = remember(coroutineScope) { WalletRequestRunner(coroutineScope) }
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
    val inboxActionRouteRuntime = remember { InboxActionRouteRuntime() }
    val entryRouteRuntime = remember(coroutineScope, authCoordinator) {
        EntryRouteRuntime(
            scope = coroutineScope,
            reduceAuthAction = ::reduceAuthFormState,
            reduceRegisterAction = ::reduceRegisterFormState,
            login = authCoordinator::login,
            register = authCoordinator::register,
        )
    }
    val walletRouteRuntime = remember(walletRequestRunner, walletRequestCoordinator) {
        WalletRouteRuntime(
            requestRunner = walletRequestRunner,
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

    return AppRouteRuntimes(
        callSessionRuntime = callSessionRuntime,
        callSessionRouteRuntime = callSessionRouteRuntime,
        realtimeReconnectRuntime = realtimeReconnectRuntime,
        sessionRefreshRuntime = sessionRefreshRuntime,
        walletRequestRunner = walletRequestRunner,
        contactsRouteRuntime = contactsRouteRuntime,
        chatRouteRuntime = chatRouteRuntime,
        inboxRouteRuntime = inboxRouteRuntime,
        inboxActionRouteRuntime = inboxActionRouteRuntime,
        entryRouteRuntime = entryRouteRuntime,
        walletRouteRuntime = walletRouteRuntime,
        realtimeConnectionRuntime = realtimeConnectionRuntime,
        appPersistenceRuntime = appPersistenceRuntime,
        appInitialHydrationRuntime = appInitialHydrationRuntime,
        appRouteLoadRuntime = appRouteLoadRuntime,
        appRouteNavigationRuntime = appRouteNavigationRuntime,
    )
}
