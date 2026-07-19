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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedTab: PeriodTab = PeriodTab.DAILY,
    val periods: Map<PeriodTab, List<PeriodItem>> = emptyMap(),
    val hasUsageAccess: Boolean = false,
    val dailyChartPoints: List<DailyUsagePoint> = emptyList(),
    val isDailyChartLoading: Boolean = false,
    val dailyChartError: String? = null,
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
    private var dailyChartJob: Job? = null
    private var monthlyUsageJob: Job? = null
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
            dailyChartJob?.cancel()
            monthlyUsageJob?.cancel()
            _uiState.update { current ->
                current.copy(
                    dailyChartPoints = emptyList(),
                    isDailyChartLoading = false,
                    dailyChartError = null,
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

    fun refreshDailyChart() {
        dailyChartJob?.cancel()
        dailyChartJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isDailyChartLoading = true, dailyChartError = null)
            }
            try {
                val points = usageRepository.getRecentDailyBreakdown(days = 7)
                _uiState.update { current ->
                    current.copy(
                        dailyChartPoints = points,
                        isDailyChartLoading = false,
                        dailyChartError = null
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _uiState.update { current ->
                    current.copy(
                        isDailyChartLoading = false,
                        dailyChartError = throwable.message ?: "Unable to load daily usage"
                    )
                }
            }
        }
    }

    private fun refreshMonthlyUsage() {
        monthlyUsageJob?.cancel()
        monthlyUsageJob = viewModelScope.launch {
            _uiState.update { current ->
                current.copy(
                    isMonthlyUsageLoading = true,
                    monthlyUsageError = null
                )
            }
            try {
                val usedBytes = usageRepository.getCurrentMonthCellUsage()
                _uiState.update { current ->
                    current.copy(
                        monthlyUsedBytes = usedBytes,
                        monthlyProgress = computeMonthlyProgress(usedBytes, current.monthlyCapBytes),
                        isMonthlyUsageLoading = false,
                        monthlyUsageError = null
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
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
