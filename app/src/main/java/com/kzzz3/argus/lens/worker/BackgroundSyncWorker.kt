package com.kzzz3.argus.lens.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BackgroundSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParameters: WorkerParameters,
    private val backgroundSyncTask: BackgroundSyncTask,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        return backgroundSyncTask.run().toWorkerResult()
    }
}

internal fun BackgroundSyncResult.toWorkerResult(): ListenableWorker.Result {
    return when (this) {
        BackgroundSyncResult.Synced,
        BackgroundSyncResult.SkippedNoSession,
        -> ListenableWorker.Result.success()
        BackgroundSyncResult.Retry -> ListenableWorker.Result.retry()
    }
}
