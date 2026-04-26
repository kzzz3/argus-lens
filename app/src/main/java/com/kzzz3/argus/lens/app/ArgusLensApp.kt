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
    ArgusLensNavHost(
        dependencies = viewModel.dependencies,
        appSessionState = uiState.appSessionState,
        currentRoute = uiState.currentRoute,
        authFormState = uiState.authFormState,
        registerFormState = uiState.registerFormState,
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
        onConversationOpened = viewModel::openConversation,
        onConversationSelectionCleared = viewModel::clearSelectedConversation,
        onChatStatusChanged = viewModel::updateChatStatus,
        onChatStatusCleared = viewModel::clearChatStatus,
        onFriendRequestStatusChanged = viewModel::updateFriendRequestStatus,
        onFriendRequestsSnapshotChanged = viewModel::updateFriendRequestsSnapshot,
        onFriendRequestStatusReset = viewModel::resetFriendRequestStatus,
        onHydratedSessionApplied = viewModel::applyHydratedSession,
        onAuthenticatedSessionApplied = viewModel::applyAuthenticatedSession,
        onSessionRefreshed = viewModel::applyRefreshedSession,
        onSessionCleared = viewModel::clearSession,
        onHydratedConversationAccountChanged = viewModel::updateHydratedConversationAccountId,
        onRealtimeConnectionStateChanged = viewModel::updateRealtimeConnectionState,
        onRealtimeEventIdRecorded = viewModel::recordRealtimeEventId,
        onRealtimeLastEventIdReset = viewModel::resetRealtimeLastEventId,
        onRealtimeReconnectIncremented = viewModel::incrementRealtimeReconnectGeneration,
        onRealtimeReconnectIncrementedBy = viewModel::incrementRealtimeReconnectGenerationBy,
    )
}
