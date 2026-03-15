package com.jx.jxdatausage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jx.jxdatausage.data.DailyUsagePoint
import com.jx.jxdatausage.data.PeriodGenerator
import com.jx.jxdatausage.data.PeriodItem
import com.jx.jxdatausage.data.PeriodTab
import com.jx.jxdatausage.data.SettingsRepository
import com.jx.jxdatausage.data.UsageAccessHelper
import com.jx.jxdatausage.data.UsageRepository
import com.jx.jxdatausage.util.computeMonthlyProgress
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedTab: PeriodTab = PeriodTab.DAILY,
    val periods: Map<PeriodTab, List<PeriodItem>> = emptyMap(),
    val hasUsageAccess: Boolean = false,
    val dailyChartPoints: List<DailyUsagePoint> = emptyList(),
    val isDailyChartLoading: Boolean = false,
    val monthlyUsedBytes: Long = 0L,
    val monthlyCapBytes: Long? = null,
    val monthlyProgress: Float = 0f,
    val isMonthlyUsageLoading: Boolean = false,
    val monthlyUsageError: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext
    private val usageRepository = UsageRepository(appContext)
    private val settingsRepository = SettingsRepository(appContext)
    private val periodsByTab = PeriodTab.entries.associateWith { tab ->
        PeriodGenerator.generatePeriods(tab)
    }

    private val _uiState = MutableStateFlow(
        MainUiState(
            periods = periodsByTab
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        observeMonthlyCap()
        refreshPermissionState()
    }

    fun selectTab(tab: PeriodTab) {
        _uiState.update { current -> current.copy(selectedTab = tab) }
    }

    fun refreshPermissionState() {
        val hasAccess = UsageAccessHelper.hasUsageAccess(appContext)
        _uiState.update { current -> current.copy(hasUsageAccess = hasAccess) }
        if (hasAccess) {
            refreshDailyChart()
            refreshMonthlyUsage()
        } else {
            _uiState.update { current ->
                current.copy(
                    dailyChartPoints = emptyList(),
                    isDailyChartLoading = false,
                    isMonthlyUsageLoading = false,
                    monthlyUsageError = null
                )
            }
        }
    }

    private fun observeMonthlyCap() {
        viewModelScope.launch {
            settingsRepository.monthlyCapBytes.collect { capBytes ->
                _uiState.update { current ->
                    current.copy(
                        monthlyCapBytes = capBytes,
                        monthlyProgress = computeMonthlyProgress(current.monthlyUsedBytes, capBytes)
                    )
                }
            }
        }
    }

    private fun refreshDailyChart() {
        viewModelScope.launch {
            _uiState.update { current -> current.copy(isDailyChartLoading = true) }
            val points = usageRepository.getRecentDailyBreakdown(days = 7)
            _uiState.update { current ->
                current.copy(
                    dailyChartPoints = points,
                    isDailyChartLoading = false
                )
            }
        }
    }

    private fun refreshMonthlyUsage() {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isMonthlyUsageLoading = true,
                    monthlyUsageError = null
                )
            }
            runCatching {
                usageRepository.getCurrentMonthCellUsage()
            }.onSuccess { usedBytes ->
                _uiState.update { current ->
                    current.copy(
                        monthlyUsedBytes = usedBytes,
                        monthlyProgress = computeMonthlyProgress(usedBytes, current.monthlyCapBytes),
                        isMonthlyUsageLoading = false,
                        monthlyUsageError = null
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { current ->
                    current.copy(
                        isMonthlyUsageLoading = false,
                        monthlyUsageError = throwable.message ?: "Unable to load monthly usage"
                    )
                }
            }
        }
    }
}
