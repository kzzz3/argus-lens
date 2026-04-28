package com.kzzz3.argus.lens.feature.realtime

import com.kzzz3.argus.lens.feature.inbox.ConversationThreadsState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeClient
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeConnectionState
import com.kzzz3.argus.lens.model.realtime.ConversationRealtimeSubscription
import com.kzzz3.argus.lens.model.session.AppSessionState
import com.kzzz3.argus.lens.session.SessionCredentials
import com.kzzz3.argus.lens.session.SessionRefreshOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RealtimeConnectionRequest(
    val isAuthenticated: Boolean,
    val accountId: String,
    val credentials: SessionCredentials,
    val lastEventId: String,
    val reconnectGeneration: Int,
    val isRealtimeEnabled: () -> Boolean = { isAuthenticated && credentials.hasAccessToken },
    val getSession: () -> AppSessionState,
    val getConversationThreadsState: () -> ConversationThreadsState,
    val getActiveChatConversationId: () -> String,
    val isChatRouteActive: () -> Boolean,
)

data class RealtimeConnectionCallbacks(
    val onConnectionStateChanged: (ConversationRealtimeConnectionState) -> Unit,
    val onEventIdRecorded: (String) -> Unit,
    val onLastEventIdReset: () -> Unit,
    val onConversationThreadsChanged: (ConversationThreadsState) -> Unit,
    val onReconnectGenerationIncremented: () -> Unit,
    val onScheduleSessionRefreshLoop: () -> Unit,
    val onCancelSessionRefreshLoop: () -> Unit,
    val refreshSessionTokens: suspend () -> SessionRefreshOutcome,
    val signOutToEntry: (String) -> Unit,
)

class RealtimeConnectionManager(
    private val scope: CoroutineScope,
    private val realtimeClient: ConversationRealtimeClient,
    private val applyRealtimeConversationEvent: ApplyRealtimeConversationEventUseCase,
    private val reconnectScheduler: RealtimeReconnectScheduler,
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
            reconnectScheduler.disable()
            callbacks.onLastEventIdReset()
            callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.DISABLED)
            return
        }

        val connectionId = "realtime-${request.accountId}-${request.reconnectGeneration}"
        activeConnectionId = connectionId
        callbacks.onConnectionStateChanged(if (reconnectScheduler.currentAttempt > 0) {
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
                        reconnectScheduler.markConnected()
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
                    when (applyRealtimeConversationEvent.classifyEvent(event)) {
                        RealtimeEventKind.StreamReady -> {
                            callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.LIVE)
                            return@launch
                        }
                        RealtimeEventKind.Heartbeat -> return@launch
                        RealtimeEventKind.DomainEvent -> Unit
                    }
                    eventMutex.withLock {
                        callbacks.onConversationThreadsChanged(applyRealtimeConversationEvent.applyEvent(
                            event = event,
                            session = request.getSession(),
                            currentState = request.getConversationThreadsState(),
                            activeChatConversationId = request.getActiveChatConversationId(),
                            isChatRouteActive = request.isChatRouteActive(),
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
        reconnectScheduler.disable()
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
                SessionRefreshOutcome.Success -> {
                    reconnectScheduler.markConnected()
                    callbacks.onReconnectGenerationIncremented()
                }
                is SessionRefreshOutcome.Failure -> {
                    if (refreshResult.isUnauthorized) {
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
        reconnectScheduler.schedule(
            isEnabled = request.isRealtimeEnabled,
            markRecovering = { callbacks.onConnectionStateChanged(ConversationRealtimeConnectionState.RECOVERING) },
            incrementGeneration = callbacks.onReconnectGenerationIncremented,
        )
    }
}
