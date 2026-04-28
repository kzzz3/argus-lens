package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.app.state.resolvePreviewDisplayName
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.model.session.createAuthenticatedSession
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler

data class AppHydrationState(
    val session: AppSessionState,
    val conversationThreadsState: ConversationThreadsState,
    val hydratedConversationAccountId: String?,
)

data class AppSignedInState(
    val conversationThreadsState: ConversationThreadsState,
    val hydratedConversationAccountId: String,
    val callSessionState: CallSessionState,
)

data class AppSignedOutState(
    val authFormState: com.kzzz3.argus.lens.feature.auth.AuthFormState,
    val registerFormState: com.kzzz3.argus.lens.feature.register.RegisterFormState,
    val contactsState: ContactsState,
    val callSessionState: CallSessionState,
    val conversationThreadsState: ConversationThreadsState,
)

class AppShellUseCases(
    private val sessionRepository: SessionRepository,
    private val conversationRepository: ConversationRepository,
    private val paymentRepository: PaymentRepository,
    private val backgroundSyncScheduler: BackgroundSyncScheduler,
) {
    suspend fun hydrateAppState(
        previewThreadsState: ConversationThreadsState,
    ): AppHydrationState {
        val persistedSession = sessionRepository.loadSession()
        val credentials = sessionRepository.loadCredentials()
        if (!persistedSession.isAuthenticated || !credentials.hasAccessToken) {
            return AppHydrationState(
                session = AppSessionState(),
                conversationThreadsState = previewThreadsState,
                hydratedConversationAccountId = null,
            )
        }

        val session = createAuthenticatedSession(
            accountId = persistedSession.accountId,
            displayName = persistedSession.displayName,
        )
        val threads = conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = resolvePreviewDisplayName(session.displayName),
        )
        backgroundSyncScheduler.enqueue()
        return AppHydrationState(
            session = session,
            conversationThreadsState = threads,
            hydratedConversationAccountId = session.accountId,
        )
    }

    suspend fun persistSession(
        session: AppSessionState,
        credentials: SessionCredentials,
    ) {
        if (session.isAuthenticated) {
            sessionRepository.saveSession(session, credentials)
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
        backgroundSyncScheduler.enqueue()
        return AppSignedInState(
            conversationThreadsState = threads,
            hydratedConversationAccountId = session.accountId,
            callSessionState = CallSessionState(),
        )
    }

    fun createSignedOutState(
        previewThreadsState: ConversationThreadsState,
        signedOutAccountId: String,
    ): AppSignedOutState {
        paymentRepository.clearLocalData(signedOutAccountId)
        return AppSignedOutState(
            authFormState = com.kzzz3.argus.lens.feature.auth.AuthFormState(),
            registerFormState = com.kzzz3.argus.lens.feature.register.RegisterFormState(),
            contactsState = ContactsState(),
            callSessionState = CallSessionState(),
            conversationThreadsState = previewThreadsState,
        )
    }

    fun createPreviewConversationThreads(
        currentUserDisplayName: String,
    ): ConversationThreadsState {
        return conversationRepository.createPreviewState(currentUserDisplayName)
    }

    suspend fun loadInitialAuthenticatedConversations(
        session: AppSessionState,
    ): ConversationThreadsState {
        return conversationRepository.loadOrCreateConversationThreads(
            accountId = session.accountId,
            currentUserDisplayName = resolvePreviewDisplayName(session.displayName),
        )
    }
}
