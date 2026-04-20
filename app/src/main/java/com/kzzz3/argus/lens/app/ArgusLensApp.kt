package com.kzzz3.argus.lens.app

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.app.session.AppSessionState
import com.kzzz3.argus.lens.app.session.createAuthenticatedSession
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.conversation.ConversationRepository
import com.kzzz3.argus.lens.data.conversation.buildDirectConversationId
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.friend.FriendRepository
import com.kzzz3.argus.lens.data.friend.FriendRepositoryResult
import com.kzzz3.argus.lens.data.media.MediaRepository
import com.kzzz3.argus.lens.data.media.MediaRepositoryResult
import com.kzzz3.argus.lens.data.media.FinalizedAttachmentMetadata
import com.kzzz3.argus.lens.data.payment.PaymentRepository
import com.kzzz3.argus.lens.data.payment.PaymentRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeEvent
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.data.session.createLocalSessionSnapshot
import com.kzzz3.argus.lens.feature.auth.AuthEntryAction
import com.kzzz3.argus.lens.feature.auth.AuthEntryEffect
import com.kzzz3.argus.lens.feature.auth.AuthEntryScreen
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.auth.reduceAuthFormState
import com.kzzz3.argus.lens.feature.call.CallSessionAction
import com.kzzz3.argus.lens.feature.call.CallSessionMode
import com.kzzz3.argus.lens.feature.call.CallSessionScreen
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionStatus
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.call.createInitialCallSessionState
import com.kzzz3.argus.lens.feature.call.reduceCallSessionState
import com.kzzz3.argus.lens.feature.contacts.ContactsAction
import com.kzzz3.argus.lens.feature.contacts.ContactsEffect
import com.kzzz3.argus.lens.feature.contacts.ContactsScreen
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsAction
import com.kzzz3.argus.lens.feature.contacts.NewFriendsScreen
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.contacts.reduceContactsState
import com.kzzz3.argus.lens.feature.inbox.ChatAction
import com.kzzz3.argus.lens.feature.inbox.ChatMessageAttachment
import com.kzzz3.argus.lens.feature.inbox.ChatCallMode
import com.kzzz3.argus.lens.feature.inbox.ChatEffect
import com.kzzz3.argus.lens.feature.inbox.ChatMessageDeliveryStatus
import com.kzzz3.argus.lens.feature.inbox.ChatMessageItem
import com.kzzz3.argus.lens.feature.inbox.ChatScreen
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatDraftAttachmentKind
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxAction
import com.kzzz3.argus.lens.feature.inbox.InboxConversationThread
import com.kzzz3.argus.lens.feature.inbox.InboxScreen
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.inbox.reduceChatState
import com.kzzz3.argus.lens.feature.me.MeScreen
import com.kzzz3.argus.lens.feature.me.MeStatCardUi
import com.kzzz3.argus.lens.feature.me.MeUiState
import com.kzzz3.argus.lens.feature.register.RegisterAction
import com.kzzz3.argus.lens.feature.register.RegisterEffect
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterScreen
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.register.reduceRegisterFormState
import com.kzzz3.argus.lens.feature.wallet.WalletAction
import com.kzzz3.argus.lens.feature.wallet.WalletEffect
import com.kzzz3.argus.lens.feature.wallet.WalletScreen
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.feature.wallet.reduceWalletState
import com.kzzz3.argus.lens.feature.wallet.withConfirmFailure
import com.kzzz3.argus.lens.feature.wallet.withConfirmedPayment
import com.kzzz3.argus.lens.feature.wallet.withCurrentAccount
import com.kzzz3.argus.lens.feature.wallet.withHistoryFailure
import com.kzzz3.argus.lens.feature.wallet.withHistoryLoaded
import com.kzzz3.argus.lens.feature.wallet.withReceiptFailure
import com.kzzz3.argus.lens.feature.wallet.withReceiptLoaded
import com.kzzz3.argus.lens.feature.wallet.withResolveFailure
import com.kzzz3.argus.lens.feature.wallet.withResolvedPayment
import com.kzzz3.argus.lens.feature.wallet.withWalletSummaryFailure
import com.kzzz3.argus.lens.feature.wallet.withWalletSummaryLoaded
import com.kzzz3.argus.lens.ui.shell.AuthenticatedShell
import com.kzzz3.argus.lens.ui.theme.ImBackground
import com.kzzz3.argus.lens.ui.theme.ImGreen
import com.kzzz3.argus.lens.ui.theme.ImSurfaceElevated
import com.kzzz3.argus.lens.ui.theme.ImTextPrimary
import com.kzzz3.argus.lens.ui.theme.ImTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.compose.ui.unit.dp
import kotlin.text.Charsets

