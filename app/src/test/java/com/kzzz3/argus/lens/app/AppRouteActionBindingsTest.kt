package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.AuthReducerResult
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.auth.AuthSubmissionResult
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.call.CallSessionRuntime
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.contacts.AddFriendResult
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsActionResult
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.inbox.ChatActionResult
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatStatusResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.inbox.OpenInboxConversationResult
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.me.createMeUiState
import com.kzzz3.argus.lens.feature.realtime.RealtimeCoordinator
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterReducerResult
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.wallet.WalletEffectHandler
import com.kzzz3.argus.lens.feature.wallet.WalletFeatureController
import com.kzzz3.argus.lens.feature.wallet.WalletReducerResult
import com.kzzz3.argus.lens.feature.wallet.WalletRequestRunner
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.WalletStateHolder
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteActionBindingsTest {
    @Test
    fun authStateHolderCallbacksRouteEntryTransitionsThroughHostCallbacks() {
        val harness = createHarness()
        val callbacks = harness.bindings.authStateHolderCallbacks()

        callbacks.onNavigateToRegister()
        callbacks.onNavigateBackToLogin()

        assertEquals(listOf(AppRoute.RegisterEntry, AppRoute.AuthEntry), harness.hostCallbacks.routes)
        harness.close()
    }

    @Test
    fun openTopLevelAndShellDestinationRouteThroughNavigationRuntimeWithSessionAccount() {
        val harness = createHarness()

        harness.bindings.openTopLevelRoute(AppRoute.Wallet)
        harness.bindings.openShellDestination(ShellDestination.Contacts)
        harness.bindings.openShellDestination(ShellDestination.Secondary)

        assertEquals("argus_tester", harness.walletStateHolder.state.value.currentAccountId)
        assertEquals(listOf(AppRoute.Wallet, AppRoute.Contacts), harness.hostCallbacks.routes)
        harness.close()
    }

    @Test
    fun openInboxConversationUsesCurrentThreadsAndPublishesOpenThenSynchronizedState() = runBlocking {
        val openedThreads = ConversationThreadsState()
        val synchronizedThreads = ConversationThreadsState()
        var openInputThreads: ConversationThreadsState? = null
        var openInputConversationId: String? = null
        var synchronizeInputThreads: ConversationThreadsState? = null
        var synchronizeInputConversationId: String? = null
        val harness = createHarness(
            openInboxConversation = { threads, conversationId ->
                openInputThreads = threads
                openInputConversationId = conversationId
                OpenInboxConversationResult(openedThreads, "opened-$conversationId")
            },
            synchronizeInboxConversation = { threads, conversationId ->
                synchronizeInputThreads = threads
                synchronizeInputConversationId = conversationId
                synchronizedThreads
            },
        )

        harness.bindings.openInboxConversation("conversation-1")

        assertEquals(harness.hostState.conversationThreadsState, openInputThreads)
        assertEquals("conversation-1", openInputConversationId)
        assertEquals(openedThreads, synchronizeInputThreads)
        assertEquals("conversation-1", synchronizeInputConversationId)
        assertEquals(listOf(openedThreads, synchronizedThreads), harness.hostCallbacks.conversationThreadsStates)
        assertEquals(listOf("opened-conversation-1"), harness.hostCallbacks.openedConversations)
        harness.close()
    }

    @Test
    fun inboxStateHolderCallbacksRouteFeatureActionsThroughAppBindings() = runBlocking {
        val harness = createHarness()
        val callbacks = harness.bindings.inboxStateHolderCallbacks()

        callbacks.onOpenConversation("conversation-1")
        callbacks.onOpenContacts()
        callbacks.onOpenWallet()

        assertEquals(listOf("conversation-1"), harness.hostCallbacks.openedConversations)
        assertEquals(listOf(AppRoute.Contacts, AppRoute.Wallet), harness.hostCallbacks.routes)
        assertEquals("argus_tester", harness.walletStateHolder.state.value.currentAccountId)

        callbacks.onSignOutToHud()

        assertEquals(1, harness.sessionCallbacks.sessionClearedCount)
        assertEquals("", harness.walletStateHolder.state.value.currentAccountId)
        assertEquals(AuthFormState(), harness.sessionCallbacks.authFormState)
        harness.close()
    }

    private fun createHarness(
        openInboxConversation: (ConversationThreadsState, String) -> OpenInboxConversationResult = { threads, conversationId ->
            OpenInboxConversationResult(threads, conversationId)
        },
        synchronizeInboxConversation: suspend (ConversationThreadsState, String) -> ConversationThreadsState = { threads, _ -> threads },
    ): Harness {
        val scope = CoroutineScope(Dispatchers.Unconfined)
        val sessionRepository = FakeSessionRepository()
        val conversationRepository = FakeConversationRepository()
        val paymentRepository = FakePaymentRepository()
        val appShellCoordinator = AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            paymentRepository = paymentRepository,
            backgroundSyncScheduler = FakeBackgroundSyncScheduler(),
        )
        val walletStateHolder = createWalletStateHolder(scope)
        val hostState = createHostState()
        val hostCallbacks = RecordingHostCallbacks()
        val sessionCallbacks = RecordingSessionBoundaryCallbacks(walletStateHolder)
        val routeRuntimes = createRouteRuntimes(
            scope = scope,
            appShellCoordinator = appShellCoordinator,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            openInboxConversation = openInboxConversation,
            synchronizeInboxConversation = synchronizeInboxConversation,
        )
        return Harness(
            scope = scope,
            hostState = hostState,
            hostCallbacks = hostCallbacks,
            sessionCallbacks = sessionCallbacks,
            walletStateHolder = walletStateHolder,
            bindings = AppRouteActionBindings(
                state = hostState,
                authStateHolder = createAuthStateHolder(scope),
                inboxStateHolder = InboxStateHolder(),
                walletStateHolder = walletStateHolder,
                callbacks = hostCallbacks.asHostCallbacks(),
                routeUiState = createRouteUiState(hostState),
                routeRuntimes = routeRuntimes,
                previewThreadsState = ConversationThreadsState(),
                sessionBoundaryRuntime = AppSessionBoundaryRuntime(
                    appShellCoordinator = appShellCoordinator,
                    refreshSessionOnce = { _, _ -> FakeAuthRepository.failure() },
                    startSessionRefreshLoop = { _ -> },
                    cancelSessionRefreshLoop = {},
                    invalidateWalletRequests = walletStateHolder::invalidate,
                    cancelCallSession = routeRuntimes.callSessionRuntime::cancel,
                ),
                sessionBoundaryCallbacks = sessionCallbacks.asBoundaryCallbacks(),
                getLatestCurrentRoute = { hostState.currentRoute },
            ),
        )
    }

    private data class Harness(
        val scope: CoroutineScope,
        val hostState: AppRouteHostState,
        val hostCallbacks: RecordingHostCallbacks,
        val sessionCallbacks: RecordingSessionBoundaryCallbacks,
        val walletStateHolder: WalletStateHolder,
        val bindings: AppRouteActionBindings,
    ) {
        fun close() {
            scope.cancel()
        }
    }

    private class RecordingHostCallbacks {
        val routes = mutableListOf<AppRoute>()
        val conversationThreadsStates = mutableListOf<ConversationThreadsState>()
        val openedConversations = mutableListOf<String>()

        fun asHostCallbacks(): AppRouteHostCallbacks {
            return AppRouteHostCallbacks(
                onRouteChanged = routes::add,
                onCallSessionStateChanged = {},
                onContactsStateChanged = {},
                onFriendsChanged = {},
                onConversationOpened = openedConversations::add,
                onSelectedConversationChanged = {},
                onChatStatusChanged = { _, _ -> },
                onChatStatusCleared = {},
                onFriendRequestStatusChanged = {},
                onFriendRequestsSnapshotChanged = {},
                onFriendRequestStatusReset = {},
                onHydratedSessionApplied = { _, _ -> },
                onAuthenticatedSessionApplied = { _, _, _, _ -> },
                onSessionRefreshed = {},
                onSessionCleared = {},
                onConversationThreadsChanged = conversationThreadsStates::add,
                onHydratedConversationAccountChanged = {},
                onRestorableEntryContextCleared = {},
                onRealtimeConnectionStateChanged = {},
                onRealtimeEventIdRecorded = {},
                onRealtimeLastEventIdReset = {},
                onRealtimeReconnectIncremented = {},
            )
        }
    }

    private class RecordingSessionBoundaryCallbacks(
        private val walletStateHolder: WalletStateHolder,
    ) {
        var sessionClearedCount: Int = 0
        var authFormState: AuthFormState? = null

        fun asBoundaryCallbacks(): AppSessionBoundaryCallbacks {
            return AppSessionBoundaryCallbacks(
                onHydratedConversationAccountChanged = {},
                onCallSessionStateChanged = {},
                onWalletStateChanged = walletStateHolder::replaceState,
                onConversationThreadsChanged = {},
                onAuthenticatedSessionApplied = { _, _, _, _ -> },
                onAuthFormStateChanged = { authFormState = it },
                onSessionCleared = { sessionClearedCount += 1 },
                onRegisterFormStateChanged = {},
                onContactsStateChanged = {},
                onFriendsChanged = {},
                onFriendRequestStatusReset = {},
            )
        }
    }

    private fun createHostState(): AppRouteHostState {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        )
        return AppRouteHostState(
            appSessionState = session,
            conversationThreadsState = ConversationThreadsState(),
            currentRoute = AppRoute.Inbox,
            callSessionState = CallSessionState(),
            contactsState = ContactsState(),
            friends = emptyList(),
            selectedConversationId = "",
            restorableEntryContext = null,
            chatStatusMessage = null,
            chatStatusError = false,
            friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList()),
            friendRequestsStatusMessage = null,
            friendRequestsStatusError = false,
            hydratedConversationAccountId = session.accountId,
            realtimeConnectionState = ConversationRealtimeConnectionState.DISABLED,
            realtimeLastEventId = "",
            realtimeReconnectGeneration = 0,
        )
    }

    private fun createRouteUiState(hostState: AppRouteHostState): AppRouteUiState {
        val walletState = WalletState()
        return AppRouteUiState(
            authState = createAuthEntryUiState(AuthFormState()),
            registerState = createRegisterUiState(RegisterFormState()),
            inboxState = createInboxUiState(
                sessionState = hostState.appSessionState,
                threads = hostState.conversationThreadsState.threads,
                realtimeStatusLabel = "disabled",
                shellStatusLabel = "Offline",
            ),
            contactsUiState = createContactsUiState(
                state = hostState.contactsState,
                friends = hostState.friends,
                threads = hostState.conversationThreadsState.threads,
                currentAccountId = hostState.appSessionState.accountId,
            ),
            chatState = null,
            chatUiState = null,
            callSessionUiState = createCallSessionUiState(hostState.callSessionState),
            walletUiState = createWalletUiState(walletState),
            meUiState = createMeUiState(
                sessionState = hostState.appSessionState,
                walletState = walletState,
                friends = hostState.friends,
                conversationThreads = hostState.conversationThreadsState.threads,
                shellStatusLabel = "Offline",
                shellStatusSummary = "Cached shell is available offline.",
            ),
            newFriendsUiState = NewFriendsUiState(
                title = "New Friends",
                subtitle = "Review incoming requests.",
                isLoading = false,
                statusMessage = null,
                isStatusError = false,
                incoming = hostState.friendRequestsSnapshot.incoming,
                outgoing = hostState.friendRequestsSnapshot.outgoing,
            ),
        )
    }

    private fun createAuthStateHolder(scope: CoroutineScope): AuthStateHolder {
        return AuthStateHolder(
            scope = scope,
            reduceAuthAction = { state, _ -> AuthReducerResult(state) },
            reduceRegisterAction = { state, _ -> RegisterReducerResult(state) },
            login = { state, _, _ -> AuthSubmissionResult.Failure(state) },
            register = { state, _, _, _ -> AuthSubmissionResult.Failure(state) },
        )
    }

    private fun createWalletStateHolder(scope: CoroutineScope): WalletStateHolder {
        val runner = WalletRequestRunner(scope)
        val effectHandler = WalletEffectHandler(
            requestRunner = runner,
            loadWalletSummary = { state -> state },
            resolvePayload = { state, _ -> state },
            confirmPayment = { state, _, _, _ -> state },
            loadPaymentHistory = { state -> state },
            loadPaymentReceipt = { state, _ -> state },
        )
        return WalletStateHolder(
            controller = WalletFeatureController(
                reduceAction = { state, _ -> WalletReducerResult(state, null) },
                effectHandler = effectHandler,
            ),
            invalidateRequests = runner::invalidate,
        )
    }

    private fun createRouteRuntimes(
        scope: CoroutineScope,
        appShellCoordinator: AppShellCoordinator,
        sessionRepository: SessionRepository,
        conversationRepository: ConversationRepository,
        openInboxConversation: (ConversationThreadsState, String) -> OpenInboxConversationResult,
        synchronizeInboxConversation: suspend (ConversationThreadsState, String) -> ConversationThreadsState,
    ): AppRouteRuntimes {
        val authRepository = FakeAuthRepository
        val sessionCredentialsStore = SessionCredentialsStore()
        val callSessionRuntime = CallSessionRuntime(scope)
        val realtimeReconnectRuntime = RealtimeReconnectRuntime(scope, delayMillisForAttempt = { 0L })
        return AppRouteRuntimes(
            callSessionRuntime = callSessionRuntime,
            callSessionRouteRuntime = CallSessionRouteRuntime(
                reduceAction = { state, _ -> state },
                endCall = { _, _, _ -> },
            ),
            realtimeReconnectRuntime = realtimeReconnectRuntime,
            sessionRefreshRuntime = SessionRefreshRuntime(
                scope = scope,
                appSessionCoordinator = AppSessionCoordinator(authRepository, sessionRepository),
                credentialsStore = sessionCredentialsStore,
            ),
            contactsRouteRuntime = ContactsRouteRuntime(
                scope = scope,
                openConversation = { request, conversationId ->
                    ContactsOpenConversationResult(request.conversationThreadsState, conversationId)
                },
                addFriend = { state, _ -> AddFriendResult(state, null) },
                acceptFriendRequest = { _, _, snapshot, _ -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
                rejectFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
                ignoreFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
            ),
            chatRouteRuntime = ChatRouteRuntime(
                scope = scope,
                reduceAction = { threads, chat, _ -> ChatActionResult(threads, chat, null) },
                startCall = { _, _, _, _, openCallSession, _ -> openCallSession() },
                dispatchOutgoingMessages = { threads, _, _ -> com.kzzz3.argus.lens.feature.inbox.ChatDispatchResult(threads, null) },
                downloadAttachment = { _, _ -> ChatStatusResult(null, false) },
                recallMessage = { threads, _, _ -> threads },
            ),
            inboxRouteRuntime = InboxRouteRuntime(
                scope = scope,
                openConversation = openInboxConversation,
                synchronizeConversation = synchronizeInboxConversation,
            ),
            realtimeConnectionRuntime = RealtimeConnectionRuntime(
                scope = scope,
                realtimeClient = FakeConversationRealtimeClient(),
                realtimeCoordinator = RealtimeCoordinator(conversationRepository),
                reconnectRuntime = realtimeReconnectRuntime,
            ),
            appPersistenceRuntime = AppPersistenceRuntime(appShellCoordinator),
            appInitialHydrationRuntime = AppInitialHydrationRuntime(
                loadInitialAuthenticatedConversations = { ConversationThreadsState() },
                hydrateAppState = { threads -> AppHydrationState(AppSessionState(), threads, null) },
            ),
            appRouteLoadRuntime = AppRouteLoadRuntime(
                loadFriends = { null },
                loadRequests = { snapshot -> FriendRequestStatusState(snapshot, null) },
            ),
            appRouteNavigationRuntime = AppRouteNavigationRuntime(),
        )
    }

    private object FakeAuthRepository : AuthRepository {
        override suspend fun restoreSession(accessToken: String): AuthRepositoryResult = failure()
        override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult = failure()
        override suspend fun login(account: String, password: String): AuthRepositoryResult = failure()
        override suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult = failure()

        fun failure(): AuthRepositoryResult = AuthRepositoryResult.Failure(
            code = "UNUSED",
            message = "unused",
            kind = AuthFailureKind.NETWORK,
        )
    }

    private class FakeSessionRepository : SessionRepository {
        override suspend fun loadSession(): AppSessionState = AppSessionState()
        override suspend fun loadCredentials(): SessionCredentials = SessionCredentials()
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) = Unit
        override suspend fun clearSession() = Unit
    }

    private class FakeConversationRepository : ConversationRepository {
        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = ConversationThreadsState()
        override suspend fun saveConversationThreads(accountId: String, state: ConversationThreadsState) = Unit
        override suspend fun clearConversationThreads(accountId: String) = Unit
        override suspend fun refreshConversationMessages(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun refreshConversationDetail(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override suspend fun sendMessage(state: ConversationThreadsState, conversationId: String, localMessageId: String, body: String, attachment: ChatMessageAttachment?): ConversationThreadsState = state
        override suspend fun acknowledgeMessageDelivery(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun acknowledgeMessageRead(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun recallMessage(state: ConversationThreadsState, conversationId: String, messageId: String): ConversationThreadsState = state
        override suspend fun markConversationReadRemote(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun markConversationAsRead(state: ConversationThreadsState, conversationId: String): ConversationThreadsState = state
        override fun updateConversationFromChatState(state: ConversationThreadsState, updatedState: ChatState): ConversationThreadsState = state
        override fun resolveOutgoingMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
        override fun resolveDeliveredMessages(state: ConversationThreadsState, conversationId: String, messageIds: List<String>): ConversationThreadsState = state
    }

    private class FakePaymentRepository : PaymentRepository {
        override suspend fun getWalletSummary(): PaymentRepositoryResult = failure()
        override suspend fun resolveScanPayload(scanPayload: String): PaymentRepositoryResult = failure()
        override suspend fun confirmPayment(sessionId: String, amount: Double?, note: String): PaymentRepositoryResult = failure()
        override suspend fun listPayments(): PaymentRepositoryResult = failure()
        override suspend fun getPaymentReceipt(paymentId: String): PaymentRepositoryResult = failure()

        private fun failure(): PaymentRepositoryResult = PaymentRepositoryResult.Failure("UNUSED", "unused")
    }

    private class FakeConversationRealtimeClient : ConversationRealtimeClient {
        override fun connect(
            accessToken: String,
            lastEventId: String?,
            onConnected: () -> Unit,
            onClosed: () -> Unit,
            onEvent: (ConversationRealtimeEvent) -> Unit,
            onError: (Throwable) -> Unit,
        ): ConversationRealtimeSubscription {
            return object : ConversationRealtimeSubscription {
                override fun close() = Unit
            }
        }
    }

    private class FakeBackgroundSyncScheduler : BackgroundSyncScheduler {
        override fun enqueue() = Unit
    }
}
