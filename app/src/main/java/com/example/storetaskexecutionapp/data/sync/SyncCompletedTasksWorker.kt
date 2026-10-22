package com.example.storetaskexecutionapp.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.storetaskexecutionapp.StoreTaskApplication
import com.example.storetaskexecutionapp.data.repository.OfflineException
import java.util.concurrent.TimeUnit

class SyncCompletedTasksWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? StoreTaskApplication ?: return Result.retry()
        return try {
            app.container.taskRepository.syncCompletedTasks()
            Result.success()
        } catch (_: OfflineException) {
            Result.retry()
        } catch (_: Exception) {
            if (runAttemptCount >= 5) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val UNIQUE_ONE_TIME = "sync_completed_tasks"
        const val UNIQUE_PERIODIC = "sync_completed_tasks_periodic"

        private val connectedConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun enqueueNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<SyncCompletedTasksWorker>()
                .setConstraints(connectedConstraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(UNIQUE_ONE_TIME, ExistingWorkPolicy.REPLACE, request)
        }

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncCompletedTasksWorker>(15, TimeUnit.MINUTES)
                .setConstraints(connectedConstraints)
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(
                    UNIQUE_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
