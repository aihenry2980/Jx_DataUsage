package com.jx.jxdatausage.util

import kotlin.math.ln
import kotlin.math.pow

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroup = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.lastIndex)
    val value = bytes / 1024.0.pow(digitGroup.toDouble())
    val formatted = if (value >= 100 || digitGroup == 0) {
        "%.0f".format(value)
    } else {
        "%.1f".format(value)
    }
    return "$formatted ${units[digitGroup]}"
}

fun progressFraction(value: Long, maxValue: Long): Float {
    if (maxValue <= 0L || value <= 0L) return 0f
    return (value.toDouble() / maxValue.toDouble()).coerceIn(0.0, 1.0).toFloat()
}

