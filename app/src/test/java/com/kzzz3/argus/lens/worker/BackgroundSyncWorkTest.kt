package com.kzzz3.argus.lens.worker

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundSyncWorkTest {

    @Test
    fun requestRequiresNetworkConnectivity() {
        val request = BackgroundSyncWork.createRequest()

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun enqueueSpecKeepsExistingBackgroundSyncWork() {
        val spec = BackgroundSyncWork.enqueueSpec()

        assertEquals("argus-lens-background-sync", spec.uniqueWorkName)
        assertEquals(ExistingWorkPolicy.KEEP, spec.existingWorkPolicy)
        assertEquals(BackgroundSyncWork.createRequest().workSpec.workerClassName, spec.request.workSpec.workerClassName)
    }

    @Test
    fun requestUsesExponentialBackoffForTransientSyncFailures() {
        val request = BackgroundSyncWork.createRequest()

        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(WorkRequest.MIN_BACKOFF_MILLIS, request.workSpec.backoffDelayDuration)
    }
}
