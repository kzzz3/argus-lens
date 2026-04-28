package com.kzzz3.argus.lens.feature.realtime

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RealtimeReconnectScheduler(
    private val scope: CoroutineScope,
    private val delayMillisForAttempt: (Int) -> Long = ::realtimeReconnectDelayMillis,
) {
    private var reconnectJob: Job? = null

    var currentAttempt: Int = 0
        private set

    fun schedule(
        isEnabled: () -> Boolean,
        markRecovering: () -> Unit,
        incrementGeneration: () -> Unit,
    ) {
        if (!isEnabled()) return
        if (reconnectJob?.isActive == true) return
        val nextAttempt = currentAttempt + 1
        currentAttempt = nextAttempt
        markRecovering()
        reconnectJob = scope.launch {
            delay(delayMillisForAttempt(nextAttempt))
            if (isEnabled()) {
                incrementGeneration()
            }
            reconnectJob = null
        }
    }

    fun markConnected() {
        currentAttempt = 0
        reconnectJob?.cancel()
        reconnectJob = null
    }

    fun disable() {
        markConnected()
    }
}
