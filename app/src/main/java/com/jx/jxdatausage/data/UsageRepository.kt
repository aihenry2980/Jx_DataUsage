package com.jx.jxdatausage.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.ConnectivityManager
import android.os.Process
import com.jx.jxdatausage.util.computeMonthStartMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class UsageRepository(private val context: Context) {
    private val networkStatsManager = context.getSystemService(NetworkStatsManager::class.java)
    private val packageManager = context.packageManager
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("M/d", Locale.getDefault())

    suspend fun getUsageForRange(
        startMs: Long,
        endMs: Long,
        includeWifiInSort: Boolean
    ): List<AppUsageRow> = withContext(Dispatchers.IO) {
        val uidToAppMap = buildUidToAppMap()
        val computedRows = buildComputedUsageRows(startMs, endMs, includeWifiInSort)
        computedRows.map { computed ->
            val resolvedApp = resolveApp(computed.uid, uidToAppMap) ?: error("App resolution should not be null")
            AppUsageRow(
                uid = computed.uid,
                packageName = resolvedApp.packageName,
                appName = resolvedApp.appName,
                iconRef = resolvedApp.packageName,
                isSystemApp = resolvedApp.isSystemApp,
                mobile = computed.mobile,
                wifi = computed.wifi,
                split = computed.split,
                combinedTotalForSort = computed.combinedTotal,
                mobileSplit = computed.mobileSplit,
                wifiSplit = computed.wifiSplit
            )
        }
    }

    fun getUsageForRangeBatched(
        startMs: Long,
        endMs: Long,
        includeWifiInSort: Boolean,
        batchSize: Int = 24
    ): Flow<List<AppUsageRow>> = flow {
        val safeBatchSize = batchSize.coerceAtLeast(1)
        val uidToAppMap = buildUidToAppMap()
        val computedRows = buildComputedUsageRows(startMs, endMs, includeWifiInSort)
        if (computedRows.isEmpty()) {
            emit(emptyList())
            return@flow
        }

        val resolvedRows = mutableListOf<AppUsageRow>()
        var lastEmittedSize = 0
        computedRows.forEach { computed ->
            val resolvedApp = resolveApp(computed.uid, uidToAppMap) ?: return@forEach
            resolvedRows.add(
                AppUsageRow(
                    uid = computed.uid,
                    packageName = resolvedApp.packageName,
                    appName = resolvedApp.appName,
                    iconRef = resolvedApp.packageName,
                    isSystemApp = resolvedApp.isSystemApp,
                    mobile = computed.mobile,
                    wifi = computed.wifi,
                    split = computed.split,
                    combinedTotalForSort = computed.combinedTotal,
                    mobileSplit = computed.mobileSplit,
                    wifiSplit = computed.wifiSplit
                )
            )
            if (resolvedRows.size - lastEmittedSize >= safeBatchSize) {
                emit(resolvedRows.toList())
                lastEmittedSize = resolvedRows.size
            }
        }

        if (resolvedRows.size != lastEmittedSize) {
            emit(resolvedRows.toList())
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getTotalUsageForRange(
        startMs: Long,
        endMs: Long,
        includeWifi: Boolean
    ): Long = withContext(Dispatchers.IO) {
        val mobileTotal = queryDeviceTotal(ConnectivityManager.TYPE_MOBILE, startMs, endMs)
        if (!includeWifi) {
            return@withContext mobileTotal
        }
        mobileTotal + queryDeviceTotal(ConnectivityManager.TYPE_WIFI, startMs, endMs)
    }

    suspend fun getRecentDailyBreakdown(
        days: Int,
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<DailyUsagePoint> = withContext(Dispatchers.IO) {
        if (days <= 0) return@withContext emptyList()

        val today = Instant.ofEpochMilli(nowMs).atZone(zoneId).toLocalDate()
        val oldestDay = today.minusDays((days - 1).toLong())
        return@withContext (0 until days).map { index ->
            val day = oldestDay.plusDays(index.toLong())
            val startMs = day.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMs = if (day == today) {
                nowMs
            } else {
                day.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            }
            val cellTotal = queryDeviceTotal(ConnectivityManager.TYPE_MOBILE, startMs, endMs)
            val wifiTotal = queryDeviceTotal(ConnectivityManager.TYPE_WIFI, startMs, endMs)
            DailyUsagePoint(
                label = day.format(dayLabelFormatter),
                cellBytes = cellTotal,
                wifiBytes = wifiTotal
            )
        }
    }

    suspend fun getCurrentMonthCellUsage(
        nowMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = withContext(Dispatchers.IO) {
        val monthStartMs = computeMonthStartMs(nowMs, zoneId)
        queryDeviceTotal(ConnectivityManager.TYPE_MOBILE, monthStartMs, nowMs)
    }

    suspend fun buildDailyWidgetSnapshot(
        includeWifi: Boolean,
        topLimit: Int
    ): WidgetSnapshot = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dayStart = PeriodGenerator.startOfTodayMs(ZoneId.systemDefault(), Instant.ofEpochMilli(now))
        val rows = getUsageForRange(dayStart, now, includeWifi)
        val dailyTotal = if (rows.isEmpty()) {
            getTotalUsageForRange(dayStart, now, includeWifi)
        } else {
            rows.sumOf { it.combinedTotalForSort }
        }
        WidgetSnapshot(
            generatedAtMs = now,
            dailyTotalBytes = dailyTotal,
            topApps = rows.take(topLimit).map { row ->
                WidgetRankItem(
                    uid = row.uid,
                    appName = row.appName,
                    packageName = row.packageName,
                    foregroundRx = row.split.foregroundRx,
                    foregroundTx = row.split.foregroundTx,
                    backgroundRx = row.split.backgroundRx,
                    backgroundTx = row.split.backgroundTx,
                    totalBytes = row.combinedTotalForSort
                )
            }
        )
    }

    private fun collectBuckets(
        networkType: Int,
        startMs: Long,
        endMs: Long,
        accumulators: MutableMap<Int, MutableUsageAccumulator>
    ) {
        val networkStats = runCatching {
            networkStatsManager.querySummary(networkType, null, startMs, endMs)
        }.getOrNull() ?: return

        val bucket = NetworkStats.Bucket()
        try {
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                val uid = bucket.uid
                val isTetheringUid = uid == NetworkStats.Bucket.UID_TETHERING
                if (uid == NetworkStats.Bucket.UID_REMOVED) {
                    continue
                }
                if (uid <= 0 && !isTetheringUid) {
                    continue
                }
                val usage = accumulators.getOrPut(uid) { MutableUsageAccumulator() }
                val rx = bucket.rxBytes.coerceAtLeast(0L)
                val tx = bucket.txBytes.coerceAtLeast(0L)
                val state = bucket.state
                when (networkType) {
                    ConnectivityManager.TYPE_MOBILE -> {
                        usage.mobileRx += rx
                        usage.mobileTx += tx
                        when (state) {
                            NetworkStats.Bucket.STATE_FOREGROUND -> {
                                usage.mobileForegroundRx += rx
                                usage.mobileForegroundTx += tx
                            }

                            NetworkStats.Bucket.STATE_DEFAULT -> {
                                usage.mobileBackgroundRx += rx
                                usage.mobileBackgroundTx += tx
                            }
                        }
                    }

                    ConnectivityManager.TYPE_WIFI -> {
                        usage.wifiRx += rx
                        usage.wifiTx += tx
                        when (state) {
                            NetworkStats.Bucket.STATE_FOREGROUND -> {
                                usage.wifiForegroundRx += rx
                                usage.wifiForegroundTx += tx
                            }

                            NetworkStats.Bucket.STATE_DEFAULT -> {
                                usage.wifiBackgroundRx += rx
                                usage.wifiBackgroundTx += tx
                            }
                        }
                    }
                }
            }
        } finally {
            networkStats.close()
        }
    }

    private fun queryDeviceTotal(networkType: Int, startMs: Long, endMs: Long): Long {
        return runCatching {
            val bucket = networkStatsManager.querySummaryForDevice(networkType, null, startMs, endMs)
            bucket.rxBytes.coerceAtLeast(0L) + bucket.txBytes.coerceAtLeast(0L)
        }.getOrDefault(0L)
    }

    private fun buildComputedUsageRows(
        startMs: Long,
        endMs: Long,
        includeWifiInSort: Boolean
    ): List<ComputedUsageRow> {
        val accumulators = mutableMapOf<Int, MutableUsageAccumulator>()
        collectBuckets(ConnectivityManager.TYPE_MOBILE, startMs, endMs, accumulators)
        collectBuckets(ConnectivityManager.TYPE_WIFI, startMs, endMs, accumulators)

        return accumulators.mapNotNull { (uid, usage) ->
            val mobile = NetworkUsage(
                totalBytes = usage.mobileRx + usage.mobileTx,
                rxBytes = usage.mobileRx,
                txBytes = usage.mobileTx
            )
            val wifi = NetworkUsage(
                totalBytes = usage.wifiRx + usage.wifiTx,
                rxBytes = usage.wifiRx,
                txBytes = usage.wifiTx
            )
            val combinedTotal = computeCombinedTotal(mobile, wifi, includeWifiInSort)
            if (combinedTotal <= 0L) {
                return@mapNotNull null
            }
            val split = if (includeWifiInSort) {
                SplitUsage(
                    foregroundRx = usage.mobileForegroundRx + usage.wifiForegroundRx,
                    foregroundTx = usage.mobileForegroundTx + usage.wifiForegroundTx,
                    backgroundRx = usage.mobileBackgroundRx + usage.wifiBackgroundRx,
                    backgroundTx = usage.mobileBackgroundTx + usage.wifiBackgroundTx
                )
            } else {
                SplitUsage(
                    foregroundRx = usage.mobileForegroundRx,
                    foregroundTx = usage.mobileForegroundTx,
                    backgroundRx = usage.mobileBackgroundRx,
                    backgroundTx = usage.mobileBackgroundTx
                )
            }
            val mobileSplit = SplitUsage(
                foregroundRx = usage.mobileForegroundRx,
                foregroundTx = usage.mobileForegroundTx,
                backgroundRx = usage.mobileBackgroundRx,
                backgroundTx = usage.mobileBackgroundTx
            )
            val wifiSplit = SplitUsage(
                foregroundRx = usage.wifiForegroundRx,
                foregroundTx = usage.wifiForegroundTx,
                backgroundRx = usage.wifiBackgroundRx,
                backgroundTx = usage.wifiBackgroundTx
            )
            ComputedUsageRow(
                uid = uid,
                mobile = mobile,
                wifi = wifi,
                split = split,
                mobileSplit = mobileSplit,
                wifiSplit = wifiSplit,
                combinedTotal = combinedTotal
            )
        }.sortedByDescending { it.combinedTotal }
    }

    private fun resolveApp(uid: Int, uidToAppMap: Map<Int, ResolvedApp>): ResolvedApp? {
        if (uid == NetworkStats.Bucket.UID_TETHERING) {
            return ResolvedApp(
                packageName = null,
                appName = "Tethering / Hotspot",
                isSystemApp = true
            )
        }
        if (uid == Process.SYSTEM_UID) {
            return ResolvedApp(
                packageName = "android",
                appName = "Android System",
                isSystemApp = true
            )
        }

        uidToAppMap[uid]?.let { return it }

        val packages = packageManager.getPackagesForUid(uid).orEmpty()
        val bestPackage = packages
            .mapNotNull { packageName ->
                runCatching {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    ResolvedApp(packageName = packageName, appName = label, isSystemApp = isSystem)
                }.getOrNull()
            }
            .sortedWith(
                compareBy<ResolvedApp> { it.isSystemApp }.thenBy { it.appName.lowercase() }
            )
            .firstOrNull()
        if (bestPackage != null) {
            return bestPackage
        }

        val fallbackName = packageManager.getNameForUid(uid)?.takeIf { it.contains(".") }
        if (!fallbackName.isNullOrBlank()) {
            val appName = runCatching {
                val appInfo = packageManager.getApplicationInfo(fallbackName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(fallbackName)
            return ResolvedApp(packageName = fallbackName, appName = appName, isSystemApp = false)
        }

        return ResolvedApp(
            packageName = null,
            appName = "UID $uid",
            isSystemApp = true
        )
    }

    private fun buildUidToAppMap(): Map<Int, ResolvedApp> {
        val appInfos = runCatching {
            packageManager.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0L))
        }.getOrElse {
            emptyList()
        }

        val groupedByUid = mutableMapOf<Int, MutableList<ResolvedApp>>()
        appInfos.forEach { appInfo ->
            if (appInfo.uid <= 0) return@forEach
            val packageName = appInfo.packageName ?: return@forEach
            val label = runCatching {
                packageManager.getApplicationLabel(appInfo).toString()
            }.getOrDefault(packageName)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            groupedByUid.getOrPut(appInfo.uid) { mutableListOf() }
                .add(ResolvedApp(packageName = packageName, appName = label, isSystemApp = isSystem))
        }

        return groupedByUid.mapValues { (_, candidates) ->
            candidates.sortedWith(
                compareBy<ResolvedApp> { it.isSystemApp }.thenBy { it.appName.lowercase() }
            ).first()
        }
    }

    private data class MutableUsageAccumulator(
        var mobileRx: Long = 0L,
        var mobileTx: Long = 0L,
        var wifiRx: Long = 0L,
        var wifiTx: Long = 0L,
        var mobileForegroundRx: Long = 0L,
        var mobileForegroundTx: Long = 0L,
        var mobileBackgroundRx: Long = 0L,
        var mobileBackgroundTx: Long = 0L,
        var wifiForegroundRx: Long = 0L,
        var wifiForegroundTx: Long = 0L,
        var wifiBackgroundRx: Long = 0L,
        var wifiBackgroundTx: Long = 0L
    )

    private data class ResolvedApp(
        val packageName: String?,
        val appName: String,
        val isSystemApp: Boolean
    )

    private data class ComputedUsageRow(
        val uid: Int,
        val mobile: NetworkUsage,
        val wifi: NetworkUsage,
        val split: SplitUsage,
        val mobileSplit: SplitUsage,
        val wifiSplit: SplitUsage,
        val combinedTotal: Long
    )
}
