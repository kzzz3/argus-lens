package com.kzzz3.argus.lens.app

import com.kzzz3.argus.lens.feature.realtime.RealtimeReconnectScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RealtimeReconnectSchedulerTest {
    @Test
    fun scheduleReconnectMarksRecoveringAndIncrementsGenerationAfterDelay() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val scheduler = RealtimeReconnectScheduler(
            scope = scope,
            delayMillisForAttempt = { 1L },
        )
        var recoveringCount = 0
        var generation = 0

        scheduler.schedule(
            isEnabled = { true },
            markRecovering = { recoveringCount += 1 },
            incrementGeneration = { generation += 1 },
        )
        delay(50)

        assertEquals(1, recoveringCount)
        assertEquals(1, generation)
        scope.cancel()
    }

    @Test
    fun markConnectedCancelsPendingReconnectAndResetsAttempts() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val scheduler = RealtimeReconnectScheduler(
            scope = scope,
            delayMillisForAttempt = { 10_000L },
        )
        var generation = 0

        scheduler.schedule(
            isEnabled = { true },
            markRecovering = {},
            incrementGeneration = { generation += 1 },
        )
        scheduler.markConnected()
        delay(50)

        assertEquals(0, generation)
        assertEquals(0, scheduler.currentAttempt)
        scope.cancel()
    }
}
