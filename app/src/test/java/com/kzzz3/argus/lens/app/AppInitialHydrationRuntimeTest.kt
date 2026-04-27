package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppInitialHydrationRuntimeTest {
    @Test
    fun hydrate_withRestoredChatEntryAndMatchingConversationRoutesToChatAfterThreadsLoad() = runBlocking {
        val initialSession = authenticatedSession("argus_tester")
        val loadedThreads = threads("conversation-1")
        val events = mutableListOf<String>()
        var restoredConversationId: String? = null
        var routedTo: AppRoute? = null
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = { loadedThreads },
            hydrateAppState = { error("fallback hydration not expected") },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSession,
                initialCredentials = SessionCredentials(accessToken = "token"),
                previewThreadsState = threads("preview"),
                restorableEntryContext = AppRestorableEntryContext(
                    accountId = "argus_tester",
                    routeString = AppRoute.Chat.routeString,
                    selectedConversationId = "conversation-1",
                ),
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = { events += "threads" },
                onHydratedConversationAccountChanged = { events += "hydratedAccount" },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
                onHydratedSessionApplied = { _, _ -> error("hydrated session should not be applied") },
                onSelectedConversationChanged = {
                    events += "selectedConversation"
                    restoredConversationId = it
                },
            ),
        )

        assertEquals("conversation-1", restoredConversationId)
        assertEquals(AppRoute.Chat, routedTo)
        assertEquals(listOf("threads", "hydratedAccount", "selectedConversation", "route"), events)
    }

    @Test
    fun hydrate_withRestoredChatEntryAndMissingConversationFallsBackToInboxAndClearsEntry() = runBlocking {
        val initialSession = authenticatedSession("argus_tester")
        var routedTo: AppRoute? = null
        var clearedRestorableEntry = false
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = { threads("different-conversation") },
            hydrateAppState = { error("fallback hydration not expected") },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSession,
                initialCredentials = SessionCredentials(accessToken = "token"),
                previewThreadsState = threads("preview"),
                restorableEntryContext = AppRestorableEntryContext(
                    accountId = "argus_tester",
                    routeString = AppRoute.Chat.routeString,
                    selectedConversationId = "conversation-1",
                ),
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = {},
                onHydratedConversationAccountChanged = {},
                onRouteChanged = { routedTo = it },
                onHydratedSessionApplied = { _, _ -> error("hydrated session should not be applied") },
                onSelectedConversationChanged = { error("missing conversation must not be selected") },
                onRestorableEntryContextCleared = { clearedRestorableEntry = true },
            ),
        )

        assertTrue(clearedRestorableEntry)
        assertEquals(AppRoute.Inbox, routedTo)
    }

    @Test
    fun hydrate_withRestoredChatEntryForDifferentAccountFallsBackToInboxAndClearsEntry() = runBlocking {
        val initialSession = authenticatedSession("argus_tester")
        var routedTo: AppRoute? = null
        var clearedRestorableEntry = false
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = { threads("conversation-1") },
            hydrateAppState = { error("fallback hydration not expected") },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSession,
                initialCredentials = SessionCredentials(accessToken = "token"),
                previewThreadsState = threads("preview"),
                restorableEntryContext = AppRestorableEntryContext(
                    accountId = "other_account",
                    routeString = AppRoute.Chat.routeString,
                    selectedConversationId = "conversation-1",
                ),
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = {},
                onHydratedConversationAccountChanged = {},
                onRouteChanged = { routedTo = it },
                onHydratedSessionApplied = { _, _ -> error("hydrated session should not be applied") },
                onSelectedConversationChanged = { error("cross-account conversation must not be selected") },
                onRestorableEntryContextCleared = { clearedRestorableEntry = true },
            ),
        )

        assertTrue(clearedRestorableEntry)
        assertEquals(AppRoute.Inbox, routedTo)
    }

    @Test
    fun hydrate_withInitialAuthenticatedSessionAndTokenLoadsInitialThreadsAndRoutesToInbox() = runBlocking {
        val initialSession = authenticatedSession("argus_tester")
        val loadedThreads = threads("loaded")
        var loadInitialSession: AppSessionState? = null
        var hydrateCalled = false
        val events = mutableListOf<String>()
        var routedTo: AppRoute? = null
        var hydratedAccountId: String? = null
        var threadsState: ConversationThreadsState? = null
        var hydratedSession: AppSessionState? = null
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = { session ->
                loadInitialSession = session
                loadedThreads
            },
            hydrateAppState = {
                hydrateCalled = true
                AppHydrationState(AppSessionState(), threads("fallback"), null)
            },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSession,
                initialCredentials = SessionCredentials(accessToken = "token"),
                previewThreadsState = threads("preview"),
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = {
                    events += "threads"
                    threadsState = it
                },
                onHydratedConversationAccountChanged = {
                    events += "hydratedAccount"
                    hydratedAccountId = it
                },
                onRouteChanged = {
                    events += "route"
                    routedTo = it
                },
                onHydratedSessionApplied = { session, _ -> hydratedSession = session },
            ),
        )

        assertEquals(initialSession, loadInitialSession)
        assertEquals(loadedThreads, threadsState)
        assertEquals("argus_tester", hydratedAccountId)
        assertEquals(AppRoute.Inbox, routedTo)
        assertEquals(listOf("threads", "hydratedAccount", "route"), events)
        assertTrue(!hydrateCalled)
        assertNull(hydratedSession)
    }

    @Test
    fun hydrate_withoutInitialTokenAppliesHydratedStateAndDoesNotRoute() = runBlocking {
        val hydratedSession = authenticatedSession("restored")
        val hydratedThreads = threads("hydrated")
        val previewThreads = threads("preview")
        var loadInitialCalled = false
        val events = mutableListOf<String>()
        var appliedSession: AppSessionState? = null
        var appliedHydratedAccountId: String? = null
        var threadsState: ConversationThreadsState? = null
        var routedTo: AppRoute? = null
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = {
                loadInitialCalled = true
                threads("loaded")
            },
            hydrateAppState = { preview ->
                assertEquals(previewThreads, preview)
                AppHydrationState(
                    session = hydratedSession,
                    conversationThreadsState = hydratedThreads,
                    hydratedConversationAccountId = "restored",
                )
            },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = authenticatedSession("argus_tester"),
                initialCredentials = SessionCredentials(),
                previewThreadsState = previewThreads,
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = {
                    events += "threads"
                    threadsState = it
                },
                onHydratedConversationAccountChanged = {},
                onRouteChanged = { routedTo = it },
                onHydratedSessionApplied = { session, hydratedAccountId ->
                    events += "hydratedSession"
                    appliedSession = session
                    appliedHydratedAccountId = hydratedAccountId
                },
            ),
        )

        assertEquals(hydratedSession, appliedSession)
        assertEquals("restored", appliedHydratedAccountId)
        assertEquals(hydratedThreads, threadsState)
        assertEquals(listOf("hydratedSession", "threads"), events)
        assertTrue(!loadInitialCalled)
        assertNull(routedTo)
    }

    @Test
    fun hydrate_withoutInitialTokenClearsRestorableEntryAndDoesNotRoute() = runBlocking {
        val initialSession = authenticatedSession("argus_tester")
        var routedTo: AppRoute? = null
        var clearedRestorableEntry = false
        val events = mutableListOf<String>()
        val runtime = AppInitialHydrationRuntime(
            loadInitialAuthenticatedConversations = { error("initial authenticated load not expected") },
            hydrateAppState = {
                events += "hydratedState"
                AppHydrationState(
                    session = AppSessionState(),
                    conversationThreadsState = threads("fallback"),
                    hydratedConversationAccountId = null,
                )
            },
        )

        runtime.hydrate(
            request = AppInitialHydrationRequest(
                initialSession = initialSession,
                initialCredentials = SessionCredentials(),
                previewThreadsState = threads("preview"),
                restorableEntryContext = AppRestorableEntryContext(
                    accountId = "argus_tester",
                    routeString = AppRoute.Chat.routeString,
                    selectedConversationId = "conversation-1",
                ),
            ),
            callbacks = AppInitialHydrationCallbacks(
                onConversationThreadsChanged = { events += "threads" },
                onHydratedConversationAccountChanged = {},
                onRouteChanged = { routedTo = it },
                onHydratedSessionApplied = { _, _ -> events += "session" },
                onRestorableEntryContextCleared = {
                    events += "clearRestorableEntry"
                    clearedRestorableEntry = true
                },
            ),
        )

        assertTrue(clearedRestorableEntry)
        assertNull(routedTo)
        assertEquals(listOf("clearRestorableEntry", "hydratedState", "session", "threads"), events)
    }

    private fun authenticatedSession(accountId: String): AppSessionState {
        return AppSessionState(
            isAuthenticated = true,
            accountId = accountId,
            displayName = accountId,
        )
    }

    private fun threads(id: String): ConversationThreadsState {
        return ConversationThreadsState(
            threads = listOf(
                InboxConversationThread(
                    id = id,
                    title = id,
                    subtitle = "direct",
                    unreadCount = 0,
                    messages = emptyList(),
                )
            )
        )
    }
}
