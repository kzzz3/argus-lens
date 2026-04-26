package com.kzzz3.argus.lens.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundSyncWorkTest {

    @Test
    fun requestRequiresNetworkConnectivity() {
        val request = BackgroundSyncWork.createRequest()

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun enqueueSpecUsesSingleReplaceableBackgroundSyncWork() {
        val spec = BackgroundSyncWork.enqueueSpec()

        assertEquals("argus-lens-background-sync", spec.uniqueWorkName)
        assertEquals(ExistingWorkPolicy.REPLACE, spec.existingWorkPolicy)
        assertEquals(BackgroundSyncWork.createRequest().workSpec.workerClassName, spec.request.workSpec.workerClassName)
    }
}
