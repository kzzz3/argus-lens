package com.kzzz3.argus.lens.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkRequest
import java.util.concurrent.TimeUnit

object BackgroundSyncWork {
    private const val UniqueWorkName = "argus-lens-background-sync"

    fun createRequest(): OneTimeWorkRequest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        return OneTimeWorkRequestBuilder<BackgroundSyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS,
            )
            .build()
    }

    fun enqueueSpec(): BackgroundSyncEnqueueSpec {
        return BackgroundSyncEnqueueSpec(
            uniqueWorkName = UniqueWorkName,
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            request = createRequest(),
        )
    }
}

data class BackgroundSyncEnqueueSpec(
    val uniqueWorkName: String,
    val existingWorkPolicy: ExistingWorkPolicy,
    val request: OneTimeWorkRequest,
)
