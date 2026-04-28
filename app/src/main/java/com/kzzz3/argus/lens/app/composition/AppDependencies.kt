package com.kzzz3.argus.lens.app.composition

import com.kzzz3.argus.lens.app.AppSessionRefresher
import com.kzzz3.argus.lens.app.AppShellUseCases
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.friend.FriendRepository
import com.kzzz3.argus.lens.core.data.media.MediaRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionCredentialsStore
import com.kzzz3.argus.lens.session.SessionRepository
import com.kzzz3.argus.lens.feature.auth.AuthUseCases
import com.kzzz3.argus.lens.feature.contacts.ContactsUseCases
import com.kzzz3.argus.lens.feature.contacts.FriendRequestUseCases
import com.kzzz3.argus.lens.feature.inbox.ChatUseCases
import com.kzzz3.argus.lens.feature.realtime.ApplyRealtimeConversationEventUseCase
import com.kzzz3.argus.lens.feature.wallet.WalletUseCases
import com.kzzz3.argus.lens.model.session.AppSessionState

data class AppDependencies(
    val authRepository: AuthRepository,
    val sessionRepository: SessionRepository,
    val conversationRepository: ConversationRepository,
    val friendRepository: FriendRepository,
    val mediaRepository: MediaRepository,
    val paymentRepository: PaymentRepository,
    val realtimeClient: ConversationRealtimeClient,
    val appShellUseCases: AppShellUseCases,
    val appSessionRefresher: AppSessionRefresher,
    val authUseCases: AuthUseCases,
    val contactsUseCases: ContactsUseCases,
    val friendRequestUseCases: FriendRequestUseCases,
    val chatUseCases: ChatUseCases,
    val walletUseCases: WalletUseCases,
    val applyRealtimeConversationEventUseCase: ApplyRealtimeConversationEventUseCase,
    val initialSessionSnapshot: AppSessionState,
    val initialSessionCredentials: SessionCredentials,
    val sessionCredentialsStore: SessionCredentialsStore,
)
