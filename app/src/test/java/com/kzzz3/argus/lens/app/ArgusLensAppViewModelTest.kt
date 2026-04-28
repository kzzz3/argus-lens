package com.kzzz3.argus.lens.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.composition.AppDependencies
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.app.runtime.AppRestorableEntryContext
import com.kzzz3.argus.lens.app.state.ArgusLensAppUiState
import com.kzzz3.argus.lens.app.state.ArgusLensAppViewModel
import com.kzzz3.argus.lens.app.state.applyAuthenticatedSessionTransition
import com.kzzz3.argus.lens.app.state.applyHydratedSessionTransition
import com.kzzz3.argus.lens.app.state.applySessionClearedTransition
import com.kzzz3.argus.lens.app.state.resolveInitialAppRoute
import com.kzzz3.argus.lens.app.state.resolveInitialHydratedConversationAccountId
import com.kzzz3.argus.lens.core.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.friend.FriendRequestEntry
import com.kzzz3.argus.lens.core.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.core.data.friend.FriendRepository
import com.kzzz3.argus.lens.core.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.core.data.media.MediaRepository
import com.kzzz3.argus.lens.core.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionCredentialsStore
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthUseCases
import com.kzzz3.argus.lens.feature.contacts.ContactsUseCases
import com.kzzz3.argus.lens.feature.contacts.FriendRequestUseCases
import com.kzzz3.argus.lens.feature.contacts.createFriendRequestStatusState
import com.kzzz3.argus.lens.feature.inbox.ChatUseCases
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.ApplyRealtimeConversationEventUseCase
import com.kzzz3.argus.lens.feature.wallet.WalletUseCases
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
    fun appShellHost_doesNotOwnLongLivedCoroutineScope() {
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()

        assertFalse(routeHostSource.contains("rememberCoroutineScope"))
    }

    @Test
    fun appShellHost_usesExplicitStateAndCallbackBoundaryObjects() {
        val boundaryFile = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellState.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue("AppShellState.kt should define the host boundary objects", boundaryFile.exists())
        val boundarySource = boundaryFile.readText()
        assertTrue(boundarySource.contains("data class AppShellState"))
        assertTrue(boundarySource.contains("data class AppShellCallbacks"))
        assertTrue(boundarySource.contains("data class AppFeatureCallbacks"))
        assertTrue(routeHostSource.contains("state: AppShellState"))
        assertTrue(routeHostSource.contains("callbacks: AppShellCallbacks"))
        assertTrue(appSource.contains("AppShellState("))
        assertTrue(appSource.contains("AppShellCallbacks("))
        assertFalse(appSource.contains("onCallSessionStateChanged ="))
        assertFalse(appSource.contains("onContactsStateChanged ="))
        assertFalse(appSource.contains("onConversationThreadsChanged ="))
        assertFalse(routeHostSource.contains("appSessionState: AppSessionState"))
        assertFalse(routeHostSource.contains("onAuthFormStateChanged: (AuthFormState) -> Unit"))
    }

    @Test
    fun appShellHost_delegatesActionDispatcherFactories() {
        val actionBindingsFile = File("src/main/java/com/kzzz3/argus/lens/app/host/AppActionDispatcher.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()

        assertTrue("AppActionDispatcher.kt should own route request/callback adapters", actionBindingsFile.exists())
        val actionBindingsSource = actionBindingsFile.readText()
        listOf(
            "class AppActionDispatcher",
            "fun openTopLevelRoute",
            "fun openShellDestination",
            "fun authStateHolderCallbacks",
            "AppRoute.RegisterEntry",
            "AppRoute.AuthEntry",
            "AppRoute.Contacts",
            "AppRoute.Wallet",
            "fun contactsFeatureCallbacks",
            "fun handleInboxAction",
            "fun handleChatAction",
            "fun handleContactsAction",
            "fun realtimeConnectionCallbacks",
        ).forEach { expectedSource ->
            assertTrue("Expected AppActionDispatcher.kt to contain $expectedSource", actionBindingsSource.contains(expectedSource))
        }
        listOf(
            "fun openTopLevelRoute(",
            "fun openShellDestination(",
            "fun authStateHolderCallbacks(",
            "fun contactsFeatureCallbacks(",
            "fun realtimeConnectionCallbacks(",
        ).forEach { forbiddenSource ->
            assertFalse("AppShellHost.kt should not declare $forbiddenSource", routeHostSource.contains(forbiddenSource))
        }
    }

    @Test
    fun appShellHost_delegatesLifecycleEffects() {
        val effectsFile = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellEffects.kt")
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()

        assertTrue("AppShellEffects.kt should own host lifecycle effects", effectsFile.exists())
        assertTrue(routeHostSource.contains("AppShellEffects("))
        assertFalse(routeHostSource.contains("LaunchedEffect("))
        assertFalse(routeHostSource.contains("DisposableEffect("))
    }

    @Test
    fun appShellEffectsUseNarrowDependencyBoundary() {
        val effectsSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellEffects.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()

        assertTrue(effectsSource.contains("fun AppShellEffects"))
        assertTrue(routeHostSource.contains("AppShellEffects("))
        assertTrue(effectsSource.contains("val initialSessionSnapshot: AppSessionState"))
        assertTrue(effectsSource.contains("val initialSessionCredentials: SessionCredentials"))
        assertTrue(effectsSource.contains("val sessionCredentialsStore: SessionCredentialsStore"))
        assertTrue(effectsSource.contains("val realtimeClient: ConversationRealtimeClient"))
        assertFalse(effectsSource.contains("dependencies: AppDependencies"))
        assertFalse(effectsSource.contains("dependencies.initialSessionSnapshot"))
        assertFalse(effectsSource.contains("dependencies.initialSessionCredentials"))
        assertFalse(effectsSource.contains("dependencies.sessionCredentialsStore"))
        assertFalse(effectsSource.contains("dependencies.realtimeClient"))
    }

    @Test
    fun appViewModel_exposesAppScopeBackedByViewModelScope() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()

        assertTrue(viewModelSource.contains("viewModelScope"))
        assertTrue(viewModelSource.contains("val appScope"))
    }

    @Test
    fun appViewModelOwnsWalletStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val routeHandlersSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppRouteHandlers.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("walletStateHolder = createWalletStateHolder"))
        assertTrue(viewModelSource.contains("WalletRequestRunner(appScope)"))
        assertTrue(appSource.contains("featureStateHolders = viewModel.featureStateHolders"))
        assertFalse(routeHandlersSource.contains("WalletStateHolder("))
        assertFalse(routeHandlersSource.contains("WalletRequestRunner("))
    }

    @Test
    fun appViewModelOwnsAuthStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val routeHandlersSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppRouteHandlers.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("authStateHolder = createAuthStateHolder"))
        assertTrue(viewModelSource.contains("createAuthStateHolder(dependencies, appScope)"))
        assertTrue(appSource.contains("featureStateHolders = viewModel.featureStateHolders"))
        assertFalse(routeHandlersSource.contains("AuthStateHolder("))
        assertFalse(routeHandlersSource.contains("reduceAuthFormState"))
        assertFalse(routeHandlersSource.contains("reduceRegisterFormState"))
    }

    @Test
    fun appViewModelOwnsInboxStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val routeHandlersSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppRouteHandlers.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()
        val routeUiStateSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellUiState.kt").readText()
        val actionBindingsSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppActionDispatcher.kt").readText()

        assertTrue(viewModelSource.contains("inboxStateHolder = createInboxStateHolder"))
        assertTrue(viewModelSource.contains("createInboxStateHolder()"))
        assertTrue(appSource.contains("featureStateHolders = viewModel.featureStateHolders"))
        assertTrue(routeHostSource.contains("inboxStateHolder.state.collectAsStateWithLifecycle()"))
        assertTrue(actionBindingsSource.contains("fun handleInboxAction("))
        assertFalse(routeHandlersSource.contains("InboxStateHolder("))
        assertFalse(routeUiStateSource.contains("createInboxUiState("))
    }

    @Test
    fun appViewModelOwnsChatStateHolderLifetime() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellHost.kt").readText()
        val routeHostEffectsSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellEffects.kt").readText()
        val routeUiStateSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellUiState.kt").readText()

        assertTrue(viewModelSource.contains("chatStateHolder = createChatStateHolder"))
        assertTrue(viewModelSource.contains("createChatStateHolder()"))
        assertTrue(appSource.contains("featureStateHolders = viewModel.featureStateHolders"))
        assertTrue(routeHostSource.contains("chatStateHolder.state.collectAsStateWithLifecycle()"))
        assertTrue(routeHostEffectsSource.contains("chatStateHolder.replaceInputs("))
        assertFalse(routeUiStateSource.contains("createChatUiState("))
        assertFalse(routeUiStateSource.contains("ChatState("))
    }

    @Test
    fun appUiStateDoesNotOwnAuthOrRegisterFormState() {
        val stateSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppState.kt").readText()
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()
        val routeHostStateSource = File("src/main/java/com/kzzz3/argus/lens/app/host/AppShellState.kt").readText()

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
        val stateFile = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppState.kt")
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()

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
    fun appViewModel_defersRestoredActiveChatConversationUntilHydrationValidatesThreads() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/state/ArgusLensAppViewModel.kt").readText()
        val appSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensApp.kt").readText()

        assertTrue(viewModelSource.contains("restorableEntryContext = savedRestorableEntryContext"))
        assertTrue(appSource.contains("onActiveChatConversationChanged = viewModel::restoreActiveChatConversation"))
        assertFalse(viewModelSource.contains("activeChatConversationId = initialRestorableEntryContext?.activeChatConversationId.orEmpty()"))
    }

    @Test
    fun appViewModel_openConversationPersistsSafeRestorableEntryContext() {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createTestViewModel(savedStateHandle = savedStateHandle)

        viewModel.openConversation("conversation-1")

        val state = viewModel.uiState.value
        assertEquals(AppRoute.Chat, state.currentRoute)
        assertEquals("conversation-1", state.activeChatConversationId)
        assertEquals(
            AppRestorableEntryContext(
                accountId = "argus_tester",
                routeString = AppRoute.Chat.routeString,
                activeChatConversationId = "conversation-1",
            ),
            state.restorableEntryContext,
        )
        assertEquals("argus_tester", savedStateHandle["restorableEntryAccountId"])
        assertEquals(AppRoute.Chat.routeString, savedStateHandle["restorableEntryRoute"])
        assertEquals("conversation-1", savedStateHandle["restorableChatConversationId"])
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
        assertNull(savedStateHandle["restorableChatConversationId"])

        viewModel.openConversation("conversation-2")
        viewModel.clearSession()

        assertEquals(AppRoute.AuthEntry, viewModel.uiState.value.currentRoute)
        assertEquals("", viewModel.uiState.value.activeChatConversationId)
        assertEquals(null, viewModel.uiState.value.restorableEntryContext)
        assertNull(savedStateHandle["restorableEntryAccountId"])
        assertNull(savedStateHandle["restorableEntryRoute"])
        assertNull(savedStateHandle["restorableChatConversationId"])
    }

    @Test
    fun appViewModel_savedRestorableEntryIsNotSelectedBeforeHydrationValidation() {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "restorableEntryAccountId" to "argus_tester",
                "restorableEntryRoute" to AppRoute.Chat.routeString,
                "restorableChatConversationId" to "conversation-1",
            )
        )
        val viewModel = createTestViewModel(savedStateHandle = savedStateHandle)

        val state = viewModel.uiState.value
        assertEquals(AppRoute.Inbox, state.currentRoute)
        assertEquals("", state.activeChatConversationId)
        assertEquals(
            AppRestorableEntryContext(
                accountId = "argus_tester",
                routeString = AppRoute.Chat.routeString,
                activeChatConversationId = "conversation-1",
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
                activeChatConversationId = "conversation-1",
                realtimeReconnectGeneration = 2,
            ),
            session = session,
            hydratedConversationAccountId = "argus_tester",
            realtimeReconnectIncrement = 1,
        )

        assertEquals(session, state.appSessionState)
        assertEquals(AppRoute.Inbox, state.currentRoute)
        assertEquals("", state.activeChatConversationId)
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
                activeChatConversationId = "conversation-1",
                hydratedConversationAccountId = "argus_tester",
            )
        )

        assertEquals(AppSessionState(), state.appSessionState)
        assertEquals(AppRoute.AuthEntry, state.currentRoute)
        assertEquals("", state.activeChatConversationId)
        assertEquals(null, state.hydratedConversationAccountId)
    }

    @Test
    fun appViewModel_realtimeMutationsApplyConnectionEventAndGenerationState() {
        val viewModel = createTestViewModel(savedStateHandle = SavedStateHandle())

        viewModel.updateRealtimeConnectionState(ConversationRealtimeConnectionState.CONNECTING)
        viewModel.recordRealtimeEventId("event-1")
        viewModel.recordRealtimeEventId("   ")
        viewModel.incrementRealtimeReconnectGeneration()
        viewModel.incrementRealtimeReconnectGeneration()

        assertEquals(ConversationRealtimeConnectionState.CONNECTING, viewModel.uiState.value.realtimeConnectionState)
        assertEquals("event-1", viewModel.uiState.value.realtimeLastEventId)
        assertEquals(2, viewModel.uiState.value.realtimeReconnectGeneration)

        viewModel.resetRealtimeLastEventId()

        assertEquals("", viewModel.uiState.value.realtimeLastEventId)
    }

    @Test
    fun appViewModel_chatStatusMutationsSetAndClearMessageState() {
        val viewModel = createTestViewModel(savedStateHandle = SavedStateHandle())
        val holder = viewModel.featureStateHolders.inboxChatFeatureStateHolder

        holder.updateChatStatus("Attachment downloaded.", isError = false)

        assertEquals("Attachment downloaded.", holder.state.value.chatStatusMessage)
        assertFalse(holder.state.value.chatStatusError)

        holder.updateChatStatus("Attachment failed.", isError = true)

        assertEquals("Attachment failed.", holder.state.value.chatStatusMessage)
        assertTrue(holder.state.value.chatStatusError)

        holder.clearChatStatus()

        assertEquals(null, holder.state.value.chatStatusMessage)
        assertFalse(holder.state.value.chatStatusError)
    }

    @Test
    fun appViewModel_friendRequestStatusMutationsApplySnapshotAndResetState() {
        val viewModel = createTestViewModel(savedStateHandle = SavedStateHandle())
        val holder = viewModel.featureStateHolders.contactsFeatureStateHolder
        val incoming = FriendRequestEntry(
            requestId = "request-1",
            accountId = "alice",
            displayName = "Alice",
            direction = "INCOMING",
            status = "PENDING",
            note = "hello",
        )
        val snapshot = FriendRequestsSnapshot(
            incoming = listOf(incoming),
            outgoing = emptyList(),
        )

        holder.applyFriendRequestStatus(
            createFriendRequestStatusState(
                snapshot = snapshot,
                message = "Friend request accepted.",
                isError = false,
            )
        )

        assertEquals(snapshot, holder.state.value.friendRequestsSnapshot)
        assertEquals("Friend request accepted.", holder.state.value.friendRequestsStatusMessage)
        assertFalse(holder.state.value.friendRequestsStatusError)

        holder.replaceFriendRequestsSnapshot(FriendRequestsSnapshot(emptyList(), listOf(incoming)))

        assertEquals(FriendRequestsSnapshot(emptyList(), listOf(incoming)), holder.state.value.friendRequestsSnapshot)
        assertEquals("Friend request accepted.", holder.state.value.friendRequestsStatusMessage)

        holder.resetFriendRequestStatus()

        assertEquals(FriendRequestsSnapshot(emptyList(), emptyList()), holder.state.value.friendRequestsSnapshot)
        assertEquals(null, holder.state.value.friendRequestsStatusMessage)
        assertFalse(holder.state.value.friendRequestsStatusError)
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
            appShellUseCases = AppShellUseCases(
                sessionRepository = sessionRepository,
                conversationRepository = conversationRepository,
                paymentRepository = paymentRepository,
                backgroundSyncScheduler = backgroundSyncScheduler,
            ),
            appSessionRefresher = AppSessionRefresher(authRepository, sessionRepository),
            authUseCases = AuthUseCases(authRepository),
            contactsUseCases = ContactsUseCases(conversationRepository, friendRepository),
            friendRequestUseCases = FriendRequestUseCases(friendRepository, conversationRepository),
            chatUseCases = ChatUseCases(conversationRepository, mediaRepository),
            walletUseCases = WalletUseCases(paymentRepository),
            applyRealtimeConversationEventUseCase = ApplyRealtimeConversationEventUseCase(conversationRepository),
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

        override suspend fun uploadContent(uploadSession: com.kzzz3.argus.lens.core.data.media.MediaUploadSession, contentBytes: ByteArray): MediaRepositoryResult = failure()
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
