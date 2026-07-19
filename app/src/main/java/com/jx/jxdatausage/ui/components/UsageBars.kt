package com.jx.jxdatausage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jx.jxdatausage.data.NetworkUsage
import com.jx.jxdatausage.data.SplitUsage
import java.util.Locale
import kotlin.math.max

@Composable
fun UsageBars(
    usage: NetworkUsage,
    split: SplitUsage,
    maxValue: Long,
    maxBackgroundValue: Long,
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = formatMb(usage.totalBytes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            metricBytes = usage.totalBytes,
            foregroundBytes = totalParts.first,
            backgroundBytes = totalParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFFFF1A1A),
            backgroundColor = Color(0xFFF4A9C6)
        )
        BackgroundComparisonRow(
            backgroundBytes = totalParts.second,
            maxBackgroundValue = maxBackgroundValue,
            color = Color(0xFFF06FA5)
        )
        MetricSplitRow(
            label = "Up",
            metricBytes = usage.txBytes,
            foregroundBytes = upParts.first,
            backgroundBytes = upParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFF2D69A1),
            backgroundColor = Color(0xFF9FC3E2)
        )
        BackgroundComparisonRow(
            backgroundBytes = upParts.second,
            maxBackgroundValue = maxBackgroundValue,
            color = Color(0xFF78ADD7)
        )
        MetricSplitRow(
            label = "Down",
            metricBytes = usage.rxBytes,
            foregroundBytes = downParts.first,
            backgroundBytes = downParts.second,
            maxValue = maxValue,
            foregroundColor = Color(0xFF3B8125),
            backgroundColor = Color(0xFF8ED071)
        )
        BackgroundComparisonRow(
            backgroundBytes = downParts.second,
            maxBackgroundValue = maxBackgroundValue,
            color = Color(0xFF70BE5A)
        )
    }
}

@Composable
private fun BackgroundComparisonRow(
    backgroundBytes: Long,
    maxBackgroundValue: Long,
    color: Color
) {
    val safeMax = max(maxBackgroundValue, 1L)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelWidth = 44.dp
        val valueWidth = 86.dp
        val reserved = labelWidth + valueWidth + 8.dp
        val maxBarArea = (maxWidth - reserved).coerceAtLeast(0.dp)
        val ratio = (backgroundBytes.toDouble() / safeMax.toDouble()).coerceIn(0.0, 1.0)
        val barWidth = maxBarArea * ratio.toFloat()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "BG",
                modifier = Modifier.width(labelWidth),
                color = color,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 9.sp,
                    lineHeight = 9.sp
                ),
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (barWidth == 0.dp) {
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                Canvas(
                    modifier = Modifier
                        .width(barWidth)
                        .height(6.dp)
                ) {
                    drawLine(
                        color = color,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(
                            intervals = floatArrayOf(6.dp.toPx(), 3.dp.toPx())
                        )
                    )
                }
            }
            Text(
                text = formatMb(backgroundBytes),
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
private fun MetricSplitRow(
    label: String,
    metricBytes: Long,
    foregroundBytes: Long,
    backgroundBytes: Long,
    maxValue: Long,
    foregroundColor: Color,
    backgroundColor: Color
) {
    val splitTotal = (foregroundBytes + backgroundBytes).coerceAtLeast(0L)
    val safeMax = max(maxValue, 1L)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val labelWidth = 44.dp
        val valueWidth = 86.dp
        val reserved = labelWidth + valueWidth + 8.dp
        val maxBarArea = (maxWidth - reserved).coerceAtLeast(0.dp)
        val metricRatio = (metricBytes.toDouble() / safeMax.toDouble()).coerceIn(0.0, 1.0)
        val totalBarWidth = maxBarArea * metricRatio.toFloat()
        val fgRatio = if (splitTotal > 0L) {
            (foregroundBytes.toDouble() / splitTotal.toDouble()).coerceIn(0.0, 1.0).toFloat()
        } else {
            0f
        }
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
            StackedDataBar(
                width = totalBarWidth,
                foregroundRatio = fgRatio,
                foregroundColor = foregroundColor,
                backgroundColor = backgroundColor
            )
            Text(
                text = "${formatMbValue(foregroundBytes)} / ${formatMbValue(backgroundBytes)} MB",
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
private fun StackedDataBar(
    width: androidx.compose.ui.unit.Dp,
    foregroundRatio: Float,
    foregroundColor: Color,
    backgroundColor: Color
) {
    val shape = RoundedCornerShape(5.dp)
    val safeWidth = width.coerceAtLeast(0.dp)
    val foregroundWidth = safeWidth * foregroundRatio.coerceIn(0f, 1f)
    val backgroundWidth = safeWidth - foregroundWidth

    if (safeWidth == 0.dp) {
        Spacer(modifier = Modifier.height(16.dp))
    } else {
        Row(
            modifier = Modifier
                .width(safeWidth)
                .height(16.dp)
                .clip(shape)
                .background(backgroundColor, shape)
                .border(1.dp, Color(0xFF003049), shape)
        ) {
            DataSegment(
                letter = "F",
                width = foregroundWidth,
                color = foregroundColor
            )
            DataSegment(
                letter = "B",
                width = backgroundWidth,
                color = backgroundColor
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
    Box(
        modifier = Modifier
            .width(width)
            .height(16.dp)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (width >= 18.dp) {
            Text(
                text = letter,
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
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

private fun formatMb(bytes: Long): String = "${formatMbValue(bytes)} MB"

private fun formatMbValue(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    val pattern = when {
        mb >= 10.0 -> "%.1f"
        mb >= 0.01 -> "%.2f"
        else -> return if (bytes > 0L) "<0.01" else "0"
    }
    return String.format(Locale.getDefault(), pattern, mb)
}
