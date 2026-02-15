package com.jx.jxdatausage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jx.jxdatausage.data.AppUsageRow
import com.jx.jxdatausage.data.PeriodTab
import com.jx.jxdatausage.data.SortDirection
import com.jx.jxdatausage.data.UsageSortMode
import com.jx.jxdatausage.data.UsageRepository
import com.jx.jxdatausage.data.sortUsageRows
import com.jx.jxdatausage.ui.components.AppIconView
import com.jx.jxdatausage.ui.components.UsageBars
import com.jx.jxdatausage.ui.theme.JxDataUsageTheme

class UsageDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val periodTab = intent.getStringExtra(EXTRA_PERIOD_TAB)?.let {
            runCatching { PeriodTab.valueOf(it) }.getOrNull()
        } ?: PeriodTab.DAILY
        val startMs = intent.getLongExtra(EXTRA_START_MS, 0L)
        val endMs = intent.getLongExtra(EXTRA_END_MS, System.currentTimeMillis())
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { periodTab.name }

        setContent {
            JxDataUsageTheme {
                val usageRepository = remember { UsageRepository(applicationContext) }
                var showWifi by rememberSaveable { mutableStateOf(false) }
                var isLoading by remember { mutableStateOf(true) }
                var usageRows by remember { mutableStateOf(emptyList<AppUsageRow>()) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(startMs, endMs) {
                    isLoading = true
                    errorMessage = null
                    usageRows = emptyList()
                    runCatching {
                        usageRepository.getUsageForRangeBatched(
                            startMs = startMs,
                            endMs = endMs,
                            includeWifiInSort = true,
                            batchSize = 20
                        ).collect { partialRows ->
                            usageRows = partialRows
                            isLoading = false
                        }
                    }.onFailure { throwable ->
                        errorMessage = throwable.message ?: "Failed to load usage data"
                    }
                    isLoading = false
                }

                UsageDetailScreen(
                    title = title,
                    showWifi = showWifi,
                    onShowWifiChanged = { showWifi = it },
                    usageRows = usageRows,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onBack = { finish() }
                )
            }
        }
    }

    companion object {
        const val EXTRA_PERIOD_TAB = "period_tab"
        const val EXTRA_START_MS = "start_ms"
        const val EXTRA_END_MS = "end_ms"
        const val EXTRA_TITLE = "title"

        fun createIntent(
            context: Context,
            periodTab: PeriodTab,
            startMs: Long,
            endMs: Long,
            title: String
        ): Intent {
            return Intent(context, UsageDetailActivity::class.java)
                .putExtra(EXTRA_PERIOD_TAB, periodTab.name)
                .putExtra(EXTRA_START_MS, startMs)
                .putExtra(EXTRA_END_MS, endMs)
                .putExtra(EXTRA_TITLE, title)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageDetailScreen(
    title: String,
    showWifi: Boolean,
    onShowWifiChanged: (Boolean) -> Unit,
    usageRows: List<AppUsageRow>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit
) {
    var sortMode by remember { mutableStateOf(UsageSortMode.TOTAL) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESC) }
    val sortedRows = remember(usageRows, showWifi, sortMode, sortDirection) {
        sortUsageRows(
            rows = usageRows,
            sortMode = sortMode,
            includeWifi = showWifi,
            sortDirection = sortDirection
        )
    }
    val maxMobile = usageRows.maxOfOrNull { it.mobile.totalBytes } ?: 1L
    val maxWifi = usageRows.maxOfOrNull { it.wifi?.totalBytes ?: 0L } ?: 1L
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Text(
                text = "Loading usage...",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
            return@Scaffold
        }

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
            return@Scaffold
        }

        if (sortedRows.isEmpty()) {
            Text(
                text = "No usage data found for this period.",
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                SortSection(
                    selectedMode = sortMode,
                    sortDirection = sortDirection,
                    showWifi = showWifi,
                    onSortModeSelected = { sortMode = it },
                    onSortDirectionToggle = {
                        sortDirection = if (sortDirection == SortDirection.DESC) {
                            SortDirection.ASC
                        } else {
                            SortDirection.DESC
                        }
                    },
                    onShowWifiToggle = { onShowWifiChanged(!showWifi) }
                )
            }
            items(items = sortedRows, key = { "${it.uid}-${it.packageName}" }) { row ->
                AppUsageCard(
                    row = row,
                    showWifi = showWifi,
                    maxMobile = maxMobile,
                    maxWifi = maxWifi
                )
            }
        }
    }
}

@Composable
private fun SortSection(
    selectedMode: UsageSortMode,
    sortDirection: SortDirection,
    showWifi: Boolean,
    onSortModeSelected: (UsageSortMode) -> Unit,
    onSortDirectionToggle: () -> Unit,
    onShowWifiToggle: () -> Unit
) {
    val sortModes = remember { UsageSortMode.entries }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallSortButton(
                    text = if (sortDirection == SortDirection.DESC) "Desc" else "Asc",
                    selected = sortDirection == SortDirection.ASC,
                    icon = if (sortDirection == SortDirection.DESC) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    onClick = onSortDirectionToggle
                )
                SmallSortButton(
                    text = if (showWifi) "WiFi On" else "WiFi Off",
                    selected = showWifi,
                    onClick = onShowWifiToggle
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortModes.forEach { mode ->
                SmallSortButton(
                    text = mode.label,
                    selected = mode == selectedMode,
                    onClick = { onSortModeSelected(mode) },
                    horizontalPadding = 6.dp,
                    verticalPadding = 4.dp,
                    textSizeSp = 10f
                )
            }
        }
    }
}

@Composable
private fun SmallSortButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    horizontalPadding: androidx.compose.ui.unit.Dp = 8.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 6.dp,
    textSizeSp: Float = 12f
) {
    val shape = RoundedCornerShape(10.dp)
    val borderColor = if (selected) Color(0xFFD38B8B) else Color(0xFF9E8F8F)
    val background = if (selected) Color(0xFFF3D1D1) else Color.Transparent
    Row(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.width(12.dp)
            )
        }
        Text(
            text = text,
            fontSize = textSizeSp.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppUsageCard(
    row: AppUsageRow,
    showWifi: Boolean,
    maxMobile: Long,
    maxWifi: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.width(96.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AppIconView(packageName = row.iconRef)
                Text(
                    text = row.appName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                UsageBars(
                    usage = row.mobile,
                    split = row.mobileSplit,
                    maxValue = maxMobile,
                    title = "Mobile"
                )
                if (showWifi && row.wifi != null) {
                    UsageBars(
                        usage = row.wifi,
                        split = row.wifiSplit,
                        maxValue = maxWifi,
                        title = "WiFi"
                    )
                }
            }
        }
    }
}
