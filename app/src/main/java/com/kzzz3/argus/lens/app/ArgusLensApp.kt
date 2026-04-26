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
        currentRoute = uiState.currentRoute,
        selectedConversationId = uiState.selectedConversationId,
        hydratedConversationAccountId = uiState.hydratedConversationAccountId,
        realtimeConnectionState = uiState.realtimeConnectionState,
        realtimeLastEventId = uiState.realtimeLastEventId,
        realtimeReconnectGeneration = uiState.realtimeReconnectGeneration,
        onRouteChanged = viewModel::openRoute,
        onConversationOpened = viewModel::openConversation,
        onConversationSelectionCleared = viewModel::clearSelectedConversation,
        onHydratedConversationAccountChanged = viewModel::updateHydratedConversationAccountId,
        onRealtimeConnectionStateChanged = viewModel::updateRealtimeConnectionState,
        onRealtimeEventIdRecorded = viewModel::recordRealtimeEventId,
        onRealtimeLastEventIdReset = viewModel::resetRealtimeLastEventId,
        onRealtimeReconnectIncremented = viewModel::incrementRealtimeReconnectGeneration,
        onRealtimeReconnectIncrementedBy = viewModel::incrementRealtimeReconnectGenerationBy,
    )
}
