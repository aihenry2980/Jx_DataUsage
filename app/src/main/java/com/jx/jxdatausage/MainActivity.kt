package com.jx.jxdatausage

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jx.jxdatausage.data.DailyUsagePoint
import com.jx.jxdatausage.data.PeriodItem
import com.jx.jxdatausage.data.PeriodTab
import com.jx.jxdatausage.ui.theme.JxDataUsageTheme
import com.jx.jxdatausage.worker.HourlyWidgetRefreshWorker
import kotlin.math.ceil
import kotlin.math.roundToInt
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        HourlyWidgetRefreshWorker.cancel(applicationContext)
        setContent {
            JxDataUsageTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                MainScreen(
                    state = uiState,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                            isLoading = state.isDailyChartLoading
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
    isLoading: Boolean
) {
    val cellColor = Color(0xFF1976D2)
    val wifiColor = Color(0xFFD32F2F)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "DAILY USAGE",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (isLoading) {
                Text(
                    text = "Loading chart...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
                return@Column
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem(label = "Cell", color = cellColor)
                LegendItem(label = "WiFi", color = wifiColor)
            }
            Text(
                text = "Unit: GB",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall
            )

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
                        .width(34.dp)
                        .fillMaxSize()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (step in steps downTo 0) {
                        val value = (chartMaxMb / steps * step).roundToInt()
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.labelSmall
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
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    BarWithValue(
                                        valueGb = point.cellBytes / (1024.0 * 1024.0 * 1024.0),
                                        barHeight = chartHeight * cellFraction,
                                        color = cellColor
                                    )
                                    BarWithValue(
                                        valueGb = point.wifiBytes / (1024.0 * 1024.0 * 1024.0),
                                        barHeight = chartHeight * wifiFraction,
                                        color = wifiColor
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
                                style = MaterialTheme.typography.labelSmall
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
    valueGb: Double,
    barHeight: Dp,
    color: Color
) {
    Column(
        modifier = Modifier.width(20.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = String.format(Locale.getDefault(), "%.2f", valueGb),
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
                .background(color)
        )
    }
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
