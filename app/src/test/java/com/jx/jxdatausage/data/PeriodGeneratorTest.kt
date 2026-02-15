package com.jx.jxdatausage.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class PeriodGeneratorTest {

    @Test
    fun `daily periods contain 30 items`() {
        val periods = PeriodGenerator.generatePeriods(
            tab = PeriodTab.DAILY,
            now = Instant.parse("2026-02-15T12:00:00Z"),
            zoneId = ZoneId.of("UTC")
        )
        assertEquals(30, periods.size)
        assertTrue(periods.first().startMs < periods.first().endMs)
    }

    @Test
    fun `weekly periods contain 12 items`() {
        val periods = PeriodGenerator.generatePeriods(
            tab = PeriodTab.WEEKLY,
            now = Instant.parse("2026-02-15T12:00:00Z"),
            zoneId = ZoneId.of("UTC")
        )
        assertEquals(12, periods.size)
        assertTrue(periods.first().startMs < periods.first().endMs)
    }

    @Test
    fun `monthly periods contain 12 items`() {
        val periods = PeriodGenerator.generatePeriods(
            tab = PeriodTab.MONTHLY,
            now = Instant.parse("2026-02-15T12:00:00Z"),
            zoneId = ZoneId.of("UTC")
        )
        assertEquals(12, periods.size)
        assertTrue(periods.first().startMs < periods.first().endMs)
    }

    @Test
    fun `yearly periods contain 5 items`() {
        val periods = PeriodGenerator.generatePeriods(
            tab = PeriodTab.YEARLY,
            now = Instant.parse("2026-02-15T12:00:00Z"),
            zoneId = ZoneId.of("UTC")
        )
        assertEquals(5, periods.size)
        assertTrue(periods.first().startMs < periods.first().endMs)
    }
}

