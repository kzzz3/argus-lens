package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kzzz3.argus.lens.data.friend.FriendEntry
import com.kzzz3.argus.lens.data.friend.FriendRequestsSnapshot
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.feature.auth.AuthEntryUiState
import com.kzzz3.argus.lens.feature.auth.AuthFormState
import com.kzzz3.argus.lens.feature.auth.createAuthEntryUiState
import com.kzzz3.argus.lens.feature.call.CallSessionState
import com.kzzz3.argus.lens.feature.call.CallSessionUiState
import com.kzzz3.argus.lens.feature.call.createCallSessionUiState
import com.kzzz3.argus.lens.feature.contacts.ContactsState
import com.kzzz3.argus.lens.feature.contacts.ContactsUiState
import com.kzzz3.argus.lens.feature.contacts.NewFriendsUiState
import com.kzzz3.argus.lens.feature.contacts.createContactsUiState
import com.kzzz3.argus.lens.feature.inbox.ChatState
import com.kzzz3.argus.lens.feature.inbox.ChatUiState
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.inbox.InboxUiState
import com.kzzz3.argus.lens.feature.inbox.createChatUiState
import com.kzzz3.argus.lens.feature.inbox.createInboxUiState
import com.kzzz3.argus.lens.feature.me.MeUiState
import com.kzzz3.argus.lens.feature.me.createMeUiState
import com.kzzz3.argus.lens.feature.register.RegisterFormState
import com.kzzz3.argus.lens.feature.register.RegisterUiState
import com.kzzz3.argus.lens.feature.register.createRegisterUiState
import com.kzzz3.argus.lens.feature.realtime.buildRealtimeStatusLabel
import com.kzzz3.argus.lens.feature.wallet.WalletState
import com.kzzz3.argus.lens.feature.wallet.WalletUiState
import com.kzzz3.argus.lens.feature.wallet.createWalletUiState
import com.kzzz3.argus.lens.model.session.AppSessionState

internal data class AppRouteUiState(
    val authState: AuthEntryUiState,
    val registerState: RegisterUiState,
    val inboxState: InboxUiState,
    val contactsUiState: ContactsUiState,
    val chatState: ChatState?,
    val chatUiState: ChatUiState?,
    val callSessionUiState: CallSessionUiState,
    val walletUiState: WalletUiState,
    val meUiState: MeUiState,
    val newFriendsUiState: NewFriendsUiState,
)

@Composable
internal fun rememberAppRouteUiState(
    appSessionState: AppSessionState,
    conversationThreadsState: ConversationThreadsState,
    realtimeConnectionState: ConversationRealtimeConnectionState,
    authFormState: AuthFormState,
    registerFormState: RegisterFormState,
    callSessionState: CallSessionState,
    contactsState: ContactsState,
    walletStateModel: WalletState,
    friends: List<FriendEntry>,
    selectedConversationId: String,
    chatStatusMessage: String?,
    chatStatusError: Boolean,
    friendRequestsSnapshot: FriendRequestsSnapshot,
    friendRequestsStatusMessage: String?,
    friendRequestsStatusError: Boolean,
): AppRouteUiState {
    val conversationThreads = conversationThreadsState.threads
    val authState = remember(authFormState) {
        createAuthEntryUiState(formState = authFormState)
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
    val contactsUiState = remember(contactsState, friends, conversationThreads, appSessionState.accountId) {
        createContactsUiState(
            state = contactsState,
            friends = friends,
            threads = conversationThreads,
            currentAccountId = appSessionState.accountId,
        )
    }
    val selectedConversation = remember(selectedConversationId, conversationThreads) {
        conversationThreads.firstOrNull { it.id == selectedConversationId }
    }
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
        createMeUiState(
            sessionState = appSessionState,
            walletState = walletStateModel,
            friends = friends,
            conversationThreads = conversationThreads,
            shellStatusLabel = shellStatusLabel,
            shellStatusSummary = shellStatusSummary,
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

    return AppRouteUiState(
        authState = authState,
        registerState = registerState,
        inboxState = inboxState,
        contactsUiState = contactsUiState,
        chatState = chatState,
        chatUiState = chatUiState,
        callSessionUiState = callSessionUiState,
        walletUiState = walletUiState,
        meUiState = meUiState,
        newFriendsUiState = newFriendsUiState,
    )
}
