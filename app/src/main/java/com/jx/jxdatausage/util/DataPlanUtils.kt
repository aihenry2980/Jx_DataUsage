package com.jx.jxdatausage.util

import com.jx.jxdatausage.data.DataUnit
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId

private const val BYTES_PER_MB = 1024L * 1024L
private const val BYTES_PER_GB = 1024L * 1024L * 1024L

fun bytesPerUnit(unit: DataUnit): Long {
    return when (unit) {
        DataUnit.MB -> BYTES_PER_MB
        DataUnit.GB -> BYTES_PER_GB
    }
}

fun parseMonthlyCapBytes(amountText: String, unit: DataUnit): Long? {
    val trimmed = amountText.trim()
    if (trimmed.isBlank()) return null
    val amount = trimmed.toBigDecimalOrNull() ?: return null
    if (amount <= BigDecimal.ZERO) return null

    val rawBytes = amount.multiply(BigDecimal.valueOf(bytesPerUnit(unit)))
    if (rawBytes > BigDecimal.valueOf(Long.MAX_VALUE)) return null

    val rounded = rawBytes.setScale(0, RoundingMode.HALF_UP)
    return runCatching { rounded.longValueExact() }.getOrNull()
}

fun formatCapInput(bytes: Long, unit: DataUnit): String {
    if (bytes <= 0L) return ""
    val value = BigDecimal.valueOf(bytes)
        .divide(BigDecimal.valueOf(bytesPerUnit(unit)), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return value.toPlainString()
}

fun computeMonthStartMs(
    nowMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Long {
    val now = Instant.ofEpochMilli(nowMs).atZone(zoneId)
    return now.toLocalDate()
        .withDayOfMonth(1)
        .atStartOfDay(zoneId)
        .toInstant()
        .toEpochMilli()
}

fun computeMonthlyProgress(usedBytes: Long, capBytes: Long?): Float {
    if (capBytes == null || capBytes <= 0L || usedBytes <= 0L) return 0f
    return usedBytes.toDouble().div(capBytes.toDouble()).toFloat()
}
