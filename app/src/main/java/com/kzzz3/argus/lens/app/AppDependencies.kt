package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.data.auth.AuthRepository
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
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
    val initialSessionCredentials: SessionCredentials,
    val sessionCredentialsStore: SessionCredentialsStore,
)
