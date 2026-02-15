package com.jx.jxdatausage.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process

object UsageAccessHelper {
    fun hasUsageAccess(context: Context): Boolean {
        val appOpsManager = context.getSystemService(AppOpsManager::class.java)
        val mode = appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true
        }

        // Fallback for devices that report MODE_DEFAULT while still granting usage access.
        val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 60_000L
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
        return !stats.isNullOrEmpty()
    }
}

