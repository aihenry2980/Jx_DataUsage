package com.jx.jxdatausage.data

enum class PeriodTab {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class DataUnit {
    MB,
    GB
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

data class PeriodItem(
    val id: String,
    val label: String,
    val startMs: Long,
    val endMs: Long,
    val tab: PeriodTab
)

data class NetworkUsage(
    val totalBytes: Long,
    val rxBytes: Long,
    val txBytes: Long
)

data class SplitUsage(
    val foregroundRx: Long,
    val foregroundTx: Long,
    val backgroundRx: Long,
    val backgroundTx: Long
)

data class AppUsageRow(
    val uid: Int,
    val packageName: String?,
    val appName: String,
    val iconRef: String?,
    val isSystemApp: Boolean = false,
    val mobile: NetworkUsage,
    val wifi: NetworkUsage?,
    val split: SplitUsage,
    val combinedTotalForSort: Long,
    val mobileSplit: SplitUsage = SplitUsage(0L, 0L, 0L, 0L),
    val wifiSplit: SplitUsage = SplitUsage(0L, 0L, 0L, 0L)
)

data class WidgetRankItem(
    val uid: Int,
    val appName: String,
    val packageName: String?,
    val foregroundRx: Long,
    val foregroundTx: Long,
    val backgroundRx: Long,
    val backgroundTx: Long,
    val totalBytes: Long
)

data class WidgetSnapshot(
    val generatedAtMs: Long,
    val dailyTotalBytes: Long,
    val topApps: List<WidgetRankItem>
)

data class DailyUsagePoint(
    val label: String,
    val cellBytes: Long,
    val wifiBytes: Long,
    val isToday: Boolean = false
)
