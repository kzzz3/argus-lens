package com.kzzz3.argus.lens.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthCoordinator
import com.kzzz3.argus.lens.feature.contacts.ContactsCoordinator
import com.kzzz3.argus.lens.feature.contacts.NewFriendsCoordinator
import com.kzzz3.argus.lens.feature.inbox.ChatCoordinator
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.RealtimeCoordinator
import com.kzzz3.argus.lens.feature.wallet.WalletRequestCoordinator
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArgusLensAppViewModelTest {
    @Test
    fun appViewModelUsesInjectedDependenciesWithoutApplicationSuperclass() {
        assertEquals(ViewModel::class.java, ArgusLensAppViewModel::class.java.superclass)
    }

    @Test
    fun appRouteHost_doesNotOwnLongLivedCoroutineScope() {
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()

        assertFalse(routeHostSource.contains("rememberCoroutineScope"))
    }

    @Test
    fun appRouteHost_usesExplicitStateAndCallbackBoundaryObjects() {
        val boundaryFile = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHostState.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue("AppRouteHostState.kt should define the host boundary objects", boundaryFile.exists())
        val boundarySource = boundaryFile.readText()
        assertTrue(boundarySource.contains("data class AppRouteHostState"))
        assertTrue(boundarySource.contains("data class AppRouteHostCallbacks"))
        assertTrue(routeHostSource.contains("state: AppRouteHostState"))
        assertTrue(routeHostSource.contains("callbacks: AppRouteHostCallbacks"))
        assertTrue(appSource.contains("AppRouteHostState("))
        assertTrue(appSource.contains("AppRouteHostCallbacks("))
        assertFalse(routeHostSource.contains("appSessionState: AppSessionState"))
        assertFalse(routeHostSource.contains("onAuthFormStateChanged: (AuthFormState) -> Unit"))
    }

    @Test
    fun appRouteHost_delegatesActionBindingFactories() {
        val actionBindingsFile = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteActionBindings.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()

        assertTrue("AppRouteActionBindings.kt should own route request/callback adapters", actionBindingsFile.exists())
        val actionBindingsSource = actionBindingsFile.readText()
        listOf(
            "class AppRouteActionBindings",
            "fun openTopLevelRoute",
            "fun openShellDestination",
            "fun openInboxConversation",
            "fun authStateHolderCallbacks",
            "AppRoute.RegisterEntry",
            "AppRoute.AuthEntry",
            "fun inboxStateHolderCallbacks",
            "AppRoute.Contacts",
            "AppRoute.Wallet",
            "fun contactsRouteRequest",
            "fun contactsRouteCallbacks",
            "fun realtimeConnectionCallbacks",
        ).forEach { expectedSource ->
            assertTrue("Expected AppRouteActionBindings.kt to contain $expectedSource", actionBindingsSource.contains(expectedSource))
        }
        listOf(
            "fun openTopLevelRoute(",
            "fun openShellDestination(",
            "fun openInboxConversation(",
            "fun authStateHolderCallbacks(",
            "fun inboxStateHolderCallbacks(",
            "fun contactsRouteRequest(",
            "fun contactsRouteCallbacks(",
            "fun realtimeConnectionCallbacks(",
        ).forEach { forbiddenSource ->
            assertFalse("AppRouteHost.kt should not declare $forbiddenSource", routeHostSource.contains(forbiddenSource))
        }
    }

    @Test
    fun appRouteHost_delegatesLifecycleEffects() {
        val effectsFile = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHostEffects.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()

        assertTrue("AppRouteHostEffects.kt should own host lifecycle effects", effectsFile.exists())
        assertTrue(routeHostSource.contains("AppRouteHostEffects("))
        assertFalse(routeHostSource.contains("LaunchedEffect("))
        assertFalse(routeHostSource.contains("DisposableEffect("))
    }

    @Test
    fun appRouteHostEffectsUseNarrowDependencyBoundary() {
        val effectsSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHostEffects.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()

        assertTrue(effectsSource.contains("data class AppRouteHostEffectDependencies"))
        assertTrue(effectsSource.contains("effectDependencies: AppRouteHostEffectDependencies"))
        assertTrue(effectsSource.contains("val initialSessionSnapshot: AppSessionState"))
        assertTrue(effectsSource.contains("val initialSessionCredentials: SessionCredentials"))
        assertTrue(effectsSource.contains("val sessionCredentialsStore: SessionCredentialsStore"))
        assertTrue(effectsSource.contains("val realtimeClient: ConversationRealtimeClient"))
        assertTrue(routeHostSource.contains("AppRouteHostEffectDependencies("))
        assertFalse(effectsSource.contains("dependencies: AppDependencies"))
        assertFalse(effectsSource.contains("dependencies.initialSessionSnapshot"))
        assertFalse(effectsSource.contains("dependencies.initialSessionCredentials"))
        assertFalse(effectsSource.contains("dependencies.sessionCredentialsStore"))
        assertFalse(effectsSource.contains("dependencies.realtimeClient"))
    }

    @Test
    fun appViewModel_exposesRuntimeScopeBackedByViewModelScope() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()

        assertTrue(viewModelSource.contains("viewModelScope"))
        assertTrue(viewModelSource.contains("val runtimeScope"))
    }

    @Test
    fun appViewModelOwnsWalletStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val routeRuntimesSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteRuntimes.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("val walletStateHolder: WalletStateHolder"))
        assertTrue(viewModelSource.contains("WalletRequestRunner(runtimeScope)"))
        assertTrue(appSource.contains("walletStateHolder = viewModel.walletStateHolder"))
        assertFalse(routeRuntimesSource.contains("WalletStateHolder("))
        assertFalse(routeRuntimesSource.contains("WalletRequestRunner("))
    }

    @Test
    fun appViewModelOwnsAuthStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val routeRuntimesSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteRuntimes.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("val authStateHolder: AuthStateHolder"))
        assertTrue(viewModelSource.contains("createAuthStateHolder(dependencies, runtimeScope)"))
        assertTrue(appSource.contains("authStateHolder = viewModel.authStateHolder"))
        assertFalse(routeRuntimesSource.contains("EntryRouteRuntime("))
        assertFalse(routeRuntimesSource.contains("reduceAuthFormState"))
        assertFalse(routeRuntimesSource.contains("reduceRegisterFormState"))
    }

    @Test
    fun appViewModelOwnsInboxStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val routeRuntimesSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteRuntimes.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()
        val routeUiStateSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteUiState.kt").readText()
        val actionBindingsSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteActionBindings.kt").readText()

        assertTrue(viewModelSource.contains("val inboxStateHolder: InboxStateHolder"))
        assertTrue(viewModelSource.contains("createInboxStateHolder()"))
        assertTrue(appSource.contains("inboxStateHolder = viewModel.inboxStateHolder"))
        assertTrue(routeHostSource.contains("inboxStateHolder.state.collectAsStateWithLifecycle()"))
        assertTrue(actionBindingsSource.contains("InboxStateHolderCallbacks"))
        assertFalse(routeRuntimesSource.contains("InboxActionRouteRuntime"))
        assertFalse(routeUiStateSource.contains("createInboxUiState("))
    }

    @Test
    fun appViewModelOwnsChatStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHost.kt").readText()
        val routeHostEffectsSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHostEffects.kt").readText()
        val routeUiStateSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteUiState.kt").readText()

        assertTrue(viewModelSource.contains("val chatStateHolder: ChatStateHolder"))
        assertTrue(viewModelSource.contains("createChatStateHolder()"))
        assertTrue(appSource.contains("chatStateHolder = viewModel.chatStateHolder"))
        assertTrue(routeHostSource.contains("chatStateHolder.state.collectAsStateWithLifecycle()"))
        assertTrue(routeHostEffectsSource.contains("chatStateHolder.replaceInputs("))
        assertFalse(routeUiStateSource.contains("createChatUiState("))
        assertFalse(routeUiStateSource.contains("ChatState("))
    }

    @Test
    fun appUiStateDoesNotOwnAuthOrRegisterFormState() {
        val stateSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppState.kt").readText()
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostStateSource = File("src/main/java/com/kzzz3/argus/lens/app/AppRouteHostState.kt").readText()

        assertFalse(stateSource.contains("authFormState: AuthFormState"))
        assertFalse(stateSource.contains("registerFormState: RegisterFormState"))
        assertFalse(viewModelSource.contains("fun updateAuthFormState"))
        assertFalse(viewModelSource.contains("fun updateRegisterFormState"))
        assertFalse(appSource.contains("uiState.authFormState"))
        assertFalse(appSource.contains("uiState.registerFormState"))
        assertFalse(routeHostStateSource.contains("authFormState: AuthFormState"))
        assertFalse(routeHostStateSource.contains("registerFormState: RegisterFormState"))
    }

    @Test
    fun appViewModel_keepsStateModelAndTransitionsInDedicatedStateFile() {
        val stateFile = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppState.kt")
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()

        assertTrue("ArgusLensAppState.kt should own the app state model", stateFile.exists())
        val stateSource = stateFile.readText()
        assertTrue(stateSource.contains("data class ArgusLensAppUiState"))
        assertTrue(stateSource.contains("internal fun resolveInitialAppRoute"))
        assertTrue(stateSource.contains("internal fun applySessionClearedTransition"))
        assertFalse(viewModelSource.contains("data class ArgusLensAppUiState"))
        assertFalse(viewModelSource.contains("internal fun resolveInitialAppRoute"))
        assertFalse(viewModelSource.contains("internal fun applySessionClearedTransition"))
    }

    @Test
    fun appViewModel_defersRestoredSelectedConversationUntilHydrationValidatesThreads() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("restorableEntryContext = savedRestorableEntryContext"))
        assertTrue(appSource.contains("onSelectedConversationChanged = viewModel::restoreSelectedConversation"))
        assertFalse(viewModelSource.contains("selectedConversationId = initialRestorableEntryContext?.selectedConversationId.orEmpty()"))
    }

    @Test
    fun appViewModel_openConversationPersistsSafeRestorableEntryContext() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createTestViewModel(savedStateHandle = savedStateHandle)

        viewModel.openConversation("conversation-1")

        val state = viewModel.uiState.value
        assertEquals(AppRoute.Chat, state.currentRoute)
        assertEquals("conversation-1", state.selectedConversationId)
        assertEquals(
            AppRestorableEntryContext(
                accountId = "argus_tester",
                routeString = AppRoute.Chat.routeString,
                selectedConversationId = "conversation-1",
            ),
            state.restorableEntryContext,
        )
        assertEquals("argus_tester", savedStateHandle["restorableEntryAccountId"])
        assertEquals(AppRoute.Chat.routeString, savedStateHandle["restorableEntryRoute"])
        assertEquals("conversation-1", savedStateHandle["restorableEntrySelectedConversationId"])
        assertNull(savedStateHandle["accessToken"])
        assertNull(savedStateHandle["refreshToken"])
    }

    @Test
    fun appViewModel_nonChatRouteAndSessionClearRemoveRestorableEntryContext() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createTestViewModel(savedStateHandle = savedStateHandle)
        viewModel.openConversation("conversation-1")

        viewModel.openRoute(AppRoute.Inbox)

        assertEquals(AppRoute.Inbox, viewModel.uiState.value.currentRoute)
        assertEquals(null, viewModel.uiState.value.restorableEntryContext)
        assertNull(savedStateHandle["restorableEntryAccountId"])
        assertNull(savedStateHandle["restorableEntryRoute"])
        assertNull(savedStateHandle["restorableEntrySelectedConversationId"])

        viewModel.openConversation("conversation-2")
        viewModel.clearSession()

        assertEquals(AppRoute.AuthEntry, viewModel.uiState.value.currentRoute)
        assertEquals("", viewModel.uiState.value.selectedConversationId)
        assertEquals(null, viewModel.uiState.value.restorableEntryContext)
        assertNull(savedStateHandle["restorableEntryAccountId"])
        assertNull(savedStateHandle["restorableEntryRoute"])
        assertNull(savedStateHandle["restorableEntrySelectedConversationId"])
    }

    @Test
    fun appViewModel_savedRestorableEntryIsNotSelectedBeforeHydrationValidation() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "restorableEntryAccountId" to "argus_tester",
                "restorableEntryRoute" to AppRoute.Chat.routeString,
                "restorableEntrySelectedConversationId" to "conversation-1",
            )
        )
        val viewModel = createTestViewModel(savedStateHandle = savedStateHandle)

        val state = viewModel.uiState.value
        assertEquals(AppRoute.Inbox, state.currentRoute)
        assertEquals("", state.selectedConversationId)
        assertEquals(
            AppRestorableEntryContext(
                accountId = "argus_tester",
                routeString = AppRoute.Chat.routeString,
                selectedConversationId = "conversation-1",
            ),
            state.restorableEntryContext,
        )
    }

    @Test
    fun resolveInitialAppRoute_authenticatedSessionWithTokenStartsInInbox() {
        val route = resolveInitialAppRoute(
            session = AppSessionState(
                isAuthenticated = true,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            credentials = SessionCredentials(accessToken = "access-token"),
        )

        assertEquals(AppRoute.Inbox, route)
    }

    @Test
    fun resolveInitialAppRoute_missingTokenStartsAtAuthEntry() {
        val route = resolveInitialAppRoute(
            session = AppSessionState(
                isAuthenticated = true,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
            credentials = SessionCredentials(),
        )

        assertEquals(AppRoute.AuthEntry, route)
    }

    @Test
    fun resolveInitialHydratedConversationAccountId_authenticatedSessionKeepsAccountBoundary() {
        val accountId = resolveInitialHydratedConversationAccountId(
            session = AppSessionState(
                isAuthenticated = true,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
        )

        assertEquals("argus_tester", accountId)
    }

    @Test
    fun resolveInitialHydratedConversationAccountId_signedOutSessionHasNoAccountBoundary() {
        val accountId = resolveInitialHydratedConversationAccountId(
            session = AppSessionState(
                isAuthenticated = false,
                accountId = "argus_tester",
                displayName = "Argus Tester",
            ),
        )

        assertEquals(null, accountId)
    }

    @Test
    fun appUiStateDefaultsRealtimeBookkeepingToDisconnectedState() {
        val state = ArgusLensAppUiState(
            appSessionState = AppSessionState(),
            currentRoute = AppRoute.AuthEntry,
        )

        assertEquals(ConversationRealtimeConnectionState.DISABLED, state.realtimeConnectionState)
        assertEquals("", state.realtimeLastEventId)
        assertEquals(0, state.realtimeReconnectGeneration)
    }

    @Test
    fun applyHydratedSessionTransition_authenticatedSessionMovesToInboxWithHydrationBoundary() {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        )
        val state = applyHydratedSessionTransition(
            state = ArgusLensAppUiState(
                appSessionState = AppSessionState(),
                currentRoute = AppRoute.AuthEntry,
            ),
            session = session,
            hydratedConversationAccountId = "argus_tester",
        )

        assertEquals(session, state.appSessionState)
        assertEquals(AppRoute.Inbox, state.currentRoute)
        assertEquals("argus_tester", state.hydratedConversationAccountId)
    }

    @Test
    fun applyAuthenticatedSessionTransition_clearsConversationAndIncrementsReconnectGeneration() {
        val session = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        )
        val state = applyAuthenticatedSessionTransition(
            state = ArgusLensAppUiState(
                appSessionState = AppSessionState(),
                currentRoute = AppRoute.AuthEntry,
                selectedConversationId = "conversation-1",
                realtimeReconnectGeneration = 2,
            ),
            session = session,
            hydratedConversationAccountId = "argus_tester",
            realtimeReconnectIncrement = 1,
        )

        assertEquals(session, state.appSessionState)
        assertEquals(AppRoute.Inbox, state.currentRoute)
        assertEquals("", state.selectedConversationId)
        assertEquals("argus_tester", state.hydratedConversationAccountId)
        assertEquals(3, state.realtimeReconnectGeneration)
    }

    @Test
    fun applySessionClearedTransition_returnsToAuthEntryAndClearsSessionScopedState() {
        val state = applySessionClearedTransition(
            ArgusLensAppUiState(
                appSessionState = AppSessionState(
                    isAuthenticated = true,
                    accountId = "argus_tester",
                    displayName = "Argus Tester",
                ),
                currentRoute = AppRoute.Chat,
                selectedConversationId = "conversation-1",
                hydratedConversationAccountId = "argus_tester",
            )
        )

        assertEquals(AppSessionState(), state.appSessionState)
        assertEquals(AppRoute.AuthEntry, state.currentRoute)
        assertEquals("", state.selectedConversationId)
        assertEquals(null, state.hydratedConversationAccountId)
    }

    private fun createTestViewModel(
        savedStateHandle: SavedStateHandle,
        initialSession: AppSessionState = AppSessionState(
            isAuthenticated = true,
            accountId = "argus_tester",
            displayName = "Argus Tester",
        ),
        initialCredentials: SessionCredentials = SessionCredentials(accessToken = "access-token"),
    ): ArgusLensAppViewModel {
        return ArgusLensAppViewModel(
            dependencies = createTestDependencies(
                initialSession = initialSession,
                initialCredentials = initialCredentials,
            ),
            savedStateHandle = savedStateHandle,
        )
    }

    private fun createTestDependencies(
        initialSession: AppSessionState,
        initialCredentials: SessionCredentials,
    ): AppDependencies {
        val authRepository = FakeAuthRepository()
        val sessionRepository = FakeSessionRepository(initialSession, initialCredentials)
        val conversationRepository = FakeConversationRepository()
        val friendRepository = FakeFriendRepository()
        val mediaRepository = FakeMediaRepository()
        val paymentRepository = FakePaymentRepository()
        val backgroundSyncScheduler = FakeBackgroundSyncScheduler()
        return AppDependencies(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            friendRepository = friendRepository,
            mediaRepository = mediaRepository,
            paymentRepository = paymentRepository,
            realtimeClient = FakeConversationRealtimeClient(),
            appShellCoordinator = AppShellCoordinator(
                sessionRepository = sessionRepository,
                conversationRepository = conversationRepository,
                paymentRepository = paymentRepository,
                backgroundSyncScheduler = backgroundSyncScheduler,
            ),
            appSessionCoordinator = AppSessionCoordinator(authRepository, sessionRepository),
            authCoordinator = AuthCoordinator(authRepository),
            contactsCoordinator = ContactsCoordinator(conversationRepository, friendRepository),
            newFriendsCoordinator = NewFriendsCoordinator(friendRepository, conversationRepository),
            chatCoordinator = ChatCoordinator(conversationRepository, mediaRepository),
            walletRequestCoordinator = WalletRequestCoordinator(paymentRepository),
            realtimeCoordinator = RealtimeCoordinator(conversationRepository),
            initialSessionSnapshot = initialSession,
            initialSessionCredentials = initialCredentials,
            sessionCredentialsStore = SessionCredentialsStore(initialCredentials),
        )
    }

    private class FakeAuthRepository : AuthRepository {
        override suspend fun restoreSession(accessToken: String): AuthRepositoryResult = failure()
        override suspend fun refreshSession(refreshToken: String): AuthRepositoryResult = failure()
        override suspend fun login(account: String, password: String): AuthRepositoryResult = failure()
        override suspend fun register(displayName: String, account: String, password: String): AuthRepositoryResult = failure()

        private fun failure(): AuthRepositoryResult = AuthRepositoryResult.Failure(
            code = "UNUSED",
            message = "unused",
            kind = AuthFailureKind.NETWORK,
        )
    }

    private class FakeSessionRepository(
        private var session: AppSessionState,
        private var credentials: SessionCredentials,
    ) : SessionRepository {
        override suspend fun loadSession(): AppSessionState = session
        override suspend fun loadCredentials(): SessionCredentials = credentials
        override suspend fun saveSession(state: AppSessionState, credentials: SessionCredentials) {
            session = state
            this.credentials = credentials
        }
        override suspend fun clearSession() {
            session = AppSessionState()
            credentials = SessionCredentials()
        }
    }

    private class FakeConversationRepository : ConversationRepository {
        private val threads = ConversationThreadsState()

        override fun createPreviewState(currentUserDisplayName: String): ConversationThreadsState = threads
        override suspend fun loadOrCreateConversationThreads(accountId: String, currentUserDisplayName: String): ConversationThreadsState = threads
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

    private class FakeFriendRepository : FriendRepository {
        override suspend fun listFriends(): FriendRepositoryResult = failure()
        override suspend fun sendFriendRequest(friendAccountId: String): FriendRepositoryResult = failure()
        override suspend fun listFriendRequests(): FriendRepositoryResult = failure()
        override suspend fun acceptFriendRequest(requestId: String): FriendRepositoryResult = failure()
        override suspend fun rejectFriendRequest(requestId: String): FriendRepositoryResult = failure()
        override suspend fun ignoreFriendRequest(requestId: String): FriendRepositoryResult = failure()

        private fun failure(): FriendRepositoryResult = FriendRepositoryResult.Failure("UNUSED", "unused")
    }

    private class FakeMediaRepository : MediaRepository {
        override suspend fun createUploadSession(
            conversationId: String,
            attachmentKind: ChatDraftAttachmentKind,
            fileName: String,
            contentType: String,
            contentLength: Long,
            durationSeconds: Int?,
        ): MediaRepositoryResult = failure()

        override suspend fun finalizeUploadSession(
            sessionId: String,
            conversationId: String,
            fileName: String,
            contentType: String,
            contentLength: Long,
            objectKey: String,
        ): MediaRepositoryResult = failure()

        override suspend fun uploadContent(uploadSession: com.kzzz3.argus.lens.data.media.MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult = failure()
        override suspend fun downloadAttachment(attachmentId: String, fileName: String): MediaRepositoryResult = failure()

        private fun failure(): MediaRepositoryResult = MediaRepositoryResult.Failure("UNUSED", "unused")
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
