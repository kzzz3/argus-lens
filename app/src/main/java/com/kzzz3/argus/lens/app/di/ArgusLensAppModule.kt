package com.kzzz3.argus.lens.app.di

import android.content.Context
import com.kzzz3.argus.lens.app.AppDependencies
import com.kzzz3.argus.lens.app.AppSessionCoordinator
import com.kzzz3.argus.lens.app.AppShellCoordinator
import com.kzzz3.argus.lens.app.SessionCredentialsStore
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
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.data.session.SessionRepository
import com.kzzz3.argus.lens.data.session.createLocalSessionCredentialsSnapshot
import com.kzzz3.argus.lens.data.session.createLocalSessionSnapshot
import com.kzzz3.argus.lens.data.session.createLocalSessionStore
import com.kzzz3.argus.lens.feature.auth.AuthCoordinator
import com.kzzz3.argus.lens.feature.contacts.ContactsCoordinator
import com.kzzz3.argus.lens.feature.contacts.NewFriendsCoordinator
import com.kzzz3.argus.lens.feature.inbox.ChatCoordinator
import com.kzzz3.argus.lens.feature.realtime.RealtimeCoordinator
import com.kzzz3.argus.lens.feature.wallet.WalletRequestCoordinator
import com.kzzz3.argus.lens.worker.BackgroundSyncScheduler
import com.kzzz3.argus.lens.worker.WorkManagerBackgroundSyncScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ArgusLensAppModule {
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = createAuthRepository()

    @Provides
    @Singleton
    fun provideSessionRepository(
        @ApplicationContext context: Context,
    ): SessionRepository = createLocalSessionStore(context)

    @Provides
    @Singleton
    fun provideConversationRepository(
        @ApplicationContext context: Context,
        sessionRepository: SessionRepository,
    ): ConversationRepository = createConversationRepository(context, sessionRepository)

    @Provides
    @Singleton
    fun provideFriendRepository(
        sessionRepository: SessionRepository,
    ): FriendRepository = createFriendRepository(sessionRepository)

    @Provides
    @Singleton
    fun provideMediaRepository(
        sessionRepository: SessionRepository,
        @ApplicationContext context: Context,
    ): MediaRepository = createMediaRepository(sessionRepository, context)

    @Provides
    @Singleton
    fun providePaymentRepository(
        @ApplicationContext context: Context,
        sessionRepository: SessionRepository,
    ): PaymentRepository = createPaymentRepository(context, sessionRepository)

    @Provides
    @Singleton
    fun provideRealtimeClient(): ConversationRealtimeClient = createConversationRealtimeClient()

    @Provides
    @Singleton
    fun provideBackgroundSyncScheduler(
        @ApplicationContext context: Context,
    ): BackgroundSyncScheduler = WorkManagerBackgroundSyncScheduler(context)

    @Provides
    @Singleton
    fun provideInitialSessionCredentials(
        @ApplicationContext context: Context,
    ): SessionCredentials = createLocalSessionCredentialsSnapshot(context)

    @Provides
    @Singleton
    fun provideSessionCredentialsStore(
        initialCredentials: SessionCredentials,
    ): SessionCredentialsStore = SessionCredentialsStore(initialCredentials)

    @Provides
    @Singleton
    fun provideAppShellCoordinator(
        sessionRepository: SessionRepository,
        conversationRepository: ConversationRepository,
        paymentRepository: PaymentRepository,
        backgroundSyncScheduler: BackgroundSyncScheduler,
    ): AppShellCoordinator = AppShellCoordinator(
        sessionRepository,
        conversationRepository,
        paymentRepository,
        backgroundSyncScheduler,
    )

    @Provides
    @Singleton
    fun provideAppSessionCoordinator(
        authRepository: AuthRepository,
        sessionRepository: SessionRepository,
    ): AppSessionCoordinator = AppSessionCoordinator(authRepository, sessionRepository)

    @Provides
    @Singleton
    fun provideAuthCoordinator(authRepository: AuthRepository): AuthCoordinator = AuthCoordinator(authRepository)

    @Provides
    @Singleton
    fun provideContactsCoordinator(
        conversationRepository: ConversationRepository,
        friendRepository: FriendRepository,
    ): ContactsCoordinator = ContactsCoordinator(conversationRepository, friendRepository)

    @Provides
    @Singleton
    fun provideNewFriendsCoordinator(
        friendRepository: FriendRepository,
        conversationRepository: ConversationRepository,
    ): NewFriendsCoordinator = NewFriendsCoordinator(friendRepository, conversationRepository)

    @Provides
    @Singleton
    fun provideChatCoordinator(
        conversationRepository: ConversationRepository,
        mediaRepository: MediaRepository,
    ): ChatCoordinator = ChatCoordinator(conversationRepository, mediaRepository)

    @Provides
    @Singleton
    fun provideWalletRequestCoordinator(paymentRepository: PaymentRepository): WalletRequestCoordinator =
        WalletRequestCoordinator(paymentRepository)

    @Provides
    @Singleton
    fun provideRealtimeCoordinator(conversationRepository: ConversationRepository): RealtimeCoordinator =
        RealtimeCoordinator(conversationRepository)

    @Provides
    @Singleton
    fun provideAppDependencies(
        authRepository: AuthRepository,
        sessionRepository: SessionRepository,
        conversationRepository: ConversationRepository,
        friendRepository: FriendRepository,
        mediaRepository: MediaRepository,
        paymentRepository: PaymentRepository,
        realtimeClient: ConversationRealtimeClient,
        appShellCoordinator: AppShellCoordinator,
        appSessionCoordinator: AppSessionCoordinator,
        authCoordinator: AuthCoordinator,
        contactsCoordinator: ContactsCoordinator,
        newFriendsCoordinator: NewFriendsCoordinator,
        chatCoordinator: ChatCoordinator,
        walletRequestCoordinator: WalletRequestCoordinator,
        realtimeCoordinator: RealtimeCoordinator,
        @ApplicationContext context: Context,
        initialCredentials: SessionCredentials,
        sessionCredentialsStore: SessionCredentialsStore,
    ): AppDependencies {
        return AppDependencies(
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
            initialSessionSnapshot = createLocalSessionSnapshot(context),
            initialSessionCredentials = initialCredentials,
            sessionCredentialsStore = sessionCredentialsStore,
        )
    }
}
