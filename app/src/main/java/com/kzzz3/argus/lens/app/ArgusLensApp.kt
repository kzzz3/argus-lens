package com.kzzz3.argus.lens.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kzzz3.argus.lens.app.host.AppShellCallbacks
import com.kzzz3.argus.lens.app.host.AppShellHost
import com.kzzz3.argus.lens.app.host.AppShellState
import com.kzzz3.argus.lens.app.state.ArgusLensAppViewModel

@Composable
fun ArgusLensApp(
    viewModel: ArgusLensAppViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AppShellHost(
        dependencies = viewModel.dependencies,
        appScope = viewModel.appScope,
        featureStateHolders = viewModel.featureStateHolders,
        state = AppShellState(
            appSessionState = uiState.appSessionState,
            currentRoute = uiState.currentRoute,
            activeChatConversationId = uiState.activeChatConversationId,
            restorableEntryContext = uiState.restorableEntryContext,
            hydratedConversationAccountId = uiState.hydratedConversationAccountId,
            realtimeConnectionState = uiState.realtimeConnectionState,
            realtimeLastEventId = uiState.realtimeLastEventId,
            realtimeReconnectGeneration = uiState.realtimeReconnectGeneration,
        ),
        callbacks = AppShellCallbacks(
            onRouteChanged = viewModel::openRoute,
            onConversationOpened = viewModel::openConversation,
            onActiveChatConversationChanged = viewModel::restoreActiveChatConversation,
            onHydratedSessionApplied = viewModel::applyHydratedSession,
            onAuthenticatedSessionApplied = viewModel::applyAuthenticatedSession,
            onSessionRefreshed = viewModel::applyRefreshedSession,
            onSessionCleared = viewModel::clearSession,
            onHydratedConversationAccountChanged = viewModel::updateHydratedConversationAccountId,
            onRestorableEntryContextCleared = viewModel::clearRestorableEntryContext,
            onRealtimeConnectionStateChanged = viewModel::updateRealtimeConnectionState,
            onRealtimeEventIdRecorded = viewModel::recordRealtimeEventId,
            onRealtimeLastEventIdReset = viewModel::resetRealtimeLastEventId,
            onRealtimeReconnectIncremented = viewModel::incrementRealtimeReconnectGeneration,
        ),
    )
}