@Composable
fun ArgusLensApp() {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val dependencies = rememberAppDependencies(context)
    val authRepository = dependencies.authRepository
    val conversationRepository: ConversationRepository = dependencies.conversationRepository
    val friendRepository: FriendRepository = dependencies.friendRepository
    val mediaRepository: MediaRepository = dependencies.mediaRepository
    val paymentRepository: PaymentRepository = dependencies.paymentRepository
    val realtimeClient = dependencies.realtimeClient
    val appShellCoordinator = dependencies.appShellCoordinator
    val previewThreadsState = remember {
        conversationRepository.createPreviewState(currentUserDisplayName = DEFAULT_PREVIEW_DISPLAY_NAME)
    }
    val initialSessionSnapshot = remember(context) { createLocalSessionSnapshot(context) }
    var currentRoute by rememberSaveable {
        mutableStateOf(
            if (initialSessionSnapshot.isAuthenticated && initialSessionSnapshot.accessToken.isNotBlank()) AppRoute.Inbox else AppRoute.AuthEntry
        )
    }
    var authFormState by rememberSaveable {
        mutableStateOf(AuthFormState())
    }
    var registerFormState by rememberSaveable {
        mutableStateOf(RegisterFormState())
    }
    var appSessionState by rememberSaveable {
        mutableStateOf(initialSessionSnapshot)
    }
    var contactsStateModel by rememberSaveable {
        mutableStateOf(ContactsState())
    }
    var callSessionState by rememberSaveable {
        mutableStateOf(CallSessionState())
    }
    var walletStateModel by rememberSaveable {
        mutableStateOf(WalletState())
    }
    var selectedConversationId by rememberSaveable { mutableStateOf("") }
    var chatStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var chatStatusError by rememberSaveable { mutableStateOf(false) }
    var friendRequestsSnapshot by remember { mutableStateOf(FriendRequestsSnapshot(emptyList(), emptyList())) }
    var friendRequestsStatusMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var friendRequestsStatusError by rememberSaveable { mutableStateOf(false) }
    var callSessionJob by remember { mutableStateOf<Job?>(null) }
    var hydratedConversationAccountId by remember {
        mutableStateOf(if (initialSessionSnapshot.isAuthenticated) initialSessionSnapshot.accountId else null)
    }
    var hydratedSession by remember { mutableStateOf(true) }
    var realtimeSubscription by remember { mutableStateOf<ConversationRealtimeSubscription?>(null) }
    var realtimeConnectionState by remember { mutableStateOf(ConversationRealtimeConnectionState.DISABLED) }
    var realtimeLastEventId by rememberSaveable { mutableStateOf("") }
    var realtimeReconnectAttempt by remember { mutableStateOf(0) }
    var realtimeReconnectGeneration by remember { mutableStateOf(0) }
    var realtimeReconnectJob by remember { mutableStateOf<Job?>(null) }
    var sessionRefreshJob by remember { mutableStateOf<Job?>(null) }
    var walletRequestGeneration by remember { mutableStateOf(0) }
    var activeRealtimeConnectionId by remember { mutableStateOf("") }
    val realtimeEventMutex = remember { Mutex() }
    val walletRequestJobs = remember { mutableSetOf<Job>() }
    var friends by remember { mutableStateOf<List<FriendEntry>>(emptyList()) }
    var conversationThreadsState by remember {
        mutableStateOf(previewThreadsState)
    }
    val conversationThreads = conversationThreadsState.threads

    val authState = remember(authFormState) {
        createAuthEntryUiState(
            formState = authFormState,
        )
    }
    val registerState = remember(registerFormState) {
        createRegisterUiState(registerFormState)
    }
    val sessionDisplayName = remember(appSessionState.displayName) {
        resolvePreviewDisplayName(appSessionState.displayName)
    }
    val shellStatusLabel = remember(appSessionState.isAuthenticated, realtimeConnectionState) {
        when {
            !appSessionState.isAuthenticated -> "Signed out"
            realtimeConnectionState == ConversationRealtimeConnectionState.LIVE -> "Online"
            realtimeConnectionState == ConversationRealtimeConnectionState.RECOVERING -> "Reconnecting"
            realtimeConnectionState == ConversationRealtimeConnectionState.CONNECTING -> "Connecting"
            else -> "Offline"
        }
    }
    val shellStatusSummary = remember(appSessionState.isAuthenticated, realtimeConnectionState) {
        when {
            !appSessionState.isAuthenticated -> "Sign in to enter the Argus IM shell."
            realtimeConnectionState == ConversationRealtimeConnectionState.LIVE -> "Realtime channel connected and syncing now."
            realtimeConnectionState == ConversationRealtimeConnectionState.RECOVERING -> "Network unavailable or connection interrupted. Reconnecting now."
            realtimeConnectionState == ConversationRealtimeConnectionState.CONNECTING -> "Connecting secure realtime channel..."
            else -> "Cached shell is available offline. Sign in again if your session was revoked or wait for the network to recover."
        }
    }
    val inboxState = remember(appSessionState, conversationThreads, realtimeConnectionState, shellStatusLabel) {
        createInboxUiState(
            sessionState = appSessionState,
            threads = conversationThreads,
            realtimeStatusLabel = buildRealtimeStatusLabel(realtimeConnectionState),
            shellStatusLabel = shellStatusLabel,
        )
    }
    val contactsState = remember(contactsStateModel, friends, conversationThreads, appSessionState.accountId) {
        createContactsUiState(
            state = contactsStateModel,
            friends = friends,
            threads = conversationThreads,
            currentAccountId = appSessionState.accountId,
        )
    }
    val selectedConversation = remember(selectedConversationId, conversationThreads) {
        conversationThreads.firstOrNull { it.id == selectedConversationId }
    }
    val latestConversationThreadsState by rememberUpdatedState(conversationThreadsState)
    val latestSelectedConversationId by rememberUpdatedState(selectedConversationId)
    val latestAppSessionState by rememberUpdatedState(appSessionState)
    val latestCurrentRoute by rememberUpdatedState(currentRoute)
    val latestRealtimeEnabled by rememberUpdatedState(
        hydratedSession && appSessionState.isAuthenticated && appSessionState.accessToken.isNotBlank()
    )
    val chatState = remember(selectedConversation, sessionDisplayName) {
        selectedConversation?.let { conversation ->
            ChatState(
                conversationId = conversation.id,
                conversationTitle = conversation.title,
                conversationSubtitle = conversation.subtitle,
                currentUserDisplayName = sessionDisplayName,
                messages = conversation.messages,
                draftMessage = conversation.draftMessage,
                draftAttachments = conversation.draftAttachments,
                isVoiceRecording = conversation.isVoiceRecording,
                voiceRecordingSeconds = conversation.voiceRecordingSeconds,
            )
        }
    }
    val chatUiState = remember(chatState, chatStatusMessage, chatStatusError) {
        chatState?.let {
            createChatUiState(
                state = it,
                statusMessage = chatStatusMessage,
                isStatusError = chatStatusError,
            )
        }
    }
    val callSessionUiState = remember(callSessionState) {
        createCallSessionUiState(callSessionState)
    }
    val walletUiState = remember(walletStateModel) {
        createWalletUiState(walletStateModel)
    }
    val meUiState = remember(
        appSessionState,
        walletStateModel.summary,
        friends,
        conversationThreads,
        shellStatusLabel,
        shellStatusSummary,
    ) {
        MeUiState(
            displayName = appSessionState.displayName.ifBlank { "Argus User" },
            accountId = appSessionState.accountId.ifBlank { "offline-preview" },
            walletSummary = walletStateModel.summary?.let { summary ->
                "Wallet balance · ${summary.currency} ${summary.balance}"
            } ?: "Wallet balance · Not synced yet",
            statusLabel = shellStatusLabel,
            summaryLine = shellStatusSummary,
            cards = listOf(
                MeStatCardUi(
                    title = "Chats",
                    value = "${conversationThreads.size} active threads",
                    supporting = "Open the shell instantly from cache, then let realtime catch up in the background.",
                ),
                MeStatCardUi(
                    title = "Contacts",
                    value = "${friends.size} saved friends",
                    supporting = "Friend links stay ready so you can jump into direct conversations without hunting through menus.",
                ),
            ),
        )
    }
    val newFriendsUiState = remember(friendRequestsSnapshot, friendRequestsStatusMessage, friendRequestsStatusError) {
        NewFriendsUiState(
            title = "New Friends",
            subtitle = "Review incoming requests and track the status of requests you have sent.",
            isLoading = false,
            statusMessage = friendRequestsStatusMessage,
            isStatusError = friendRequestsStatusError,
            incoming = friendRequestsSnapshot.incoming,
            outgoing = friendRequestsSnapshot.outgoing,
        )
    }

    LaunchedEffect(initialSessionSnapshot.isAuthenticated, initialSessionSnapshot.accountId, initialSessionSnapshot.accessToken) {
        if (initialSessionSnapshot.isAuthenticated && initialSessionSnapshot.accountId.isNotBlank()) {
            conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                accountId = initialSessionSnapshot.accountId,
                currentUserDisplayName = resolvePreviewDisplayName(initialSessionSnapshot.displayName),
            )
            hydratedConversationAccountId = initialSessionSnapshot.accountId
            currentRoute = AppRoute.Inbox
            return@LaunchedEffect
        }

        val hydratedState = appShellCoordinator.hydrateAppState(previewThreadsState)
        appSessionState = hydratedState.session
        conversationThreadsState = hydratedState.conversationThreadsState
        hydratedConversationAccountId = hydratedState.hydratedConversationAccountId
        if (hydratedState.session.isAuthenticated) {
            currentRoute = AppRoute.Inbox
        }
    }

    LaunchedEffect(hydratedSession, appSessionState) {
        appShellCoordinator.persistSession(hydratedSession, appSessionState)
    }

    LaunchedEffect(selectedConversationId) {
        chatStatusMessage = null
        chatStatusError = false
    }

    fun scheduleRealtimeReconnect() {
        if (!latestRealtimeEnabled) return
        if (realtimeReconnectJob?.isActive == true) return
        val nextAttempt = realtimeReconnectAttempt + 1
        realtimeReconnectAttempt = nextAttempt
        realtimeConnectionState = ConversationRealtimeConnectionState.RECOVERING
        realtimeReconnectJob = coroutineScope.launch {
            delay(realtimeReconnectDelayMillis(nextAttempt))
            if (latestRealtimeEnabled) {
                realtimeReconnectGeneration += 1
            }
        }
    }

    fun openTopLevelRoute(route: AppRoute) {
        if (route == AppRoute.Wallet) {
            walletStateModel = walletStateModel.withCurrentAccount(appSessionState.accountId)
        }
        currentRoute = route
    }

    fun invalidateWalletRequests() {
        walletRequestGeneration += 1
        walletRequestJobs.toList().forEach { it.cancel() }
        walletRequestJobs.clear()
    }

    fun isActiveWalletRequest(accountId: String, generation: Int): Boolean {
        return shouldApplyWalletRequestResult(
            currentSession = appSessionState,
            requestAccountId = accountId,
            requestGeneration = generation,
            activeGeneration = walletRequestGeneration,
        )
    }

    fun launchWalletRequest(block: suspend (String, Int) -> Unit) {
        val requestAccountId = appSessionState.accountId
        val requestGeneration = walletRequestGeneration
        val job = coroutineScope.launch {
            block(requestAccountId, requestGeneration)
        }
        walletRequestJobs += job
        job.invokeOnCompletion {
            walletRequestJobs.remove(job)
        }
    }

    fun signOutToEntry(message: String? = null) {
        val signedOutState = appShellCoordinator.createSignedOutState(previewThreadsState)
        val signedOutAccountId = appSessionState.accountId
        sessionRefreshJob?.cancel()
        sessionRefreshJob = null
        invalidateWalletRequests()
        paymentRepository.clearLocalData(signedOutAccountId)
        appSessionState = AppSessionState()
        authFormState = signedOutState.authFormState.copy(submitResult = message)
        registerFormState = signedOutState.registerFormState
        contactsStateModel = signedOutState.contactsState
        callSessionJob?.cancel()
        callSessionState = signedOutState.callSessionState
        walletStateModel = WalletState()
        hydratedConversationAccountId = null
        conversationThreadsState = signedOutState.conversationThreadsState
        selectedConversationId = signedOutState.selectedConversationId
        friends = emptyList()
        friendRequestsSnapshot = FriendRequestsSnapshot(emptyList(), emptyList())
        friendRequestsStatusMessage = null
        friendRequestsStatusError = false
        currentRoute = AppRoute.AuthEntry
    }

    suspend fun refreshSessionTokens(): AuthRepositoryResult {
        val refreshToken = appSessionState.refreshToken
        if (refreshToken.isBlank()) {
            return AuthRepositoryResult.Failure(
                code = "INVALID_CREDENTIALS",
                message = "Refresh token is missing.",
                kind = AuthFailureKind.UNAUTHORIZED,
            )
        }
        val refreshResult = authRepository.refreshSession(refreshToken)
        if (refreshResult is AuthRepositoryResult.Success) {
            appSessionState = createSessionFromAuthSession(
                session = refreshResult.session,
                fallbackRefreshToken = appSessionState.refreshToken,
            )
        }
        return refreshResult
    }

    fun scheduleSessionRefreshLoop() {
        if (sessionRefreshJob?.isActive == true) return
        if (!appSessionState.isAuthenticated || appSessionState.refreshToken.isBlank()) return
        sessionRefreshJob = coroutineScope.launch {
            while (appSessionState.isAuthenticated && realtimeConnectionState == ConversationRealtimeConnectionState.LIVE) {
                delay(60 * 60 * 1000L)
                if (!appSessionState.isAuthenticated || appSessionState.refreshToken.isBlank()) {
                    break
                }
                when (val refreshResult = authRepository.refreshSession(appSessionState.refreshToken)) {
                    is AuthRepositoryResult.Success -> {
                        appSessionState = createSessionFromAuthSession(
                            session = refreshResult.session,
                            fallbackRefreshToken = appSessionState.refreshToken,
                        )
                    }
                    is AuthRepositoryResult.Failure -> {
                        if (refreshResult.kind == AuthFailureKind.UNAUTHORIZED) {
                            signOutToEntry("Session expired or was revoked. Please sign in again.")
                            break
                        }
                    }
                }
            }
            sessionRefreshJob = null
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, appSessionState.displayName) {
        if (hydratedSession && appSessionState.isAuthenticated) {
            hydratedConversationAccountId = null
            val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
            conversationThreadsState = signedInState.conversationThreadsState
            hydratedConversationAccountId = signedInState.hydratedConversationAccountId
        } else {
            hydratedConversationAccountId = null
        }
    }

    LaunchedEffect(hydratedSession, appSessionState.isAuthenticated, appSessionState.accessToken, realtimeReconnectGeneration) {
        activeRealtimeConnectionId = ""
        realtimeSubscription?.close()
        realtimeSubscription = null

        val realtimeEnabled = hydratedSession && appSessionState.isAuthenticated && appSessionState.accessToken.isNotBlank()
        if (!realtimeEnabled) {
            realtimeReconnectJob?.cancel()
            realtimeReconnectJob = null
            realtimeReconnectAttempt = 0
            realtimeLastEventId = ""
            realtimeConnectionState = ConversationRealtimeConnectionState.DISABLED
            return@LaunchedEffect
        }

        val connectionId = "realtime-${appSessionState.accountId}-${realtimeReconnectGeneration}"
        activeRealtimeConnectionId = connectionId
        realtimeConnectionState = if (realtimeReconnectAttempt > 0) {
            ConversationRealtimeConnectionState.RECOVERING
        } else {
            ConversationRealtimeConnectionState.CONNECTING
        }
        realtimeSubscription = realtimeClient.connect(
            accessToken = appSessionState.accessToken,
            lastEventId = realtimeLastEventId.ifBlank { null },
            onConnected = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        realtimeConnectionState = ConversationRealtimeConnectionState.LIVE
                        realtimeReconnectAttempt = 0
                        realtimeReconnectJob?.cancel()
                        realtimeReconnectJob = null
                        scheduleSessionRefreshLoop()
                    }
                }
            },
            onClosed = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        scheduleRealtimeReconnect()
                    }
                }
            },
            onEvent = { event ->
                coroutineScope.launch {
                    if (activeRealtimeConnectionId != connectionId) return@launch
                    if (event.eventId.isNotBlank()) {
                        realtimeLastEventId = event.eventId
                    }
                    if (event.eventType == REALTIME_EVENT_STREAM_READY) {
                        realtimeConnectionState = ConversationRealtimeConnectionState.LIVE
                        return@launch
                    }
                    if (event.eventType == REALTIME_EVENT_HEARTBEAT) {
                        return@launch
                    }
                    realtimeEventMutex.withLock {
                        conversationThreadsState = handleConversationRealtimeEvent(
                            event = event,
                            conversationRepository = conversationRepository,
                            session = latestAppSessionState,
                            currentState = conversationThreadsState,
                            selectedConversationId = latestSelectedConversationId,
                            isChatRouteActive = latestCurrentRoute == AppRoute.Chat,
                        )
                    }
                }
            },
            onError = {
                coroutineScope.launch {
                    if (activeRealtimeConnectionId == connectionId) {
                        if (isSseAuthFailure(it)) {
                            when (val refreshResult = refreshSessionTokens()) {
                                is AuthRepositoryResult.Success -> {
                                    realtimeReconnectAttempt = 0
                                    realtimeReconnectGeneration += 1
                                }
                                is AuthRepositoryResult.Failure -> {
                                    if (refreshResult.kind == AuthFailureKind.UNAUTHORIZED) {
                                        signOutToEntry("Session expired or was revoked. Please sign in again.")
                                    } else {
                                        scheduleRealtimeReconnect()
                                    }
                                }
                            }
                        } else {
                            scheduleRealtimeReconnect()
                        }
                    }
                }
            },
        )
    }

    DisposableEffect(realtimeClient) {
        onDispose {
            activeRealtimeConnectionId = ""
            realtimeReconnectJob?.cancel()
            realtimeReconnectJob = null
            sessionRefreshJob?.cancel()
            sessionRefreshJob = null
            realtimeSubscription?.close()
            realtimeSubscription = null
        }
    }

    LaunchedEffect(appSessionState.isAuthenticated, appSessionState.accountId, hydratedConversationAccountId, conversationThreadsState) {
        appShellCoordinator.persistConversationThreads(
            session = appSessionState,
            hydratedConversationAccountId = hydratedConversationAccountId,
            state = conversationThreadsState,
        )
    }

    LaunchedEffect(currentRoute, appSessionState.isAuthenticated) {
        if (currentRoute == AppRoute.Contacts && appSessionState.isAuthenticated) {
            when (val friendResult = friendRepository.listFriends()) {
                is FriendRepositoryResult.FriendsSuccess -> friends = friendResult.friends
                is FriendRepositoryResult.Failure -> Unit
                else -> Unit
            }
        }
        if (currentRoute == AppRoute.NewFriends && appSessionState.isAuthenticated) {
            when (val requestsResult = friendRepository.listFriendRequests()) {
                is FriendRepositoryResult.RequestsSuccess -> {
                    friendRequestsSnapshot = requestsResult.snapshot
                    friendRequestsStatusMessage = null
                    friendRequestsStatusError = false
                }
                is FriendRepositoryResult.Failure -> {
                    friendRequestsStatusMessage = requestsResult.message
                    friendRequestsStatusError = true
                }
                else -> Unit
            }
        }
    }

    if (!hydratedSession) {
        AppLaunchPlaceholder()
        return
    }

    when (currentRoute) {
        AppRoute.AuthEntry -> AuthEntryScreen(
            state = authState,
            onAction = { action ->
                val result = reduceAuthFormState(
                    currentState = authFormState,
                    action = action,
                )
                authFormState = result.formState

                when (result.effect) {
                    AuthEntryEffect.NavigateBack -> Unit
                    AuthEntryEffect.NavigateToRegister -> currentRoute = AppRoute.RegisterEntry
                    is AuthEntryEffect.SubmitPasswordLogin -> {
                        coroutineScope.launch {
                            when (
                                val authResult = authRepository.login(
                                    account = result.effect.account,
                                    password = result.effect.password,
                                )
                                ) {
                                is AuthRepositoryResult.Success -> {
                                    authFormState = completeAuthForm(
                                        formState = authFormState,
                                        submitResult = authResult.session.message,
                                    )
                                    appSessionState = createSessionFromAuthSession(authResult.session)
                                    invalidateWalletRequests()
                                    hydratedConversationAccountId = null
                                    callSessionJob?.cancel()
                                    val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
                                    val postAuthUiState = createPostAuthUiState(
                                        signedInState = signedInState,
                                        accountId = authResult.session.accountId,
                                    )
                                    callSessionState = postAuthUiState.callSessionState
                                    walletStateModel = WalletState()
                                    conversationThreadsState = postAuthUiState.conversationThreadsState
                                    hydratedConversationAccountId = postAuthUiState.hydratedConversationAccountId
                                    selectedConversationId = postAuthUiState.selectedConversationId
                                    authFormState = postAuthUiState.nextAuthFormState.copy(
                                        submitResult = authResult.session.message,
                                    )
                                    realtimeReconnectGeneration += postAuthUiState.realtimeReconnectIncrement
                                    currentRoute = AppRoute.Inbox
                                }

                                is AuthRepositoryResult.Failure -> {
                                    authFormState = authFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.message,
                                    )
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.RegisterEntry -> RegisterScreen(
            state = registerState,
            onAction = { action ->
                val result = reduceRegisterFormState(
                    currentState = registerFormState,
                    action = action,
                )
                registerFormState = result.formState

                when (result.effect) {
                    RegisterEffect.NavigateBackToLogin -> currentRoute = AppRoute.AuthEntry
                    is RegisterEffect.SubmitRegistration -> {
                        coroutineScope.launch {
                            when (
                                val authResult = authRepository.register(
                                    displayName = result.effect.displayName,
                                    account = result.effect.account,
                                    password = result.effect.password,
                                )
                                ) {
                                is AuthRepositoryResult.Success -> {
                                    registerFormState = completeRegistrationForm(
                                        formState = registerFormState,
                                        submitResult = authResult.session.message,
                                    )
                                    appSessionState = createSessionFromAuthSession(authResult.session)
                                    invalidateWalletRequests()
                                    hydratedConversationAccountId = null
                                    callSessionJob?.cancel()
                                    val signedInState = appShellCoordinator.handleSignedIn(appSessionState)
                                    val postAuthUiState = createPostAuthUiState(
                                        signedInState = signedInState,
                                        accountId = authResult.session.accountId,
                                    )
                                    callSessionState = postAuthUiState.callSessionState
                                    conversationThreadsState = postAuthUiState.conversationThreadsState
                                    hydratedConversationAccountId = postAuthUiState.hydratedConversationAccountId
                                    selectedConversationId = postAuthUiState.selectedConversationId
                                    walletStateModel = WalletState()
                                    authFormState = postAuthUiState.nextAuthFormState
                                    realtimeReconnectGeneration += postAuthUiState.realtimeReconnectIncrement
                                    currentRoute = AppRoute.Inbox
                                }

                                is AuthRepositoryResult.Failure -> {
                                    registerFormState = registerFormState.copy(
                                        isSubmitting = false,
                                        submitResult = authResult.message,
                                    )
                                }
                            }
                        }
                    }
                    null -> Unit
                }
            }
        )

        AppRoute.Inbox -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            InboxScreen(
                state = inboxState,
                onAction = { action ->
                    when (action) {
                        is InboxAction.OpenConversation -> {
                            conversationThreadsState = conversationRepository.markConversationAsRead(
                                state = conversationThreadsState,
                                conversationId = action.conversationId,
                            )
                            selectedConversationId = action.conversationId
                            currentRoute = AppRoute.Chat
                            coroutineScope.launch {
                                conversationThreadsState = synchronizeActiveConversation(
                                    state = conversationThreadsState,
                                    conversationId = action.conversationId,
                                    conversationRepository = conversationRepository,
                                )
                            }
                        }

                        InboxAction.OpenContacts -> openTopLevelRoute(AppRoute.Contacts)
                        InboxAction.OpenWallet -> openTopLevelRoute(AppRoute.Wallet)
                        InboxAction.SignOutToHud -> signOutToEntry()
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Contacts -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            ContactsScreen(
                state = contactsState,
                onAction = { action ->
                    val result = reduceContactsState(
                        currentState = contactsStateModel,
                        action = action,
                    )
                    contactsStateModel = result.state

                    when (val effect = result.effect) {
                        is ContactsEffect.OpenConversation -> {
                            coroutineScope.launch {
                                val target = resolveDirectConversationTarget(
                                    currentAccountId = appSessionState.accountId,
                                    requestedConversationId = effect.conversationId,
                                    friends = friends,
                                    existingThreadIds = conversationThreads.map { it.id }.toSet(),
                                )
                                val resolvedConversationId = if (!target.requiresRefresh && !target.requiresPlaceholder) {
                                    conversationThreadsState = conversationRepository.markConversationAsRead(
                                        state = conversationThreadsState,
                                        conversationId = target.conversationId,
                                    )
                                    target.conversationId
                                } else {
                                    val refreshedConversationId = if (appSessionState.isAuthenticated && target.requiresRefresh) {
                                        conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                                            accountId = appSessionState.accountId,
                                            currentUserDisplayName = appSessionState.displayName,
                                        )
                                        conversationThreadsState.threads.firstOrNull { thread ->
                                            thread.id == target.conversationId
                                        }?.id
                                    } else {
                                        null
                                    }
                                    if (refreshedConversationId != null) {
                                        conversationThreadsState = conversationRepository.markConversationAsRead(
                                            state = conversationThreadsState,
                                            conversationId = refreshedConversationId,
                                        )
                                        refreshedConversationId
                                    } else {
                                        conversationThreadsState = ensureDirectConversationPlaceholder(
                                            state = conversationThreadsState,
                                            conversationId = target.conversationId,
                                            title = target.placeholderTitle,
                                        )
                                        target.conversationId
                                    }
                                }
                                selectedConversationId = resolvedConversationId
                                currentRoute = AppRoute.Chat
                                conversationThreadsState = synchronizeActiveConversation(
                                    state = conversationThreadsState,
                                    conversationId = resolvedConversationId,
                                    conversationRepository = conversationRepository,
                                )
                            }
                        }

                        is ContactsEffect.AddFriend -> {
                            coroutineScope.launch {
                                when (val friendResult = friendRepository.sendFriendRequest(effect.friendAccountId)) {
                                    is FriendRepositoryResult.FriendRequestSuccess -> {
                                        val refreshed = friendRepository.listFriendRequests()
                                        contactsStateModel = createContactsStatusUpdate(
                                            currentState = contactsStateModel,
                                            message = friendResult.message ?: "Friend request sent.",
                                            isError = false,
                                        )
                                        if (refreshed is FriendRepositoryResult.RequestsSuccess) {
                                            friendRequestsSnapshot = refreshed.snapshot
                                        }
                                    }

                                    is FriendRepositoryResult.Failure -> {
                                        contactsStateModel = createContactsStatusUpdate(
                                            currentState = contactsStateModel,
                                            message = friendResult.message,
                                            isError = true,
                                        )
                                    }

                                    else -> Unit
                                }
                            }
                        }

                        ContactsEffect.OpenNewFriends -> currentRoute = AppRoute.NewFriends

                        ContactsEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.NewFriends -> AuthenticatedShell(
            currentRoute = AppRoute.Contacts,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            NewFriendsScreen(
                state = newFriendsUiState,
                onAction = { action ->
                    when (action) {
                        NewFriendsAction.NavigateBack -> currentRoute = AppRoute.Contacts
                        is NewFriendsAction.Accept -> {
                            coroutineScope.launch {
                                when (val result = friendRepository.acceptFriendRequest(action.requestId)) {
                                    is FriendRepositoryResult.FriendsSuccess -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message ?: "Friend request accepted.",
                                            isError = false,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                        when (val refreshedFriends = friendRepository.listFriends()) {
                                            is FriendRepositoryResult.FriendsSuccess -> friends = refreshedFriends.friends
                                            else -> Unit
                                        }
                                        when (val refreshedRequests = friendRepository.listFriendRequests()) {
                                            is FriendRepositoryResult.RequestsSuccess -> {
                                                friendRequestsSnapshot = createFriendRequestStatusState(
                                                    snapshot = refreshedRequests.snapshot,
                                                    message = statusState.message,
                                                    isError = statusState.isError,
                                                ).snapshot
                                            }
                                            else -> Unit
                                        }
                                        if (appSessionState.isAuthenticated) {
                                            conversationThreadsState = conversationRepository.loadOrCreateConversationThreads(
                                                accountId = appSessionState.accountId,
                                                currentUserDisplayName = appSessionState.displayName,
                                            )
                                        }
                                    }
                                    is FriendRepositoryResult.Failure -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message,
                                            isError = true,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is NewFriendsAction.Reject -> {
                            coroutineScope.launch {
                                when (val result = friendRepository.rejectFriendRequest(action.requestId)) {
                                    is FriendRepositoryResult.FriendRequestSuccess -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message ?: "Friend request rejected.",
                                            isError = false,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                        when (val refreshedRequests = friendRepository.listFriendRequests()) {
                                            is FriendRepositoryResult.RequestsSuccess -> {
                                                friendRequestsSnapshot = createFriendRequestStatusState(
                                                    snapshot = refreshedRequests.snapshot,
                                                    message = statusState.message,
                                                    isError = statusState.isError,
                                                ).snapshot
                                            }
                                            else -> Unit
                                        }
                                    }
                                    is FriendRepositoryResult.Failure -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message,
                                            isError = true,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is NewFriendsAction.Ignore -> {
                            coroutineScope.launch {
                                when (val result = friendRepository.ignoreFriendRequest(action.requestId)) {
                                    is FriendRepositoryResult.FriendRequestSuccess -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message ?: "Friend request ignored.",
                                            isError = false,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                        when (val refreshedRequests = friendRepository.listFriendRequests()) {
                                            is FriendRepositoryResult.RequestsSuccess -> {
                                                friendRequestsSnapshot = createFriendRequestStatusState(
                                                    snapshot = refreshedRequests.snapshot,
                                                    message = statusState.message,
                                                    isError = statusState.isError,
                                                ).snapshot
                                            }
                                            else -> Unit
                                        }
                                    }
                                    is FriendRepositoryResult.Failure -> {
                                        val statusState = createFriendRequestStatusState(
                                            snapshot = friendRequestsSnapshot,
                                            message = result.message,
                                            isError = true,
                                        )
                                        friendRequestsStatusMessage = statusState.message
                                        friendRequestsStatusError = statusState.isError
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.CallSession -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            CallSessionScreen(
                state = callSessionUiState,
                onAction = { action ->
                    callSessionState = reduceCallSessionState(callSessionState, action)
                    if (action == CallSessionAction.EndCall) {
                        callSessionJob?.cancel()
                        coroutineScope.launch {
                            delay(300)
                            currentRoute = AppRoute.Chat
                        }
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Wallet -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            WalletScreen(
                state = walletUiState,
                permissionRequestPending = walletStateModel.shouldRequestCameraPermission,
                onAction = { action ->
                    val result = reduceWalletState(
                        currentState = walletStateModel,
                        action = action,
                    )
                    walletStateModel = result.state

                    when (val effect = result.effect) {
                        WalletEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                        WalletEffect.LoadWalletSummary -> {
                            launchWalletRequest { requestAccountId, requestGeneration ->
                                when (val paymentResult = paymentRepository.getWalletSummary()) {
                                    is PaymentRepositoryResult.WalletSummarySuccess -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withWalletSummaryLoaded(paymentResult.summary)
                                        }
                                    }
                                    is PaymentRepositoryResult.Failure -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withWalletSummaryFailure(paymentResult.message)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is WalletEffect.ResolvePayload -> {
                            launchWalletRequest { requestAccountId, requestGeneration ->
                                when (val paymentResult = paymentRepository.resolveScanPayload(effect.payload)) {
                                    is PaymentRepositoryResult.ResolutionSuccess -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withResolvedPayment(paymentResult.resolution)
                                        }
                                    }
                                    is PaymentRepositoryResult.Failure -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withResolveFailure(paymentResult.message)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is WalletEffect.ConfirmPayment -> {
                            launchWalletRequest { requestAccountId, requestGeneration ->
                                val amount = effect.amountInput?.toDoubleOrNull()
                                if (effect.amountInput != null && amount == null) {
                                    if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                        walletStateModel = walletStateModel.withConfirmFailure("Amount must be a valid decimal value.")
                                    }
                                    return@launchWalletRequest
                                }
                                when (
                                    val paymentResult = paymentRepository.confirmPayment(
                                        sessionId = effect.sessionId,
                                        amount = amount,
                                        note = effect.note,
                                    )
                                ) {
                                    is PaymentRepositoryResult.ConfirmationSuccess -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withConfirmedPayment(paymentResult.receipt)
                                        }
                                    }
                                    is PaymentRepositoryResult.Failure -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withConfirmFailure(paymentResult.message)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        WalletEffect.LoadPaymentHistory -> {
                            launchWalletRequest { requestAccountId, requestGeneration ->
                                when (val paymentResult = paymentRepository.listPayments()) {
                                    is PaymentRepositoryResult.HistorySuccess -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withHistoryLoaded(paymentResult.history)
                                        }
                                    }
                                    is PaymentRepositoryResult.Failure -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withHistoryFailure(paymentResult.message)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        is WalletEffect.LoadPaymentReceipt -> {
                            launchWalletRequest { requestAccountId, requestGeneration ->
                                when (val paymentResult = paymentRepository.getPaymentReceipt(effect.paymentId)) {
                                    is PaymentRepositoryResult.ReceiptSuccess -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withReceiptLoaded(paymentResult.receipt)
                                        }
                                    }
                                    is PaymentRepositoryResult.Failure -> {
                                        if (isActiveWalletRequest(requestAccountId, requestGeneration)) {
                                            walletStateModel = walletStateModel.withReceiptFailure(paymentResult.message)
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        null -> Unit
                    }
                },
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Me -> AuthenticatedShell(
            currentRoute = currentRoute,
            onTabSelected = ::openTopLevelRoute,
        ) { innerPadding ->
            MeScreen(
                state = meUiState,
                onSignOut = ::signOutToEntry,
                modifier = Modifier.padding(innerPadding),
            )
        }

        AppRoute.Chat -> {
            val resolvedChatUiState = chatUiState
            val resolvedChatState = chatState

            if (resolvedChatUiState == null || resolvedChatState == null) {
                AuthenticatedShell(
                    currentRoute = AppRoute.Inbox,
                    onTabSelected = ::openTopLevelRoute,
                ) { innerPadding ->
                    InboxScreen(
                        state = inboxState,
                        onAction = { action ->
                            when (action) {
                                is InboxAction.OpenConversation -> {
                                    conversationThreadsState = conversationRepository.markConversationAsRead(
                                        state = conversationThreadsState,
                                        conversationId = action.conversationId,
                                    )
                                    selectedConversationId = action.conversationId
                                    currentRoute = AppRoute.Chat
                                    coroutineScope.launch {
                                        conversationThreadsState = synchronizeActiveConversation(
                                            state = conversationThreadsState,
                                            conversationId = action.conversationId,
                                            conversationRepository = conversationRepository,
                                        )
                                    }
                                }

                                InboxAction.OpenContacts -> openTopLevelRoute(AppRoute.Contacts)
                                InboxAction.OpenWallet -> openTopLevelRoute(AppRoute.Wallet)
                                InboxAction.SignOutToHud -> signOutToEntry()
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            } else {
                AuthenticatedShell(
                    currentRoute = currentRoute,
                    onTabSelected = ::openTopLevelRoute,
                ) { innerPadding ->
                    ChatScreen(
                        state = resolvedChatUiState,
                        onAction = { action ->
                            val result = reduceChatState(
                                currentState = resolvedChatState,
                                action = action,
                            )
                            conversationThreadsState = conversationRepository.updateConversationFromChatState(
                                state = conversationThreadsState,
                                updatedState = result.state,
                            )

                            when (result.effect) {
                                ChatEffect.NavigateBackToInbox -> openTopLevelRoute(AppRoute.Inbox)
                                is ChatEffect.StartCall -> {
                                    callSessionState = createInitialCallSessionState(
                                        conversationId = result.effect.conversationId,
                                        contactDisplayName = result.effect.contactDisplayName,
                                        mode = if (result.effect.mode == ChatCallMode.Video) {
                                            CallSessionMode.Video
                                        } else {
                                            CallSessionMode.Audio
                                        },
                                    )
                                    callSessionJob?.cancel()
                                    callSessionJob = coroutineScope.launch {
                                        delay(800)
                                        if (callSessionState.status == CallSessionStatus.Connecting) {
                                            callSessionState = callSessionState.copy(status = CallSessionStatus.Active)
                                        }
                                        while (callSessionState.status == CallSessionStatus.Active && currentRoute == AppRoute.CallSession) {
                                            delay(1000)
                                            callSessionState = callSessionState.copy(
                                                durationLabel = incrementCallDurationLabel(callSessionState.durationLabel)
                                            )
                                        }
                                    }
                                    currentRoute = AppRoute.CallSession
                                }
                                is ChatEffect.DispatchOutgoingMessages -> {
                                    coroutineScope.launch {
                                        val outgoingMessages = result.state.messages
                                            .filter { it.id in result.effect.messageIds }
                                        val conversationId = result.effect.conversationId
                                        var firstFailureMessage: String? = null

                                        outgoingMessages.forEach { outgoingMessage ->
                                            val sendResult = dispatchOutgoingChatMessage(
                                                state = conversationThreadsState,
                                                conversationId = conversationId,
                                                message = outgoingMessage,
                                                conversationRepository = conversationRepository,
                                                mediaRepository = mediaRepository,
                                            )
                                            conversationThreadsState = sendResult.state
                                            if (firstFailureMessage == null) {
                                                firstFailureMessage = sendResult.failureMessage
                                            }
                                        }

                                        if (firstFailureMessage != null) {
                                            chatStatusMessage = firstFailureMessage
                                            chatStatusError = true
                                        }
                                    }
                                }
                                null -> Unit
                            }

                            if (action is ChatAction.DownloadAttachment) {
                                coroutineScope.launch {
                                    when (val downloadResult = mediaRepository.downloadAttachment(
                                        action.attachmentId,
                                        action.fileName,
                                    )) {
                                        is MediaRepositoryResult.DownloadSuccess -> {
                                            chatStatusMessage = "Saved to ${downloadResult.savedPath}"
                                            chatStatusError = false
                                        }
                                        is MediaRepositoryResult.Failure -> {
                                            chatStatusMessage = downloadResult.message
                                            chatStatusError = true
                                        }
                                        else -> Unit
                                    }
                                }
                            }

                            if (action is ChatAction.RecallMessage) {
                                val recallableMessage = resolvedChatState.messages.firstOrNull { message ->
                                    message.id == action.messageId &&
                                        message.isFromCurrentUser &&
                                        (message.deliveryStatus == ChatMessageDeliveryStatus.Sent ||
                                            message.deliveryStatus == ChatMessageDeliveryStatus.Delivered)
                                }
                                if (recallableMessage != null) {
                                    coroutineScope.launch {
                                        conversationThreadsState = conversationRepository.recallMessage(
                                            state = conversationThreadsState,
                                            conversationId = resolvedChatState.conversationId,
                                            messageId = action.messageId,
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppLaunchPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ImBackground),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = ImSurfaceElevated.copy(alpha = 0.96f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "ARGUS",
                    style = MaterialTheme.typography.headlineSmall,
                    color = ImTextPrimary,
                )
                Text(
                    text = "Loading your workspace...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImTextSecondary,
                )
                Text(
                    text = "Restoring your last session and refreshing account state.",
                    style = MaterialTheme.typography.labelMedium,
                    color = ImGreen,
                )
            }
        }
    }
}

private fun markOutgoingMessagesFailed(
    state: ConversationThreadsState,
    conversationId: String,
    messageIds: List<String>,
): ConversationThreadsState {
    return state.copy(
        threads = state.threads.map { thread ->
            if (thread.id == conversationId) {
                thread.copy(
                    messages = thread.messages.map { message ->
                        if (message.id in messageIds) {
                            message.copy(deliveryStatus = ChatMessageDeliveryStatus.Failed)
                        } else {
                            message
                        }
                    }
                )
            } else {
                thread
            }
        }
    )
}

private fun isMediaPlaceholderBody(body: String): Boolean {
    return body.startsWith("[Image]") || body.startsWith("[Video]")
}

private fun mediaAttachmentKindFromPlaceholder(body: String): ChatDraftAttachmentKind? {
    return when {
        body.startsWith("[Image]") -> ChatDraftAttachmentKind.Image
        body.startsWith("[Video]") -> ChatDraftAttachmentKind.Video
        else -> null
    }
}

private fun mediaContentTypeFor(kind: ChatDraftAttachmentKind): String {
    return when (kind) {
        ChatDraftAttachmentKind.Image -> "image/jpeg"
        ChatDraftAttachmentKind.Video -> "video/mp4"
        ChatDraftAttachmentKind.Voice -> "audio/mpeg"
    }
}

private fun FinalizedAttachmentMetadata.toChatMessageAttachment(): ChatMessageAttachment {
    return ChatMessageAttachment(
        attachmentId = attachmentId,
        attachmentType = attachmentType,
        fileName = fileName,
        contentType = contentType,
        contentLength = contentLength,
    )
}

private data class OutgoingDispatchResult(
    val state: ConversationThreadsState,
    val failureMessage: String? = null,
)

private suspend fun dispatchOutgoingChatMessage(
    state: ConversationThreadsState,
    conversationId: String,
    message: ChatMessageItem,
    conversationRepository: ConversationRepository,
    mediaRepository: MediaRepository,
): OutgoingDispatchResult {
    val attachment = message.attachment
    if (attachment == null) {
        return OutgoingDispatchResult(
            state = conversationRepository.sendMessage(
                state = state,
                conversationId = conversationId,
                localMessageId = message.id,
                body = message.body,
                attachment = null,
            )
        )
    }

    val finalizedAttachment = if (attachment.attachmentId.isNullOrBlank()) {
        val attachmentKind = attachment.toDraftAttachmentKind()
        val fileName = attachment.fileName.ifBlank {
            buildMediaPlaceholderFileName(conversationId, message.id, attachmentKind)
        }
        when (val uploadSessionResult = mediaRepository.createUploadSession(
            conversationId = conversationId,
            attachmentKind = attachmentKind,
            fileName = fileName,
            contentType = attachment.contentType.ifBlank { mediaContentTypeFor(attachmentKind) },
            contentLength = attachment.contentLength,
            durationSeconds = null,
        )) {
            is MediaRepositoryResult.Success -> {
                val session = uploadSessionResult.session
                val placeholderBytes = buildMediaPlaceholderBytes(fileName, attachmentKind)
                when (val uploadResult = mediaRepository.uploadContent(session, placeholderBytes)) {
                    is MediaRepositoryResult.UploadSuccess -> {
                        when (val finalizeResult = mediaRepository.finalizeUploadSession(
                            sessionId = session.uploadSessionId,
                            conversationId = conversationId,
                            fileName = fileName,
                            contentType = session.contentType,
                            contentLength = placeholderBytes.size.toLong(),
                            objectKey = session.objectKey,
                        )) {
                            is MediaRepositoryResult.FinalizeSuccess -> finalizeResult.metadata.toChatMessageAttachment()
                            is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                                state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                                failureMessage = finalizeResult.message,
                            )
                            else -> null
                        }
                    }
                    is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                        state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                        failureMessage = uploadResult.message,
                    )
                    else -> null
                }
            }
            is MediaRepositoryResult.Failure -> return OutgoingDispatchResult(
                state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
                failureMessage = uploadSessionResult.message,
            )
            else -> null
        }
    } else {
        attachment
    }

    if (finalizedAttachment == null) {
        return OutgoingDispatchResult(
            state = markOutgoingMessagesFailed(state, conversationId, listOf(message.id)),
            failureMessage = "File upload failed.",
        )
    }

    return OutgoingDispatchResult(
        state = conversationRepository.sendMessage(
            state = state,
            conversationId = conversationId,
            localMessageId = message.id,
            body = finalizedAttachment.fileName,
            attachment = finalizedAttachment,
        )
    )
}

private fun ChatMessageAttachment.toDraftAttachmentKind(): ChatDraftAttachmentKind {
    return when (attachmentType.uppercase()) {
        "IMAGE" -> ChatDraftAttachmentKind.Image
        "VIDEO" -> ChatDraftAttachmentKind.Video
        else -> ChatDraftAttachmentKind.Voice
    }
}

private fun buildMediaPlaceholderBytes(fileName: String, kind: ChatDraftAttachmentKind): ByteArray {
    return "$fileName|${kind.name}".toByteArray(Charsets.UTF_8)
}

private fun buildMediaPlaceholderFileName(conversationId: String, localMessageId: String, kind: ChatDraftAttachmentKind): String {
    val extension = when (kind) {
        ChatDraftAttachmentKind.Image -> "jpg"
        ChatDraftAttachmentKind.Video -> "mp4"
        else -> "bin"
    }
    return "$conversationId-${kind.name.lowercase()}-$localMessageId.$extension"
}

private fun buildFileMessageBodyWithFinalizedAttachment(
    kind: ChatDraftAttachmentKind,
    metadata: FinalizedAttachmentMetadata,
): String {
    return buildString {
        append("[File] ")
        append(kind.name)
        append(" · ")
        append(metadata.fileName)
        append(" · Download or Save As")
        append(" · attachmentId=")
        append(metadata.attachmentId)
        append(" · objectKey=")
        append(metadata.objectKey)
        append(" · uploadUrl=")
        append(metadata.uploadUrl)
    }
}
