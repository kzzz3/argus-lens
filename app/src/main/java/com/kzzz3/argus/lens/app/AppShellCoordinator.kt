package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
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
        val session = sessionRepository.loadSession()
        if (!session.isAuthenticated) {
            return AppHydrationState(
                session = session,
                conversationThreadsState = previewThreadsState,
                hydratedConversationAccountId = null,
            )
        }

        val threads = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = session.displayName,
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
