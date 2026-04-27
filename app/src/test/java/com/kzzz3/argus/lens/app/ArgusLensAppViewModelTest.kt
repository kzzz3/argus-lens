package com.kzzz3.argus.lens.app

import androidx.lifecycle.ViewModel
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.model.session.AppSessionState
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
