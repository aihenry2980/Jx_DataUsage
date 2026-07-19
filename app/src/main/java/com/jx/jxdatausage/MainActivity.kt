package com.jx.jxdatausage

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jx.jxdatausage.data.DailyUsagePoint
import com.jx.jxdatausage.data.PeriodItem
import com.jx.jxdatausage.data.PeriodTab
import com.jx.jxdatausage.data.SettingsRepository
import com.jx.jxdatausage.ui.theme.JxDataUsageTheme
import com.jx.jxdatausage.util.formatBytes
import com.jx.jxdatausage.worker.HourlyWidgetRefreshWorker
import kotlin.math.ceil
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HourlyWidgetRefreshWorker.cancel(applicationContext)
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = com.jx.jxdatausage.data.ThemeMode.SYSTEM
            )
            JxDataUsageTheme(themeMode = themeMode) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    state = uiState,
                    onRefresh = viewModel::refreshPermissionState,
                    onDailyChartRetry = viewModel::refreshDailyChart,
                    onSettingsClick = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onTabSelected = viewModel::selectTab,
                    onPeriodClick = { period ->
                        startActivity(
                            UsageDetailActivity.createIntent(
                                context = this,
                                periodTab = period.tab,
                                startMs = period.startMs,
                                endMs = period.endMs,
                                title = period.label
                            )
                        )
                    },
                    onGrantUsageAccess = {
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissionState()
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainScreen(
    state: MainUiState,
    onRefresh: () -> Unit,
    onDailyChartRetry: () -> Unit,
    onSettingsClick: () -> Unit,
    onTabSelected: (PeriodTab) -> Unit,
    onPeriodClick: (PeriodItem) -> Unit,
    onGrantUsageAccess: () -> Unit
) {
    val tabs = listOf(
        PeriodTab.DAILY to stringResource(id = R.string.daily),
        PeriodTab.WEEKLY to stringResource(id = R.string.this_week),
        PeriodTab.MONTHLY to stringResource(id = R.string.this_month),
        PeriodTab.YEARLY to stringResource(id = R.string.this_year)
    )
    val selectedPeriods = state.periods[state.selectedTab].orEmpty()
    val isRefreshing = state.isDailyChartLoading || state.isMonthlyUsageLoading

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                MonthlyDataPlanCard(
                    state = state,
                    onOpenSettings = onSettingsClick
                )
                if (!state.hasUsageAccess) {
                    UsageAccessCard(onGrantUsageAccess = onGrantUsageAccess)
                }
                TabRow(
                    selectedTabIndex = tabs.indexOfFirst { it.first == state.selectedTab }
                ) {
                    tabs.forEach { (tab, label) ->
                        Tab(
                            selected = state.selectedTab == tab,
                            onClick = { onTabSelected(tab) },
                            text = { Text(text = label) }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.selectedTab == PeriodTab.DAILY) {
                        item {
                            DailyUsageChartCard(
                                points = state.dailyChartPoints,
                                isLoading = state.isDailyChartLoading,
                                errorMessage = state.dailyChartError,
                                onRetry = onDailyChartRetry
                            )
                        }
                    }
                    items(selectedPeriods, key = { it.id }) { period ->
                        PeriodItemCard(period = period, onClick = { onPeriodClick(period) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyDataPlanCard(
    state: MainUiState,
    onOpenSettings: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(16.dp)
    val isOverCap = state.monthlyProgress > 1f
    val progressColor = if (isOverCap) colorScheme.error else colorScheme.primary
    val containerColor = colorScheme.primaryContainer
    val borderColor = colorScheme.outline
    val gradientColors = listOf(
        lerp(colorScheme.primaryContainer, colorScheme.surface, 0.12f),
        lerp(colorScheme.primaryContainer, colorScheme.surface, 0.32f)
    )
    val titleColor = colorScheme.onPrimaryContainer
    val bodyColor = colorScheme.onPrimaryContainer
    val markerColor = colorScheme.onPrimaryContainer
    val trackColor = lerp(colorScheme.primaryContainer, colorScheme.primary, 0.22f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors
                    ),
                    shape = cardShape
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.monthly_plan_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                    if (state.monthlyCapBytes != null && state.monthlyCapBytes > 0L && state.hasUsageAccess && state.monthlyUsageError.isNullOrBlank()) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f%%", state.monthlyProgress * 100f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = progressColor
                        )
                    }
                }

                when {
                    !state.hasUsageAccess -> {
                        Text(
                            text = stringResource(id = R.string.monthly_plan_permission_required),
                            color = bodyColor
                        )
                    }

                    state.isMonthlyUsageLoading -> {
                        Text(
                            text = stringResource(id = R.string.monthly_plan_loading),
                            color = bodyColor
                        )
                    }

                    !state.monthlyUsageError.isNullOrBlank() -> {
                        Text(
                            text = stringResource(
                                id = R.string.monthly_plan_error_prefix,
                                state.monthlyUsageError.orEmpty()
                            ),
                            color = bodyColor
                        )
                    }

                    state.monthlyCapBytes == null || state.monthlyCapBytes <= 0L -> {
                        Text(
                            text = stringResource(
                                id = R.string.monthly_plan_cap_unset,
                                formatBytes(state.monthlyUsedBytes)
                            ),
                            color = bodyColor
                        )
                        TextButton(onClick = onOpenSettings) {
                            Text(
                                text = stringResource(id = R.string.set_total_usable_data),
                                color = colorScheme.primary
                            )
                        }
                    }

                    else -> {
                        val cap = state.monthlyCapBytes ?: 0L
                        val percent = state.monthlyProgress * 100f
                        val today = LocalDate.now(ZoneId.systemDefault())
                        val todayFraction = (
                            today.dayOfMonth.toFloat() / today.lengthOfMonth().toFloat()
                            ).coerceIn(0f, 1f)
                        Text(
                            text = stringResource(
                                id = R.string.monthly_plan_used_summary,
                                formatBytes(state.monthlyUsedBytes),
                                formatBytes(cap),
                                String.format(Locale.getDefault(), "%.1f", percent)
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = bodyColor
                        )
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val markerOffset = (maxWidth - 28.dp) * todayFraction
                            LinearProgressIndicator(
                                progress = { state.monthlyProgress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(99.dp))
                                    .border(1.dp, borderColor, RoundedCornerShape(99.dp)),
                                color = progressColor,
                                trackColor = trackColor
                            )
                            Column(
                                modifier = Modifier
                                    .offset(x = markerOffset)
                                    .zIndex(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${today.dayOfMonth}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = markerColor
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Current day marker",
                                    tint = markerColor,
                                    modifier = Modifier
                                        .offset(y = (-5).dp)
                                        .height(18.dp)
                                )
                            }
                        }
                        if (state.monthlyProgress > 1f) {
                            val overage = (state.monthlyUsedBytes - cap).coerceAtLeast(0L)
                            Text(
                                text = stringResource(
                                    id = R.string.monthly_plan_over_by,
                                    formatBytes(overage)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageAccessCard(onGrantUsageAccess: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(id = R.string.usage_access_required))
            TextButton(onClick = onGrantUsageAccess) {
                Text(text = stringResource(id = R.string.grant_usage_access))
            }
        }
    }
}

@Composable
private fun DailyUsageChartCard(
    points: List<DailyUsagePoint>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    val cellColor = Color(0xFF1976D2)
    val wifiColor = Color(0xFFD32F2F)
    val todayHighlightColor = MaterialTheme.colorScheme.primary
    val mutedCellColor = lerp(cellColor, MaterialTheme.colorScheme.surface, 0.38f)
    val mutedWifiColor = lerp(wifiColor, MaterialTheme.colorScheme.surface, 0.38f)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "DAILY USAGE",
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (!isLoading && points.isNotEmpty()) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(label = "Cell", color = cellColor)
                        LegendItem(label = "WiFi", color = wifiColor)
                    }
                }
            }

            if (isLoading && points.isEmpty()) {
                Text(
                    text = "Loading chart...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }
            if (!errorMessage.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.daily_chart_error, errorMessage),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    TextButton(onClick = onRetry) {
                        Text(text = stringResource(id = R.string.retry))
                    }
                }
                if (points.isEmpty()) return@Column
            }
            if (points.isEmpty()) {
                Text(
                    text = "No daily usage data yet",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
            }

            val cellMbPoints = points.map { it.cellBytes / (1024f * 1024f) }
            val wifiMbPoints = points.map { it.wifiBytes / (1024f * 1024f) }
            val maxMb = maxOf(
                cellMbPoints.maxOrNull() ?: 0f,
                wifiMbPoints.maxOrNull() ?: 0f
            ).coerceAtLeast(1f)
            val steps = 5
            val stepMb = ceil(maxMb / steps).coerceAtLeast(1f)
            val chartMaxMb = stepMb * steps

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (step in steps downTo 0) {
                        val value = (chartMaxMb / steps * step).roundToInt()
                        Text(
                            text = if (step == steps) "$value MB" else value.toString(),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val chartHeight: Dp = maxHeight
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            for (step in 0..steps) {
                                val y = size.height - (step / steps.toFloat()) * size.height
                                drawLine(
                                    color = Color(0xFFBBBBBB),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 1f
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            points.forEach { point ->
                                val cellMb = point.cellBytes / (1024f * 1024f)
                                val wifiMb = point.wifiBytes / (1024f * 1024f)
                                val cellFraction = (cellMb / chartMaxMb).coerceIn(0f, 1f)
                                val wifiFraction = (wifiMb / chartMaxMb).coerceIn(0f, 1f)
                                val pointCellColor = if (point.isToday) cellColor else mutedCellColor
                                val pointWifiColor = if (point.isToday) wifiColor else mutedWifiColor
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    BarWithValue(
                                        valueMb = cellMb.toDouble(),
                                        barHeight = chartHeight * cellFraction,
                                        color = pointCellColor
                                    )
                                    BarWithValue(
                                        valueMb = wifiMb.toDouble(),
                                        barHeight = chartHeight * wifiFraction,
                                        color = pointWifiColor
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        points.forEach { point ->
                            Text(
                                text = point.label,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (point.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (point.isToday) {
                                    todayHighlightColor
                                } else {
                                    Color.Unspecified
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color)
        )
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun BarWithValue(
    valueMb: Double,
    barHeight: Dp,
    color: Color
) {
    val barShape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
    val metallicGradient = Brush.verticalGradient(
        colorStops = arrayOf(
            0f to lerp(color, Color.White, 0.38f),
            0.18f to lerp(color, Color.White, 0.12f),
            0.48f to color,
            0.72f to lerp(color, Color.Black, 0.12f),
            1f to lerp(color, Color.Black, 0.32f)
        )
    )
    val sideHighlight = Brush.horizontalGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(alpha = 0.32f),
            0.22f to Color.White.copy(alpha = 0.10f),
            0.55f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.18f)
        )
    )
    Column(
        modifier = Modifier.width(20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatChartMb(valueMb),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                lineHeight = 8.sp
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .width(8.dp)
                .height(barHeight)
                .clip(barShape)
                .background(metallicGradient)
                .background(sideHighlight)
                .border(
                    width = 0.5.dp,
                    color = lerp(color, Color.Black, 0.28f),
                    shape = barShape
                )
        )
    }
}

private fun formatChartMb(valueMb: Double): String {
    val pattern = when {
        valueMb >= 100.0 -> "%.0f"
        valueMb >= 10.0 -> "%.1f"
        else -> "%.2f"
    }
    return String.format(Locale.getDefault(), pattern, valueMb)
}

@Composable
private fun PeriodItemCard(
    period: PeriodItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = period.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap to view app usage details",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
