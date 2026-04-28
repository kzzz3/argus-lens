package com.kzzz3.argus.lens.app.runtime

import com.kzzz3.argus.lens.app.AppShellUseCases
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal class PersistAppStateUseCase(
    private val appShellUseCases: AppShellUseCases,
) {
    suspend fun persistSession(
        session: AppSessionState,
        credentials: SessionCredentials,
    ) {
        appShellUseCases.persistSession(session, credentials)
    }

    suspend fun persistConversationThreads(
        session: AppSessionState,
        hydratedConversationAccountId: String?,
        state: ConversationThreadsState,
    ) {
        appShellUseCases.persistConversationThreads(
            session = session,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = state,
        )
    }
}
