package com.kzzz3.argus.lens.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
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
        return when (backgroundSyncTask.run()) {
            BackgroundSyncResult.Synced,
            BackgroundSyncResult.SkippedNoSession,
            -> Result.success()
        }
    }
}
