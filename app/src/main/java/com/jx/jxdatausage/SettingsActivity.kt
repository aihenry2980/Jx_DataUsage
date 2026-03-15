package com.jx.jxdatausage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jx.jxdatausage.data.DataUnit
import com.jx.jxdatausage.data.SettingsRepository
import com.jx.jxdatausage.data.ThemeMode
import com.jx.jxdatausage.ui.theme.JxDataUsageTheme
import com.jx.jxdatausage.util.formatCapInput
import com.jx.jxdatausage.util.parseMonthlyCapBytes
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsRepository = remember { SettingsRepository(applicationContext) }
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            JxDataUsageTheme(themeMode = themeMode) {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsRepository = remember(context) { SettingsRepository(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val storedCapBytes by settingsRepository.monthlyCapBytes.collectAsStateWithLifecycle(initialValue = null)
    val storedCapUnit by settingsRepository.monthlyCapUnit.collectAsStateWithLifecycle(initialValue = DataUnit.GB)
    val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
    var capInput by rememberSaveable { mutableStateOf("") }
    var selectedUnit by rememberSaveable { mutableStateOf(DataUnit.GB) }
    var hasInitializedInput by rememberSaveable { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(storedCapBytes, storedCapUnit) {
        if (!hasInitializedInput) {
            selectedUnit = storedCapUnit
            capInput = storedCapBytes?.let { formatCapInput(it, storedCapUnit) }.orEmpty()
            hasInitializedInput = true
        }
    }

    val parsedCapBytes = remember(capInput, selectedUnit) {
        parseMonthlyCapBytes(capInput, selectedUnit)
    }
    val hasInput = capInput.isNotBlank()
    val inputError = if (hasInput && parsedCapBytes == null) {
        stringResource(id = R.string.monthly_cap_invalid)
    } else {
        null
    }

    fun refreshFormFromStore() {
        selectedUnit = storedCapUnit
        capInput = storedCapBytes?.let { formatCapInput(it, storedCapUnit) }.orEmpty()
        feedbackMessage = null
        hasInitializedInput = true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                refreshFormFromStore()
                isRefreshing = false
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.theme_mode_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                UnitChip(
                                    text = when (mode) {
                                        ThemeMode.LIGHT -> stringResource(id = R.string.theme_light)
                                        ThemeMode.DARK -> stringResource(id = R.string.theme_dark)
                                        ThemeMode.SYSTEM -> stringResource(id = R.string.theme_system)
                                    },
                                    selected = mode == themeMode,
                                    onClick = {
                                        coroutineScope.launch {
                                            settingsRepository.setThemeMode(mode)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.total_usable_data_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        OutlinedTextField(
                            value = capInput,
                            onValueChange = { value ->
                                capInput = value
                                feedbackMessage = null
                                hasInitializedInput = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(text = stringResource(id = R.string.monthly_cap_input_label)) },
                            placeholder = { Text(text = stringResource(id = R.string.monthly_cap_input_placeholder)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            isError = inputError != null,
                            supportingText = {
                                if (inputError != null) {
                                    Text(text = inputError)
                                }
                            }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitChip(
                                text = "MB",
                                selected = selectedUnit == DataUnit.MB,
                                onClick = {
                                    selectedUnit = DataUnit.MB
                                    hasInitializedInput = true
                                }
                            )
                            UnitChip(
                                text = "GB",
                                selected = selectedUnit == DataUnit.GB,
                                onClick = {
                                    selectedUnit = DataUnit.GB
                                    hasInitializedInput = true
                                }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = parsedCapBytes != null,
                                onClick = {
                                    val bytes = parsedCapBytes ?: return@Button
                                    coroutineScope.launch {
                                        settingsRepository.setMonthlyCap(bytes, selectedUnit)
                                        feedbackMessage = context.getString(R.string.monthly_cap_saved)
                                    }
                                }
                            ) {
                                Text(text = stringResource(id = R.string.save))
                            }
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        settingsRepository.clearMonthlyCap()
                                        capInput = ""
                                        hasInitializedInput = true
                                        feedbackMessage = context.getString(R.string.monthly_cap_cleared)
                                    }
                                }
                            ) {
                                Text(text = stringResource(id = R.string.clear))
                            }
                        }
                        if (!feedbackMessage.isNullOrBlank()) {
                            Text(
                                text = feedbackMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.ad_placeholder),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (selected) colorScheme.primary else colorScheme.outline
    val background = if (selected) colorScheme.primaryContainer else colorScheme.surface
    val contentColor = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(background, shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = contentColor
        )
    }
}
