package com.kzzz3.argus.lens.app.di

import android.content.Context
import com.kzzz3.argus.lens.app.AppSessionRefresher
import com.kzzz3.argus.lens.app.AppShellUseCases
import com.kzzz3.argus.lens.app.composition.AppDependencies
import com.kzzz3.argus.lens.core.data.auth.AuthRepository
import com.kzzz3.argus.lens.core.data.auth.createAuthRepository
import com.kzzz3.argus.lens.core.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.core.data.conversation.createConversationRepository
import com.kzzz3.argus.lens.core.data.friend.FriendRepository
import com.kzzz3.argus.lens.core.data.friend.createFriendRepository
import com.kzzz3.argus.lens.core.data.media.MediaRepository
import com.kzzz3.argus.lens.core.data.media.createMediaRepository
import com.kzzz3.argus.lens.core.data.payment.PaymentRepository
import com.kzzz3.argus.lens.core.data.payment.createPaymentRepository
import com.kzzz3.argus.lens.core.data.realtime.createConversationRealtimeClient
import com.kzzz3.argus.lens.core.data.session.createInitialSessionCredentials
import com.kzzz3.argus.lens.core.data.session.createInitialSessionSnapshot
import com.kzzz3.argus.lens.core.data.session.createSessionRepository
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
    ): SessionRepository = createSessionRepository(context)

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
    ): SessionCredentials = createInitialSessionCredentials(context)

    @Provides
    @Singleton
    fun provideSessionCredentialsStore(
        initialCredentials: SessionCredentials,
    ): SessionCredentialsStore = SessionCredentialsStore(initialCredentials)

    @Provides
    @Singleton
    fun provideAppShellUseCases(
        sessionRepository: SessionRepository,
        conversationRepository: ConversationRepository,
        paymentRepository: PaymentRepository,
        backgroundSyncScheduler: BackgroundSyncScheduler,
    ): AppShellUseCases = AppShellUseCases(
        sessionRepository,
        conversationRepository,
        paymentRepository,
        backgroundSyncScheduler,
    )

    @Provides
    @Singleton
    fun provideAppSessionRefresher(
        authRepository: AuthRepository,
        sessionRepository: SessionRepository,
    ): AppSessionRefresher = AppSessionRefresher(authRepository, sessionRepository)

    @Provides
    @Singleton
    fun provideAuthUseCases(authRepository: AuthRepository): AuthUseCases = AuthUseCases(authRepository)

    @Provides
    @Singleton
    fun provideContactsUseCases(
        conversationRepository: ConversationRepository,
        friendRepository: FriendRepository,
    ): ContactsUseCases = ContactsUseCases(conversationRepository, friendRepository)

    @Provides
    @Singleton
    fun provideFriendRequestUseCases(
        friendRepository: FriendRepository,
        conversationRepository: ConversationRepository,
    ): FriendRequestUseCases = FriendRequestUseCases(friendRepository, conversationRepository)

    @Provides
    @Singleton
    fun provideChatUseCases(
        conversationRepository: ConversationRepository,
        mediaRepository: MediaRepository,
    ): ChatUseCases = ChatUseCases(conversationRepository, mediaRepository)

    @Provides
    @Singleton
    fun provideWalletUseCases(paymentRepository: PaymentRepository): WalletUseCases =
        WalletUseCases(paymentRepository)

    @Provides
    @Singleton
    fun provideApplyRealtimeConversationEventUseCase(
        conversationRepository: ConversationRepository,
    ): ApplyRealtimeConversationEventUseCase = ApplyRealtimeConversationEventUseCase(conversationRepository)

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
        appShellUseCases: AppShellUseCases,
        appSessionRefresher: AppSessionRefresher,
        authUseCases: AuthUseCases,
        contactsUseCases: ContactsUseCases,
        friendRequestUseCases: FriendRequestUseCases,
        chatUseCases: ChatUseCases,
        walletUseCases: WalletUseCases,
        applyRealtimeConversationEventUseCase: ApplyRealtimeConversationEventUseCase,
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
            appShellUseCases = appShellUseCases,
            appSessionRefresher = appSessionRefresher,
            authUseCases = authUseCases,
            contactsUseCases = contactsUseCases,
            friendRequestUseCases = friendRequestUseCases,
            chatUseCases = chatUseCases,
            walletUseCases = walletUseCases,
            applyRealtimeConversationEventUseCase = applyRealtimeConversationEventUseCase,
            initialSessionSnapshot = createInitialSessionSnapshot(context),
            initialSessionCredentials = initialCredentials,
            sessionCredentialsStore = sessionCredentialsStore,
        )
    }
}
