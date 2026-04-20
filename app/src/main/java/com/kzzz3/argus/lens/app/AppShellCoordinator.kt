package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState

data class AppHydrationState(
    val session: AppSessionState,
    val conversationThreadsState: ConversationThreadsState,
    val hydratedConversationAccountId: String?,
)

data class AppSignedInState(
    val conversationThreadsState: ConversationThreadsState,
    val hydratedConversationAccountId: String,
    val callSessionState: CallSessionState,
    val selectedConversationId: String,
)

data class AppSignedOutState(
    val authFormState: com.kzzz3.argus.lens.feature.auth.AuthFormState,
    val registerFormState: com.kzzz3.argus.lens.feature.register.RegisterFormState,
    val contactsState: ContactsState,
    val callSessionState: CallSessionState,
    val conversationThreadsState: ConversationThreadsState,
    val selectedConversationId: String,
)

class AppShellCoordinator(
    private val sessionRepository: SessionRepository,
    private val conversationRepository: ConversationRepository,
) {
    suspend fun hydrateAppState(
        previewThreadsState: ConversationThreadsState,
    ): AppHydrationState {
        val persistedSession = sessionRepository.loadSession()
        if (!persistedSession.isAuthenticated || persistedSession.accessToken.isBlank()) {
            return AppHydrationState(
                session = AppSessionState(),
                conversationThreadsState = previewThreadsState,
                hydratedConversationAccountId = null,
            )
        }

        val session = createAuthenticatedSession(
            accountId = persistedSession.accountId,
            displayName = persistedSession.displayName,
            accessToken = persistedSession.accessToken,
            refreshToken = persistedSession.refreshToken,
        )
        val threads = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = resolvePreviewDisplayName(session.displayName),
        )
        return AppHydrationState(
            session = session,
            conversationThreadsState = threads,
            hydratedConversationAccountId = session.accountId,
        )
    }

    suspend fun persistSession(
        hydratedSession: Boolean,
        session: AppSessionState,
    ) {
        if (!hydratedSession) return
        if (session.isAuthenticated) {
            sessionRepository.saveSession(session)
        } else {
            sessionRepository.clearSession()
        }
    }

    suspend fun persistConversationThreads(
        session: AppSessionState,
        hydratedConversationAccountId: String?,
        state: ConversationThreadsState,
    ) {
        if (
            session.isAuthenticated &&
            hydratedConversationAccountId == session.accountId
        ) {
            conversationRepository.saveConversationThreads(
                accountId = session.accountId,
                state = state,
            )
        }
    }

    suspend fun handleSignedIn(
        session: AppSessionState,
    ): AppSignedInState {
        val threads = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = session.displayName,
        )
        return AppSignedInState(
            conversationThreadsState = threads,
            hydratedConversationAccountId = session.accountId,
            callSessionState = CallSessionState(),
            selectedConversationId = "",
        )
    }

    fun createSignedOutState(
        previewThreadsState: ConversationThreadsState,
    ): AppSignedOutState {
        return AppSignedOutState(
            authFormState = com.kzzz3.argus.lens.feature.auth.AuthFormState(),
            registerFormState = com.kzzz3.argus.lens.feature.register.RegisterFormState(),
            contactsState = ContactsState(),
            callSessionState = CallSessionState(),
            conversationThreadsState = previewThreadsState,
            selectedConversationId = "",
        )
    }
}
