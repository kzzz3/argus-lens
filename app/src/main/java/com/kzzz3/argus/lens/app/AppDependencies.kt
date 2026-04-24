package com.kzzz3.argus.lens.app

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.auth.createAuthRepository
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.createConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.createFriendRepository
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.createMediaRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.createPaymentRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.realtime.createConversationRealtimeClient
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.data.session.createLocalSessionStore
import com.kzzz3.argus.lens.data.session.createLocalSessionSnapshot
import com.kzzz3.argus.lens.feature.auth.AuthCoordinator
import com.kzzz3.argus.lens.feature.contacts.ContactsCoordinator
import com.kzzz3.argus.lens.feature.contacts.NewFriendsCoordinator
import com.kzzz3.argus.lens.feature.inbox.ChatCoordinator
import com.kzzz3.argus.lens.feature.realtime.RealtimeCoordinator
import com.kzzz3.argus.lens.feature.wallet.WalletRequestCoordinator

data class AppDependencies(
    val authRepository: AuthRepository,
    val sessionRepository: SessionRepository,
    val conversationRepository: ConversationRepository,
    val friendRepository: FriendRepository,
    val mediaRepository: MediaRepository,
    val paymentRepository: PaymentRepository,
    val realtimeClient: ConversationRealtimeClient,
    val appShellCoordinator: AppShellCoordinator,
    val appSessionCoordinator: AppSessionCoordinator,
    val authCoordinator: AuthCoordinator,
    val contactsCoordinator: ContactsCoordinator,
    val newFriendsCoordinator: NewFriendsCoordinator,
    val chatCoordinator: ChatCoordinator,
    val walletRequestCoordinator: WalletRequestCoordinator,
    val realtimeCoordinator: RealtimeCoordinator,
    val initialSessionSnapshot: AppSessionState,
)

@Composable
fun rememberAppDependencies(
    context: Context,
): AppDependencies {
    val authRepository = remember { createAuthRepository() }
    val sessionRepository = remember(context) { createLocalSessionStore(context) }
    val conversationRepository = remember(context, sessionRepository) {
        createConversationRepository(context = context, sessionRepository = sessionRepository)
    }
    val friendRepository = remember(sessionRepository) {
        createFriendRepository(sessionRepository)
    }
    val mediaRepository = remember(sessionRepository, context) {
        createMediaRepository(sessionRepository, context)
    }
    val paymentRepository = remember(sessionRepository, context) {
        createPaymentRepository(context, sessionRepository)
    }
    val realtimeClient = remember { createConversationRealtimeClient() }
    val initialSessionSnapshot = remember(context) { createLocalSessionSnapshot(context) }
    val appShellCoordinator = remember(sessionRepository, conversationRepository, paymentRepository) {
        AppShellCoordinator(
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            paymentRepository = paymentRepository,
        )
    }
    val appSessionCoordinator = remember(authRepository) {
        AppSessionCoordinator(authRepository)
    }
    val authCoordinator = remember(authRepository) {
        AuthCoordinator(authRepository)
    }
    val contactsCoordinator = remember(conversationRepository, friendRepository) {
        ContactsCoordinator(conversationRepository, friendRepository)
    }
    val newFriendsCoordinator = remember(friendRepository, conversationRepository) {
        NewFriendsCoordinator(friendRepository, conversationRepository)
    }
    val chatCoordinator = remember(conversationRepository, mediaRepository) {
        ChatCoordinator(conversationRepository, mediaRepository)
    }
    val walletRequestCoordinator = remember(paymentRepository) {
        WalletRequestCoordinator(paymentRepository)
    }
    val realtimeCoordinator = remember(conversationRepository) {
        RealtimeCoordinator(conversationRepository)
    }
    return remember(
        authRepository,
        sessionRepository,
        conversationRepository,
        friendRepository,
        mediaRepository,
        paymentRepository,
        realtimeClient,
        appShellCoordinator,
        appSessionCoordinator,
        authCoordinator,
        contactsCoordinator,
        newFriendsCoordinator,
        chatCoordinator,
        walletRequestCoordinator,
        realtimeCoordinator,
        initialSessionSnapshot,
    ) {
        AppDependencies(
            authRepository = authRepository,
            sessionRepository = sessionRepository,
            conversationRepository = conversationRepository,
            friendRepository = friendRepository,
            mediaRepository = mediaRepository,
            paymentRepository = paymentRepository,
            realtimeClient = realtimeClient,
            appShellCoordinator = appShellCoordinator,
            appSessionCoordinator = appSessionCoordinator,
            authCoordinator = authCoordinator,
            contactsCoordinator = contactsCoordinator,
            newFriendsCoordinator = newFriendsCoordinator,
            chatCoordinator = chatCoordinator,
            walletRequestCoordinator = walletRequestCoordinator,
            realtimeCoordinator = realtimeCoordinator,
            initialSessionSnapshot = initialSessionSnapshot,
        )
    }
}
