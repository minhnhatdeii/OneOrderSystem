package com.example.oneorder_sm.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.PopularItem
import com.example.oneorder_sm.domain.usecase.GetDashboardSummaryUseCase
import com.example.oneorder_sm.domain.usecase.GetOrderStatisticsUseCase
import com.example.oneorder_sm.domain.usecase.GetPopularItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class DateRange {
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: DashboardSummary? = null,
    val orderStats: List<OrderStatistic> = emptyList(),
    val popularItems: List<PopularItem> = emptyList(),
    val selectedDateRange: DateRange = DateRange.LAST_7_DAYS,
    val lastRefreshTime: Long = System.currentTimeMillis()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val getOrderStatistics: GetOrderStatisticsUseCase,
    private val getPopularItems: GetPopularItemsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(10_000) // 10 seconds - faster refresh for real-time feel
                refreshData()
            }
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load summary
                val summaryResult = getDashboardSummary()
                
                // Calculate date range
                val (startDate, endDate) = getDateRangeStrings(_uiState.value.selectedDateRange)
                
                // Load order statistics
                val statsResult = getOrderStatistics(startDate, endDate)
                
                // Load popular items
                val popularResult = getPopularItems(10)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summaryResult.getOrNull(),
                        orderStats = statsResult.getOrNull() ?: emptyList(),
                        popularItems = popularResult.getOrNull() ?: emptyList(),
                        lastRefreshTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Lỗi tải dữ liệu"
                    )
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            // Cancel any ongoing auto-refresh cycle temporarily
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Force immediate load
                val summaryResult = getDashboardSummary()
                val (startDate, endDate) = getDateRangeStrings(_uiState.value.selectedDateRange)
                val statsResult = getOrderStatistics(startDate, endDate)
                val popularResult = getPopularItems(10)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summaryResult.getOrNull(),
                        orderStats = statsResult.getOrNull() ?: emptyList(),
                        popularItems = popularResult.getOrNull() ?: emptyList(),
                        lastRefreshTime = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Lỗi tải dữ liệu"
                    )
                }
            }
        }
    }

    fun selectDateRange(range: DateRange) {
        _uiState.update { it.copy(selectedDateRange = range) }
        loadDashboardData()
    }

    private fun getDateRangeStrings(range: DateRange): Pair<String, String> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val today = LocalDate.now()
        
        val (start, end) = when (range) {
            DateRange.TODAY -> today to today
            DateRange.LAST_7_DAYS -> today.minusDays(6) to today
            DateRange.LAST_30_DAYS -> today.minusDays(29) to today
        }
        
        return start.format(formatter) to end.format(formatter)
    }
}
