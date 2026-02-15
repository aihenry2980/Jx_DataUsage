package com.jx.jxdatausage.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageSortTest {

    @Test
    fun `computeCombinedTotal respects wifi toggle`() {
        val mobile = NetworkUsage(totalBytes = 100L, rxBytes = 70L, txBytes = 30L)
        val wifi = NetworkUsage(totalBytes = 300L, rxBytes = 250L, txBytes = 50L)

        assertEquals(100L, computeCombinedTotal(mobile, wifi, includeWifi = false))
        assertEquals(400L, computeCombinedTotal(mobile, wifi, includeWifi = true))
    }

    @Test
    fun `sortUsageRows returns descending by combined total`() {
        val row1 = AppUsageRow(
            uid = 1,
            packageName = "a",
            appName = "A",
            iconRef = "a",
            mobile = NetworkUsage(10L, 6L, 4L),
            wifi = NetworkUsage(2L, 1L, 1L),
            split = SplitUsage(5L, 2L, 1L, 2L),
            combinedTotalForSort = 12L
        )
        val row2 = AppUsageRow(
            uid = 2,
            packageName = "b",
            appName = "B",
            iconRef = "b",
            mobile = NetworkUsage(20L, 12L, 8L),
            wifi = NetworkUsage(3L, 2L, 1L),
            split = SplitUsage(2L, 3L, 6L, 2L),
            combinedTotalForSort = 23L
        )

        val sorted = sortUsageRows(listOf(row1, row2))
        assertEquals(2, sorted.size)
        assertEquals("B", sorted.first().appName)
    }

    @Test
    fun `sortUsageRows supports download upload foreground background`() {
        val row1 = AppUsageRow(
            uid = 1,
            packageName = "a",
            appName = "A",
            iconRef = "a",
            mobile = NetworkUsage(40L, 35L, 5L),
            wifi = NetworkUsage(10L, 5L, 5L),
            split = SplitUsage(2L, 3L, 20L, 5L),
            combinedTotalForSort = 50L
        )
        val row2 = AppUsageRow(
            uid = 2,
            packageName = "b",
            appName = "B",
            iconRef = "b",
            mobile = NetworkUsage(45L, 20L, 25L),
            wifi = NetworkUsage(15L, 12L, 3L),
            split = SplitUsage(25L, 10L, 1L, 1L),
            combinedTotalForSort = 60L
        )

        val rows = listOf(row1, row2)

        assertEquals(
            "A",
            sortUsageRows(rows, UsageSortMode.DOWNLOAD, includeWifi = true).first().appName
        )
        assertEquals(
            "B",
            sortUsageRows(rows, UsageSortMode.UPLOAD, includeWifi = true).first().appName
        )
        assertEquals(
            "B",
            sortUsageRows(rows, UsageSortMode.FOREGROUND, includeWifi = true).first().appName
        )
        assertEquals(
            "A",
            sortUsageRows(rows, UsageSortMode.BACKGROUND, includeWifi = true).first().appName
        )
    }

    @Test
    fun `sortUsageRows supports ascending direction`() {
        val row1 = AppUsageRow(
            uid = 1,
            packageName = "a",
            appName = "A",
            iconRef = "a",
            mobile = NetworkUsage(10L, 6L, 4L),
            wifi = NetworkUsage(2L, 1L, 1L),
            split = SplitUsage(0L, 0L, 0L, 0L),
            combinedTotalForSort = 12L
        )
        val row2 = AppUsageRow(
            uid = 2,
            packageName = "b",
            appName = "B",
            iconRef = "b",
            mobile = NetworkUsage(20L, 12L, 8L),
            wifi = NetworkUsage(3L, 2L, 1L),
            split = SplitUsage(0L, 0L, 0L, 0L),
            combinedTotalForSort = 23L
        )

        val sorted = sortUsageRows(
            rows = listOf(row1, row2),
            sortMode = UsageSortMode.TOTAL,
            includeWifi = true,
            sortDirection = SortDirection.ASC
        )
        assertEquals("A", sorted.first().appName)
    }
}
