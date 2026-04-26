package com.kzzz3.argus.lens.worker

import android.content.Context
import androidx.work.WorkManager

interface BackgroundSyncScheduler {
    fun enqueue()
}

class WorkManagerBackgroundSyncScheduler(
    context: Context,
) : BackgroundSyncScheduler {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    override fun enqueue() {
        val spec = BackgroundSyncWork.enqueueSpec()
        workManager.enqueueUniqueWork(
            spec.uniqueWorkName,
            spec.existingWorkPolicy,
            spec.request,
        )
    }
}
