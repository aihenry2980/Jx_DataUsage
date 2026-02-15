package com.jx.jxdatausage.data

import com.jx.jxdatausage.util.formatBytes
import com.jx.jxdatausage.util.progressFraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageFormattersTest {

    @Test
    fun `formatBytes returns readable units`() {
        assertEquals("0 B", formatBytes(0L))
        assertTrue(formatBytes(1024L).contains("KB"))
    }

    @Test
    fun `progressFraction handles bounds`() {
        assertEquals(0f, progressFraction(0L, 100L))
        assertEquals(0.5f, progressFraction(50L, 100L))
        assertEquals(1f, progressFraction(200L, 100L))
    }
}

