package com.kzzz3.argus.lens.worker

import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundSyncWorkerTest {
    @Test
    fun retryResultMapsToWorkManagerRetry() {
        assertEquals(
            ListenableWorker.Result.retry(),
            BackgroundSyncResult.Retry.toWorkerResult(),
        )
    }
}
