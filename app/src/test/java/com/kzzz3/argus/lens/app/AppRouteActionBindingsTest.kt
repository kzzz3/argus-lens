package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.host.AppActionDispatcher
import com.kzzz3.argus.lens.app.host.AppFeatureCallbacks
import com.kzzz3.argus.lens.app.host.AppRouteHandlers
import com.kzzz3.argus.lens.app.host.AppShellCallbacks
import com.kzzz3.argus.lens.app.host.AppShellState
import com.kzzz3.argus.lens.app.host.AppShellUiState
import com.kzzz3.argus.lens.app.runtime.AppRouteNavigator
import com.kzzz3.argus.lens.app.runtime.PersistAppStateUseCase
import com.kzzz3.argus.lens.app.runtime.RestoreAppSessionUseCase
import com.kzzz3.argus.lens.app.runtime.SessionBoundaryHandler
import com.kzzz3.argus.lens.app.runtime.AppSessionBoundaryCallbacks
import com.kzzz3.argus.lens.core.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.AuthReducerResult
import com.kzzz3.argus.lens.feature.auth.AuthStateHolder
import com.kzzz3.argus.lens.feature.auth.AuthSubmissionResult
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.call.CallSessionTimer
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.contacts.AddFriendResult
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureController
import com.kzzz3.argus.lens.feature.contacts.ContactsFeatureState
import com.kzzz3.argus.lens.feature.contacts.ContactsOpenConversationResult
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteHandler
import com.kzzz3.argus.lens.feature.contacts.ContactsRouteLoadHandler
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.FriendRequestStatusState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsActionResult
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.inbox.ChatActionResult
import com.kzzz3.argus.lens.feature.inbox.ChatRouteHandler
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatStatusResult
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureController
import com.kzzz3.argus.lens.feature.inbox.InboxChatFeatureState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxRouteHandler
import com.kzzz3.argus.lens.feature.inbox.InboxStateHolder
import com.kzzz3.argus.lens.feature.inbox.OpenInboxConversationResult
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.me.createMeUiState
import com.kzzz3.argus.lens.feature.realtime.ApplyRealtimeConversationEventUseCase
import com.kzzz3.argus.lens.feature.realtime.RealtimeConnectionManager
import com.kzzz3.argus.lens.feature.realtime.RealtimeReconnectScheduler
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
import com.kzzz3.argus.lens.session.SessionCredentialsStore
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import com.kzzz3.argus.lens.session.SessionRefreshScheduler
import com.kzzz3.argus.lens.ui.shell.ShellDestination
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AppActionDispatcherTest {
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
    fun openTopLevelAndShellDestinationRouteThroughNavigatorWithSessionAccount() {
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

        harness.bindings.handleInboxAction(InboxAction.OpenConversation("conversation-1"))

        assertEquals(harness.conversationThreadsState, openInputThreads)
        assertEquals("conversation-1", openInputConversationId)
        assertEquals(openedThreads, synchronizeInputThreads)
        assertEquals("conversation-1", synchronizeInputConversationId)
        assertEquals(listOf(openedThreads, synchronizedThreads), harness.featureCallbacks.conversationThreadsStates)
        assertEquals(listOf("opened-conversation-1"), harness.hostCallbacks.openedConversations)
        harness.close()
    }

    @Test
    fun inboxStateHolderCallbacksRouteFeatureActionsThroughAppBindings() = runBlocking {
        val harness = createHarness()
        harness.bindings.handleInboxAction(InboxAction.OpenConversation("conversation-1"))
        harness.bindings.handleInboxAction(InboxAction.OpenContacts)
        harness.bindings.handleInboxAction(InboxAction.OpenWallet)

        assertEquals(listOf("conversation-1"), harness.hostCallbacks.openedConversations)
        assertEquals(listOf(AppRoute.Contacts, AppRoute.Wallet), harness.hostCallbacks.routes)
        assertEquals("argus_tester", harness.walletStateHolder.state.value.currentAccountId)

        harness.bindings.handleInboxAction(InboxAction.SignOutToHud)

        assertEquals(1, harness.sessionCallbacks.sessionClearedCount)
        assertEquals("", harness.walletStateHolder.state.value.currentAccountId)
        assertEquals(AuthFormState(), harness.sessionCallbacks.authFormState)
        harness.close()
    }

    @Test
    fun realtimeConnectionCallbacksDelegateRealtimeStateAndSignOutThroughAppBindings() = runBlocking {
        val harness = createHarness()
        val callbacks = harness.bindings.realtimeConnectionCallbacks()
        val updatedThreads = ConversationThreadsState()

        callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.RECOVERING)
        callbacks.onEventIdRecorded("event-1")
        callbacks.onLastEventIdReset()
        callbacks.onConversationThreadsChanged(updatedThreads)
        callbacks.onReconnectGenerationIncremented()
        callbacks.signOutToEntry("Session expired.")

        assertEquals(listOf(ConversationRealtimeConnectionState.RECOVERING), harness.hostCallbacks.realtimeConnectionStates)
        assertEquals(listOf("event-1"), harness.hostCallbacks.realtimeEventIds)
        assertEquals(1, harness.hostCallbacks.realtimeLastEventResetCount)
        assertEquals(1, harness.hostCallbacks.realtimeReconnectIncrementCount)
        assertEquals(listOf(updatedThreads), harness.featureCallbacks.conversationThreadsStates)
        assertEquals(1, harness.sessionCallbacks.sessionClearedCount)
        assertEquals("Session expired.", harness.sessionCallbacks.authFormState?.submitResult)
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
        val appShellUseCases = AppShellUseCases(
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            paymentRepository = paymentRepository,
            backgroundSyncScheduler = FakeBackgroundSyncScheduler(),
        )
        val walletStateHolder = createWalletStateHolder(scope)
        val hostState = createHostState()
        val conversationThreadsState = ConversationThreadsState()
        val hostCallbacks = RecordingHostCallbacks()
        val featureCallbacks = RecordingFeatureCallbacks()
        val sessionCallbacks = RecordingSessionBoundaryCallbacks(walletStateHolder)
        val routeHandlers = createRouteHandlers(
            scope = scope,
            appShellUseCases = appShellUseCases,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            openInboxConversation = openInboxConversation,
            synchronizeInboxConversation = synchronizeInboxConversation,
        )
        return Harness(
            scope = scope,
            hostState = hostState,
            conversationThreadsState = conversationThreadsState,
            hostCallbacks = hostCallbacks,
            featureCallbacks = featureCallbacks,
            sessionCallbacks = sessionCallbacks,
            walletStateHolder = walletStateHolder,
            bindings = AppActionDispatcher(
                state = hostState,
                contactsFeatureState = ContactsFeatureState(),
                callSessionState = CallSessionState(),
                inboxChatFeatureState = InboxChatFeatureState(conversationThreadsState),
                authStateHolder = createAuthStateHolder(scope),
                inboxChatFeatureController = InboxChatFeatureController(
                    inboxStateHolder = InboxStateHolder(),
                    inboxRouteHandler = routeHandlers.inboxRouteHandler,
                    chatRouteHandler = routeHandlers.chatRouteHandler,
                ),
                walletStateHolder = walletStateHolder,
                callbacks = hostCallbacks.asHostCallbacks(),
                featureCallbacks = featureCallbacks.asFeatureCallbacks(),
                routeUiState = createRouteUiState(
                    hostState = hostState,
                    conversationThreadsState = conversationThreadsState,
                ),
                routeHandlers = routeHandlers,
                previewThreadsState = ConversationThreadsState(),
                sessionBoundaryHandler = SessionBoundaryHandler(
                    appShellUseCases = appShellUseCases,
                    refreshSessionOnce = { _, _ -> SessionRefreshOutcome.Failure(isUnauthorized = false) },
                    startSessionRefreshLoop = { _ -> },
                    cancelSessionRefreshLoop = {},
                    invalidateWalletRequests = walletStateHolder::invalidate,
                    cancelCallSession = routeHandlers.callSessionTimer::cancel,
                ),
                sessionBoundaryCallbacks = sessionCallbacks.asBoundaryCallbacks(),
                getLatestCurrentRoute = { hostState.currentRoute },
            ),
        )
    }

    private data class Harness(
        val scope: CoroutineScope,
        val hostState: AppShellState,
        val conversationThreadsState: ConversationThreadsState,
        val hostCallbacks: RecordingHostCallbacks,
        val featureCallbacks: RecordingFeatureCallbacks,
        val sessionCallbacks: RecordingSessionBoundaryCallbacks,
        val walletStateHolder: WalletStateHolder,
        val bindings: AppActionDispatcher,
    ) {
        fun close() {
            scope.cancel()
        }
    }

    private class RecordingHostCallbacks {
        val routes = mutableListOf<AppRoute>()
        val openedConversations = mutableListOf<String>()
        val realtimeConnectionStates = mutableListOf<ConversationRealtimeConnectionState>()
        val realtimeEventIds = mutableListOf<String>()
        var realtimeLastEventResetCount: Int = 0
        var realtimeReconnectIncrementCount: Int = 0

        fun asHostCallbacks(): AppShellCallbacks {
            return AppShellCallbacks(
                onRouteChanged = routes::add,
                onConversationOpened = openedConversations::add,
                onActiveChatConversationChanged = {},
                onHydratedSessionApplied = { _, _ -> },
                onAuthenticatedSessionApplied = { _, _, _, _ -> },
                onSessionRefreshed = {},
                onSessionCleared = {},
                onHydratedConversationAccountChanged = {},
                onRestorableEntryContextCleared = {},
                onRealtimeConnectionStateChanged = realtimeConnectionStates::add,
                onRealtimeEventIdRecorded = realtimeEventIds::add,
                onRealtimeLastEventIdReset = { realtimeLastEventResetCount += 1 },
                onRealtimeReconnectIncremented = { realtimeReconnectIncrementCount += 1 },
            )
        }
    }

    private class RecordingFeatureCallbacks {
        val conversationThreadsStates = mutableListOf<ConversationThreadsState>()

        fun asFeatureCallbacks(): AppFeatureCallbacks {
            return AppFeatureCallbacks(
                onCallSessionStateChanged = {},
                onContactsStateChanged = {},
                onFriendsChanged = {},
                onChatStatusChanged = { _, _ -> },
                onChatStatusCleared = {},
                onFriendRequestStatusChanged = {},
                onFriendRequestsSnapshotChanged = {},
                onFriendRequestStatusReset = {},
                onConversationThreadsChanged = conversationThreadsStates::add,
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

    private fun createHostState(): AppShellState {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        )
        return AppShellState(
            appSessionState = session,
            currentRoute = AppRoute.Inbox,
            activeChatConversationId = "",
            restorableEntryContext = null,
            hydratedConversationAccountId = session.accountId,
            realtimeConnectionState = ConversationRealtimeConnectionState.DISABLED,
            realtimeLastEventId = "",
            realtimeReconnectGeneration = 0,
        )
    }

    private fun createRouteUiState(
        hostState: AppShellState,
        conversationThreadsState: ConversationThreadsState,
    ): AppShellUiState {
        val walletState = WalletState()
        return AppShellUiState(
            authState = createAuthEntryUiState(AuthFormState()),
            registerState = createRegisterUiState(RegisterFormState()),
            inboxState = createInboxUiState(
                sessionState = hostState.appSessionState,
                threads = conversationThreadsState.threads,
                realtimeStatusLabel = "disabled",
                shellStatusLabel = "Offline",
            ),
            contactsUiState = createContactsUiState(
                state = ContactsState(),
                friends = emptyList(),
                threads = conversationThreadsState.threads,
                currentAccountId = hostState.appSessionState.accountId,
            ),
            chatState = null,
            chatUiState = null,
            callSessionUiState = createCallSessionUiState(CallSessionState()),
            walletUiState = createWalletUiState(walletState),
            meUiState = createMeUiState(
                sessionState = hostState.appSessionState,
                walletState = walletState,
                friends = emptyList(),
                conversationThreads = conversationThreadsState.threads,
                shellStatusLabel = "Offline",
                shellStatusSummary = "Cached shell is available offline.",
            ),
            newFriendsUiState = NewFriendsUiState(
                title = "New Friends",
                subtitle = "Review incoming requests.",
                isLoading = false,
                statusMessage = null,
                isStatusError = false,
                incoming = emptyList(),
                outgoing = emptyList(),
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

    private fun createRouteHandlers(
        scope: CoroutineScope,
        appShellUseCases: AppShellUseCases,
        sessionRepository: SessionRepository,
        conversationRepository: ConversationRepository,
        openInboxConversation: (ConversationThreadsState, String) -> OpenInboxConversationResult,
        synchronizeInboxConversation: suspend (ConversationThreadsState, String) -> ConversationThreadsState,
    ): AppRouteHandlers {
        val authRepository = FakeAuthRepository
        val sessionCredentialsStore = SessionCredentialsStore()
        val callSessionTimer = CallSessionTimer(scope)
        val realtimeReconnectScheduler = RealtimeReconnectScheduler(scope, delayMillisForAttempt = { 0L })
        return AppRouteHandlers(
            callSessionTimer = callSessionTimer,
            callSessionFeatureController = com.kzzz3.argus.lens.feature.call.CallSessionFeatureController(com.kzzz3.argus.lens.feature.call.CallSessionRouteHandler(
                reduceAction = { state, _ -> state },
                endCall = { _, _, _ -> },
            )),
            realtimeReconnectScheduler = realtimeReconnectScheduler,
            sessionRefreshScheduler = SessionRefreshScheduler(
                scope = scope,
                sessionRefresher = AppSessionRefresher(authRepository, sessionRepository),
                credentialsStore = sessionCredentialsStore,
            ),
            contactsFeatureController = ContactsFeatureController(ContactsRouteHandler(
                scope = scope,
                openConversation = { request, conversationId ->
                    ContactsOpenConversationResult(request.conversationThreadsState, conversationId)
                },
                addFriend = { state, _ -> AddFriendResult(state, null) },
                acceptFriendRequest = { _, _, snapshot, _ -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
                rejectFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
                ignoreFriendRequest = { _, snapshot -> NewFriendsActionResult(FriendRequestStatusState(snapshot, null)) },
            )),
            chatRouteHandler = ChatRouteHandler(
                scope = scope,
                reduceAction = { threads, chat, _ -> ChatActionResult(threads, chat, null) },
                startCall = { _, _, _, _, openCallSession, _ -> openCallSession() },
                dispatchOutgoingMessages = { threads, _, _ -> com.kzzz3.argus.lens.feature.inbox.ChatDispatchResult(threads, null) },
                downloadAttachment = { _, _ -> ChatStatusResult(null, false) },
                recallMessage = { threads, _, _ -> threads },
            ),
            inboxRouteHandler = InboxRouteHandler(
                scope = scope,
                openConversation = openInboxConversation,
                synchronizeConversation = synchronizeInboxConversation,
            ),
            realtimeConnectionManager = RealtimeConnectionManager(
                scope = scope,
                realtimeClient = FakeConversationRealtimeClient(),
                applyRealtimeConversationEvent = ApplyRealtimeConversationEventUseCase(conversationRepository),
                reconnectScheduler = realtimeReconnectScheduler,
            ),
            persistAppStateUseCase = PersistAppStateUseCase(appShellUseCases),
            restoreAppSessionUseCase = RestoreAppSessionUseCase(
                loadInitialAuthenticatedConversations = { ConversationThreadsState() },
                hydrateAppState = { threads -> AppHydrationState(AppSessionState(), threads, null) },
            ),
            contactsRouteLoadHandler = ContactsRouteLoadHandler(
                loadFriends = { null },
                loadRequests = { snapshot -> FriendRequestStatusState(snapshot, null) },
            ),
            appRouteNavigator = AppRouteNavigator(),
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
