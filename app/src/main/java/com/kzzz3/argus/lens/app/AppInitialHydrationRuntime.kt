package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.navigation.routeString
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

data class AppRestorableEntryContext(
    val accountId: String,
    val routeString: String,
    val selectedConversationId: String,
)

internal data class AppInitialHydrationRequest(
    val initialSession: AppSessionState,
    val initialCredentials: SessionCredentials,
    val previewThreadsState: ConversationThreadsState,
    val restorableEntryContext: AppRestorableEntryContext? = null,
)

internal data class AppInitialHydrationCallbacks(
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onHydratedConversationAccountChanged: (String?) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
    val onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
    val onSelectedConversationChanged: (String) -> Unit = {},
    val onRestorableEntryContextCleared: () -> Unit = {},
)

internal class AppInitialHydrationRuntime(
    private val loadInitialAuthenticatedConversations: suspend (AppSessionState) -> ConversationThreadsState,
    private val hydrateAppState: suspend (ConversationThreadsState) -> AppHydrationState,
) {
    suspend fun hydrate(
        request: AppInitialHydrationRequest,
        callbacks: AppInitialHydrationCallbacks,
    ) {
        if (
            request.initialSession.isAuthenticated &&
            request.initialSession.accountId.isNotBlank() &&
            request.initialCredentials.hasAccessToken
        ) {
            val loadedThreads = loadInitialAuthenticatedConversations(request.initialSession)
            callbacks.onConversationThreadsChanged(loadedThreads)
            callbacks.onHydratedConversationAccountChanged(request.initialSession.accountId)
            val restoredConversationId = resolveRestoredChatConversationId(
                session = request.initialSession,
                credentials = request.initialCredentials,
                conversationThreadsState = loadedThreads,
                restorableEntryContext = request.restorableEntryContext,
            )
            if (restoredConversationId != null) {
                callbacks.onSelectedConversationChanged(restoredConversationId)
                callbacks.onRouteChanged(AppRoute.Chat)
            } else {
                callbacks.onRestorableEntryContextCleared()
                callbacks.onRouteChanged(AppRoute.Inbox)
            }
            return
        }

        if (request.restorableEntryContext != null) {
            callbacks.onRestorableEntryContextCleared()
        }
        val hydratedState = hydrateAppState(request.previewThreadsState)
        callbacks.onHydratedSessionApplied(
            hydratedState.session,
            hydratedState.hydratedConversationAccountId,
        )
        callbacks.onConversationThreadsChanged(hydratedState.conversationThreadsState)
    }
}

internal fun resolveRestoredChatConversationId(
    session: AppSessionState,
    credentials: SessionCredentials,
    conversationThreadsState: ConversationThreadsState,
    restorableEntryContext: AppRestorableEntryContext?,
): String? {
    val context = restorableEntryContext ?: return null
    val selectedConversationId = context.selectedConversationId.takeIf { it.isNotBlank() } ?: return null
    return selectedConversationId.takeIf {
        session.isAuthenticated &&
            credentials.hasAccessToken &&
            session.accountId.isNotBlank() &&
            context.accountId == session.accountId &&
            context.routeString == AppRoute.Chat.routeString &&
            conversationThreadsState.threads.any { thread -> thread.id == selectedConversationId }
    }
}
