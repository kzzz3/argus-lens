package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class AppInitialHydrationRequest(
    val initialSession: AppSessionState,
    val initialCredentials: SessionCredentials,
    val previewThreadsState: ConversationThreadsState,
)

internal data class AppInitialHydrationCallbacks(
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onHydratedConversationAccountChanged: (String?) -> Unit,
    val onRouteChanged: (AppRoute) -> Unit,
    val onHydratedSessionApplied: (AppSessionState, String?) -> Unit,
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
            callbacks.onConversationThreadsChanged(loadInitialAuthenticatedConversations(request.initialSession))
            callbacks.onHydratedConversationAccountChanged(request.initialSession.accountId)
            callbacks.onRouteChanged(AppRoute.Inbox)
            return
        }

        val hydratedState = hydrateAppState(request.previewThreadsState)
        callbacks.onHydratedSessionApplied(
            hydratedState.session,
            hydratedState.hydratedConversationAccountId,
        )
        callbacks.onConversationThreadsChanged(hydratedState.conversationThreadsState)
    }
}
