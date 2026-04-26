package com.kzzz3.argus.lens.worker

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder

object BackgroundSyncWork {
    private const val UniqueWorkName = "argus-lens-background-sync"

    fun createRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        return OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
            .setConstraints(constraints)
            .build()
    }

    fun enqueueSpec(): BackgroundSyncEnqueueSpec {
        return BackgroundSyncEnqueueSpec(
            uniqueWorkName = UniqueWorkName,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
            request = createRequest(),
        )
    }
}

data class BackgroundSyncEnqueueSpec(
    val uniqueWorkName: String,
    val existingWorkPolicy: ExistingWorkPolicy,
    val request: OneTimeWorkRequest,
)
