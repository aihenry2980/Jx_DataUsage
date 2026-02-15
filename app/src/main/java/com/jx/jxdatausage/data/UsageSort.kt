package com.jx.jxdatausage.data

enum class UsageSortMode(val label: String) {
    TOTAL("Total"),
    DOWNLOAD("Down"),
    UPLOAD("Up"),
    FOREGROUND("Foreground"),
    BACKGROUND("Background")
}

enum class SortDirection {
    DESC,
    ASC
}

fun computeCombinedTotal(mobile: NetworkUsage, wifi: NetworkUsage?, includeWifi: Boolean): Long {
    return if (includeWifi) {
        mobile.totalBytes + (wifi?.totalBytes ?: 0L)
    } else {
        mobile.totalBytes
    }
}

fun sortUsageRows(rows: List<AppUsageRow>): List<AppUsageRow> {
    return rows.sortedByDescending { it.combinedTotalForSort }
}

fun sortUsageRows(
    rows: List<AppUsageRow>,
    sortMode: UsageSortMode,
    includeWifi: Boolean,
    sortDirection: SortDirection = SortDirection.DESC
): List<AppUsageRow> {
    val sorted = rows.sortedWith(
        compareByDescending<AppUsageRow> { row ->
            rowSortMetric(row, sortMode, includeWifi)
        }.thenByDescending { row ->
            computeCombinedTotal(row.mobile, row.wifi, includeWifi)
        }.thenBy { row ->
            row.appName.lowercase()
        }
    )
    return if (sortDirection == SortDirection.DESC) sorted else sorted.reversed()
}

private fun rowSortMetric(
    row: AppUsageRow,
    sortMode: UsageSortMode,
    includeWifi: Boolean
): Long {
    val wifiRx = if (includeWifi) row.wifi?.rxBytes ?: 0L else 0L
    val wifiTx = if (includeWifi) row.wifi?.txBytes ?: 0L else 0L
    val fgFromNetworkSplits = if (includeWifi) {
        row.mobileSplit.foregroundRx + row.mobileSplit.foregroundTx +
            row.wifiSplit.foregroundRx + row.wifiSplit.foregroundTx
    } else {
        row.mobileSplit.foregroundRx + row.mobileSplit.foregroundTx
    }
    val bgFromNetworkSplits = if (includeWifi) {
        row.mobileSplit.backgroundRx + row.mobileSplit.backgroundTx +
            row.wifiSplit.backgroundRx + row.wifiSplit.backgroundTx
    } else {
        row.mobileSplit.backgroundRx + row.mobileSplit.backgroundTx
    }
    val fallbackFg = row.split.foregroundRx + row.split.foregroundTx
    val fallbackBg = row.split.backgroundRx + row.split.backgroundTx
    val fg = if (fgFromNetworkSplits > 0L || bgFromNetworkSplits > 0L) fgFromNetworkSplits else fallbackFg
    val bg = if (fgFromNetworkSplits > 0L || bgFromNetworkSplits > 0L) bgFromNetworkSplits else fallbackBg
    return when (sortMode) {
        UsageSortMode.TOTAL -> computeCombinedTotal(row.mobile, row.wifi, includeWifi)
        UsageSortMode.DOWNLOAD -> row.mobile.rxBytes + wifiRx
        UsageSortMode.UPLOAD -> row.mobile.txBytes + wifiTx
        UsageSortMode.FOREGROUND -> fg
        UsageSortMode.BACKGROUND -> bg
    }
}
