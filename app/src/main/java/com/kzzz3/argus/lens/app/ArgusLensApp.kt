package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArgusLensApp(
    viewModel: ArgusLensAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppRouteHost(
        dependencies = viewModel.dependencies,
        appSessionState = uiState.appSessionState,
        conversationThreadsState = uiState.conversationThreadsState,
        currentRoute = uiState.currentRoute,
        authFormState = uiState.authFormState,
        registerFormState = uiState.registerFormState,
        callSessionState = uiState.callSessionState,
        contactsState = uiState.contactsState,
        walletStateModel = uiState.walletState,
        friends = uiState.friends,
        selectedConversationId = uiState.selectedConversationId,
        chatStatusMessage = uiState.chatStatusMessage,
        chatStatusError = uiState.chatStatusError,
        friendRequestsSnapshot = uiState.friendRequestsSnapshot,
        friendRequestsStatusMessage = uiState.friendRequestsStatusMessage,
        friendRequestsStatusError = uiState.friendRequestsStatusError,
        hydratedConversationAccountId = uiState.hydratedConversationAccountId,
        realtimeConnectionState = uiState.realtimeConnectionState,
        realtimeLastEventId = uiState.realtimeLastEventId,
        realtimeReconnectGeneration = uiState.realtimeReconnectGeneration,
        onRouteChanged = viewModel::openRoute,
        onAuthFormStateChanged = viewModel::updateAuthFormState,
        onRegisterFormStateChanged = viewModel::updateRegisterFormState,
        onCallSessionStateChanged = viewModel::updateCallSessionState,
        onContactsStateChanged = viewModel::updateContactsState,
        onWalletStateChanged = viewModel::updateWalletState,
        onFriendsChanged = viewModel::updateFriends,
        onConversationOpened = viewModel::openConversation,
        onChatStatusChanged = viewModel::updateChatStatus,
        onChatStatusCleared = viewModel::clearChatStatus,
        onFriendRequestStatusChanged = viewModel::updateFriendRequestStatus,
        onFriendRequestsSnapshotChanged = viewModel::updateFriendRequestsSnapshot,
        onFriendRequestStatusReset = viewModel::resetFriendRequestStatus,
        onHydratedSessionApplied = viewModel::applyHydratedSession,
        onAuthenticatedSessionApplied = viewModel::applyAuthenticatedSession,
        onSessionRefreshed = viewModel::applyRefreshedSession,
        onSessionCleared = viewModel::clearSession,
        onConversationThreadsChanged = viewModel::updateConversationThreadsState,
        onHydratedConversationAccountChanged = viewModel::updateHydratedConversationAccountId,
        onRealtimeConnectionStateChanged = viewModel::updateRealtimeConnectionState,
        onRealtimeEventIdRecorded = viewModel::recordRealtimeEventId,
        onRealtimeLastEventIdReset = viewModel::resetRealtimeLastEventId,
        onRealtimeReconnectIncremented = viewModel::incrementRealtimeReconnectGeneration,
    )
}
