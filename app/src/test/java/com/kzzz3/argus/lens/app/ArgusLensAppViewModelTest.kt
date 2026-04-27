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
    fun appViewModel_exposesRuntimeScopeBackedByViewModelScope() {
        val viewModelSource = File("src/main/java/com/kzzz3/argus/lens/app/ArgusLensAppViewModel.kt").readText()

        assertTrue(viewModelSource.contains("viewModelScope"))
        assertTrue(viewModelSource.contains("val runtimeScope"))
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
