package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.app.navigation.AppRoute
import com.kzzz3.argus.lens.data.auth.AuthFailureKind
import com.kzzz3.argus.lens.data.auth.AuthRepositoryResult
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.data.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.data.session.SessionCredentials
import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.feature.realtime.RealtimeCoordinator
import com.kzzz3.argus.lens.feature.realtime.RealtimeEventKind
import com.kzzz3.argus.lens.feature.realtime.isSseAuthFailure
import com.kzzz3.argus.lens.model.session.AppSessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal data class RealtimeConnectionRequest(
    val isAuthenticated: Boolean,
    val accountId: String,
    val credentials: SessionCredentials,
    val lastEventId: String,
    val reconnectGeneration: Int,
    val isRealtimeEnabled: () -> Boolean = { isAuthenticated && credentials.hasAccessToken },
    val getSession: () -> AppSessionState,
    val getConversationThreadsState: () -> ConversationThreadsState,
    val getSelectedConversationId: () -> String,
    val getCurrentRoute: () -> AppRoute,
)

internal data class RealtimeConnectionCallbacks(
    val onConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    val onEventIdRecorded: (String) -> Unit,
    val onLastEventIdReset: () -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onReconnectGenerationIncremented: () -> Unit,
    val onScheduleSessionRefreshLoop: () -> Unit,
    val onCancelSessionRefreshLoop: () -> Unit,
    val refreshSessionTokens: suspend () -> AuthRepositoryResult,
    val signOutToEntry: (String) -> Unit,
)

internal class RealtimeConnectionRuntime(
    private val scope: CoroutineScope,
    private val realtimeClient: ConversationRealtimeClient,
    private val realtimeCoordinator: RealtimeCoordinator,
    private val reconnectRuntime: RealtimeReconnectRuntime,
    private val eventMutex: Mutex = Mutex(),
) {
    private var subscription: ConversationRealtimeSubscription? = null
    private var activeConnectionId: String = ""

    fun connect(
        request: RealtimeConnectionRequest,
        callbacks: RealtimeConnectionCallbacks,
    ) {
        activeConnectionId = ""
        subscription?.close()
        subscription = null

        if (!request.isAuthenticated || !request.credentials.hasAccessToken) {
            reconnectRuntime.disable()
            callbacks.onLastEventIdReset()
            callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.DISABLED)
            return
        }

        val connectionId = "realtime-${request.accountId}-${request.reconnectGeneration}"
        activeConnectionId = connectionId
        callbacks.onConnectionStateChanged(if (reconnectRuntime.currentAttempt > 0) {
            ConversationRealtimeConnectionState.RECOVERING
        } else {
            ConversationRealtimeConnectionState.CONNECTING
        })
        subscription = realtimeClient.connect(
            accessToken = request.credentials.accessToken,
            lastEventId = request.lastEventId.ifBlank { null },
            onConnected = {
                scope.launch {
                    if (activeConnectionId == connectionId) {
                        callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.LIVE)
                        reconnectRuntime.markConnected()
                        callbacks.onScheduleSessionRefreshLoop()
                    }
                }
            },
            onClosed = {
                scope.launch {
                    if (activeConnectionId == connectionId) {
                        scheduleReconnect(request, callbacks)
                    }
                }
            },
            onEvent = { event ->
                scope.launch {
                    if (activeConnectionId != connectionId) return@launch
                    if (event.eventId.isNotBlank()) {
                        callbacks.onEventIdRecorded(event.eventId)
                    }
                    when (realtimeCoordinator.classifyEvent(event)) {
                        RealtimeEventKind.StreamReady -> {
                            callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.LIVE)
                            return@launch
                        }
                        RealtimeEventKind.Heartbeat -> return@launch
                        RealtimeEventKind.DomainEvent -> Unit
                    }
                    eventMutex.withLock {
                        callbacks.onConversationThreadsChanged(realtimeCoordinator.applyEvent(
                            event = event,
                            session = request.getSession(),
                            currentState = request.getConversationThreadsState(),
                            selectedConversationId = request.getSelectedConversationId(),
                            isChatRouteActive = request.getCurrentRoute() == AppRoute.Chat,
                        ))
                    }
                }
            },
            onError = { error ->
                scope.launch {
                    if (activeConnectionId == connectionId) {
                        handleError(
                            error = error,
                            request = request,
                            callbacks = callbacks,
                        )
                    }
                }
            },
        )
    }

    fun dispose(callbacks: RealtimeConnectionCallbacks) {
        activeConnectionId = ""
        reconnectRuntime.disable()
        callbacks.onCancelSessionRefreshLoop()
        subscription?.close()
        subscription = null
    }

    private suspend fun handleError(
        error: Throwable,
        request: RealtimeConnectionRequest,
        callbacks: RealtimeConnectionCallbacks,
    ) {
        if (isSseAuthFailure(error)) {
            when (val refreshResult = callbacks.refreshSessionTokens()) {
                is AuthRepositoryResult.Success -> {
                    reconnectRuntime.markConnected()
                    callbacks.onReconnectGenerationIncremented()
                }
                is AuthRepositoryResult.Failure -> {
                    if (refreshResult.kind == AuthFailureKind.UNAUTHORIZED) {
                        callbacks.signOutToEntry("Session expired or was revoked. Please sign in again.")
                    } else {
                        scheduleReconnect(request, callbacks)
                    }
                }
            }
        } else {
            scheduleReconnect(request, callbacks)
        }
    }

    private fun scheduleReconnect(
        request: RealtimeConnectionRequest,
        callbacks: RealtimeConnectionCallbacks,
    ) {
        reconnectRuntime.schedule(
            isEnabled = request.isRealtimeEnabled,
            markRecovering = { callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.RECOVERING) },
            incrementGeneration = callbacks.onReconnectGenerationIncremented,
        )
    }
}
