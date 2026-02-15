package com.jx.jxdatausage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jx.jxdatausage.data.NetworkUsage
import com.jx.jxdatausage.data.SplitUsage
import com.jx.jxdatausage.util.formatBytes
import kotlin.math.max

@Composable
fun UsageBars(
    usage: NetworkUsage,
    split: SplitUsage,
    maxValue: Long,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = formatBytes(usage.totalBytes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }

        val totalParts = normalizedParts(
            metricBytes = usage.totalBytes,
            foregroundBytes = split.foregroundRx + split.foregroundTx,
            backgroundBytes = split.backgroundRx + split.backgroundTx
        )
        val upParts = normalizedParts(
            metricBytes = usage.txBytes,
            foregroundBytes = split.foregroundTx,
            backgroundBytes = split.backgroundTx
        )
        val downParts = normalizedParts(
            metricBytes = usage.rxBytes,
            foregroundBytes = split.foregroundRx,
            backgroundBytes = split.backgroundRx
        )

        MetricSplitRow(
            label = "Total",
            foregroundBytes = totalParts.first,
            backgroundBytes = totalParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFFFF1A1A),
            backgroundColor = Color(0xFFF4A9C6)
        )
        MetricSplitRow(
            label = "Up",
            foregroundBytes = upParts.first,
            backgroundBytes = upParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFF2D69A1),
            backgroundColor = Color(0xFF9FC3E2)
        )
        MetricSplitRow(
            label = "Down",
            foregroundBytes = downParts.first,
            backgroundBytes = downParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFF3B8125),
            backgroundColor = Color(0xFF8ED071)
        )
    }
}

@Composable
private fun MetricSplitRow(
    label: String,
    foregroundBytes: Long,
    backgroundBytes: Long,
    maxValue: Long,
    foregroundColor: Color,
    backgroundColor: Color
) {
    val metricTotal = (foregroundBytes + backgroundBytes).coerceAtLeast(0L)
    val safeMax = max(maxValue, 1L)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelWidth = 44.dp
        val valueWidth = 40.dp
        val reserved = labelWidth + valueWidth + valueWidth + 24.dp
        val maxBarArea = (maxWidth - reserved).coerceAtLeast(60.dp)
        val metricRatio = (metricTotal.toFloat() / safeMax.toFloat()).coerceIn(0f, 1f)
        val totalBarWidth = if (metricTotal <= 0L) 40.dp else (maxBarArea * metricRatio).coerceIn(40.dp, maxBarArea)
        val fgRatio = if (metricTotal > 0L) {
            (foregroundBytes.toFloat() / metricTotal.toFloat()).coerceIn(0f, 1f)
        } else {
            0.5f
        }
        val fgWidth = totalBarWidth * fgRatio
        val bgWidth = totalBarWidth - fgWidth

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier.width(labelWidth),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                ),
                maxLines = 1,
                softWrap = false
            )
            DataSegment(
                letter = "F",
                width = fgWidth,
                color = foregroundColor
            )
            Text(
                text = formatShortData(foregroundBytes),
                modifier = Modifier.width(valueWidth),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                ),
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
            DataSegment(
                letter = "B",
                width = bgWidth,
                color = backgroundColor
            )
            Text(
                text = formatShortData(backgroundBytes),
                modifier = Modifier.width(valueWidth),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 10.sp
                ),
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun DataSegment(
    letter: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .width(width.coerceAtLeast(18.dp))
            .height(22.dp)
            .border(1.dp, Color(0xFF003049), shape)
            .background(color, shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = Color.White,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun normalizedParts(
    metricBytes: Long,
    foregroundBytes: Long,
    backgroundBytes: Long
): Pair<Long, Long> {
    val fg = foregroundBytes.coerceAtLeast(0L)
    val bg = backgroundBytes.coerceAtLeast(0L)
    if (metricBytes <= 0L) return 0L to 0L
    return if (fg + bg <= 0L) {
        metricBytes to 0L
    } else {
        fg to bg
    }
}

private fun formatShortData(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    if (gb >= 1.0) {
        return String.format("%.1fG", gb)
    }
    val mb = bytes / (1024.0 * 1024.0)
    if (mb >= 1.0) {
        return String.format("%.0fM", mb)
    }
    val kb = bytes / 1024.0
    if (kb >= 1.0) {
        return String.format("%.0fK", kb)
    }
    return "0"
}
