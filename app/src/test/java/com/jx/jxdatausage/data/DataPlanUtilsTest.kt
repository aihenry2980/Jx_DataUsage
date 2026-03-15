package com.jx.jxdatausage.data

import com.jx.jxdatausage.util.computeMonthStartMs
import com.jx.jxdatausage.util.computeMonthlyProgress
import com.jx.jxdatausage.util.parseMonthlyCapBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DataPlanUtilsTest {

    @Test
    fun `parseMonthlyCapBytes converts gb and mb`() {
        assertEquals(1024L * 1024L * 1024L, parseMonthlyCapBytes("1", DataUnit.GB))
        assertEquals(500L * 1024L * 1024L, parseMonthlyCapBytes("500", DataUnit.MB))
    }

    @Test
    fun `parseMonthlyCapBytes rejects invalid values`() {
        assertNull(parseMonthlyCapBytes("", DataUnit.GB))
        assertNull(parseMonthlyCapBytes("0", DataUnit.GB))
        assertNull(parseMonthlyCapBytes("-1", DataUnit.MB))
        assertNull(parseMonthlyCapBytes("abc", DataUnit.GB))
    }

    @Test
    fun `computeMonthStartMs uses local month boundary`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val now = ZonedDateTime.of(2026, 2, 22, 13, 50, 0, 0, zone)
        val start = computeMonthStartMs(now.toInstant().toEpochMilli(), zone)
        val expected = ZonedDateTime.of(2026, 2, 1, 0, 0, 0, 0, zone)
        assertEquals(expected.toInstant().toEpochMilli(), start)
    }

    @Test
    fun `computeMonthlyProgress handles normal and over cap`() {
        assertEquals(0.5f, computeMonthlyProgress(50, 100))
        assertEquals(1f, computeMonthlyProgress(100, 100))
        assertEquals(1.25f, computeMonthlyProgress(125, 100))
        assertEquals(0f, computeMonthlyProgress(125, null))
        assertEquals(0f, computeMonthlyProgress(125, 0))
    }
}
