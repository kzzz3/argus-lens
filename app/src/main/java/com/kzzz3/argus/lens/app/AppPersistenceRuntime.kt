package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal class AppPersistenceRuntime(
    private val appShellCoordinator: AppShellCoordinator,
) {
    suspend fun persistSession(
        session: AppSessionState,
        credentials: SessionCredentials,
    ) {
        appShellCoordinator.persistSession(session, credentials)
    }

    suspend fun persistConversationThreads(
        session: AppSessionState,
        hydratedConversationAccountId: String?,
        state: ConversationThreadsState,
    ) {
        appShellCoordinator.persistConversationThreads(
            session = session,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = state,
        )
    }
}
