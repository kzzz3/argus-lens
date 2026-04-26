package com.kzzz3.argus.lens.feature.call

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CallSessionRuntime(
    private val scope: CoroutineScope,
    private val activationDelayMillis: Long = 800L,
    private val tickDelayMillis: Long = 1_000L,
    private val returnToChatDelayMillis: Long = 300L,
) {
    private var activeJob: Job? = null

    fun startCall(
        conversationId: String,
        contactDisplayName: String,
        mode: CallSessionMode,
        setState: (CallSessionState) -> Unit,
        openCallSession: () -> Unit,
        shouldKeepTicking: () -> Boolean,
    ) {
        var latestState = startCallSession(
            conversationId = conversationId,
            contactDisplayName = contactDisplayName,
            mode = mode,
        )
        setState(latestState)
        activeJob?.cancel()
        activeJob = scope.launch {
            delay(activationDelayMillis)
            latestState = activateConnectingCallSession(latestState)
            setState(latestState)
            while (latestState.status == CallSessionStatus.Active && shouldKeepTicking()) {
                delay(tickDelayMillis)
                latestState = tickActiveCallSession(latestState)
                setState(latestState)
            }
        }
        openCallSession()
    }

    fun endCall(
        currentState: CallSessionState,
        setState: (CallSessionState) -> Unit,
        openChat: () -> Unit,
    ) {
        activeJob?.cancel()
        activeJob = null
        setState(reduceCallSessionState(currentState, CallSessionAction.EndCall))
        scope.launch {
            delay(returnToChatDelayMillis)
            openChat()
        }
    }

    fun cancel() {
        activeJob?.cancel()
        activeJob = null
    }
}
