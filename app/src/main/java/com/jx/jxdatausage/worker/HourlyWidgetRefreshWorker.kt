package com.jx.jxdatausage.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.jx.jxdatausage.data.SettingsRepository
import com.jx.jxdatausage.data.UsageAccessHelper
import com.jx.jxdatausage.data.UsageRepository
import com.jx.jxdatausage.data.WidgetSnapshotRepository
import java.util.concurrent.TimeUnit

class HourlyWidgetRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!UsageAccessHelper.hasUsageAccess(applicationContext)) {
            return Result.success()
        }

        val settingsRepository = SettingsRepository(applicationContext)
        val usageRepository = UsageRepository(applicationContext)
        val snapshotRepository = WidgetSnapshotRepository(applicationContext)
        return runCatching {
            val showWifi = settingsRepository.getShowWifiUsage()
            val snapshot = usageRepository.buildDailyWidgetSnapshot(showWifi, TOP_APPS_LIMIT)
            snapshotRepository.saveSnapshot(snapshot)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "hourly_widget_refresh_work"
        private const val TOP_APPS_LIMIT = 5

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<HourlyWidgetRefreshWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
