package com.jx.jxdatausage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jx.jxdatausage.data.DailyUsagePoint
import com.jx.jxdatausage.data.PeriodGenerator
import com.jx.jxdatausage.data.PeriodItem
import com.jx.jxdatausage.data.PeriodTab
import com.jx.jxdatausage.data.UsageAccessHelper
import com.jx.jxdatausage.data.UsageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val selectedTab: PeriodTab = PeriodTab.DAILY,
    val periods: Map<PeriodTab, List<PeriodItem>> = emptyMap(),
    val hasUsageAccess: Boolean = false,
    val dailyChartPoints: List<DailyUsagePoint> = emptyList(),
    val isDailyChartLoading: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = getApplication<Application>().applicationContext
    private val usageRepository = UsageRepository(appContext)
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
        } else {
            _uiState.update { current ->
                current.copy(
                    dailyChartPoints = emptyList(),
                    isDailyChartLoading = false
                )
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
}

