package com.example.oneorder_sm.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.domain.repository.OrderRepository
import com.example.oneorder_sm.domain.usecase.GetOrderStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import javax.inject.Inject

data class DayPerformance(
    val date: LocalDate,
    val dayLabel: String,
    val dayOfMonth: Int,
    val revenue: Double,
    val revenueLabel: String
)

data class IncomeHistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    // Month summary
    val currentMonth: YearMonth = YearMonth.now(),
    val monthRevenue: Double = 0.0,
    val monthOrderCount: Int = 0,
    val avgTicket: Double = 0.0,

    // Daily performance chips
    val dayPerformances: List<DayPerformance> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),

    // Order history for selected date
    val selectedDateOrders: List<Order> = emptyList(),
    val isLoadingOrders: Boolean = false,

    // Month stats for mini chart
    val monthStats: List<OrderStatistic> = emptyList()
)

@HiltViewModel
class IncomeHistoryViewModel @Inject constructor(
    private val getOrderStatistics: GetOrderStatisticsUseCase,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomeHistoryUiState())
    val uiState: StateFlow<IncomeHistoryUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val today = LocalDate.now()
                val currentMonth = YearMonth.now()
                
                val monthStart = currentMonth.atDay(1)
                val monthEnd = currentMonth.atEndOfMonth()
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE

                val statsResult = getOrderStatistics(
                    monthStart.format(formatter),
                    monthEnd.format(formatter)
                )

                val allStats = statsResult.getOrNull() ?: emptyList()
                
                // Filter stats strictly for the current month just to be safe
                val monthStrPrefix = currentMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                val monthStats = allStats.filter { it.orderDate.startsWith(monthStrPrefix) }
                
                val monthRevenue = monthStats.sumOf { it.totalRevenue }
                val monthOrderCount = monthStats.sumOf { it.totalOrders }
                val avgTicket = if (monthOrderCount > 0) monthRevenue / monthOrderCount else 0.0

                // Generate day performance chips for the entire month
                val dayPerformances = generateDayPerformances(monthStats, monthStart, monthEnd)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentMonth = currentMonth,
                        monthRevenue = monthRevenue,
                        monthOrderCount = monthOrderCount,
                        avgTicket = avgTicket,
                        monthStats = monthStats,
                        dayPerformances = dayPerformances,
                        selectedDate = today
                    )
                }

                loadOrdersForDate(today)

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

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadOrdersForDate(date)
    }

    fun selectDateFromCalendar(year: Int, month: Int, dayOfMonth: Int) {
        val selectedDate = LocalDate.of(year, month, dayOfMonth)
        val selectedMonth = YearMonth.of(year, month)
        
        viewModelScope.launch {
            if (selectedMonth == _uiState.value.currentMonth) {
                // If it's the same month as currently loaded, just update the selected date
                _uiState.update { it.copy(selectedDate = selectedDate) }
                loadOrdersForDate(selectedDate)
            } else {
                // Different month: load entire selected month
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val startDate = selectedMonth.atDay(1)
                    val endDate = selectedMonth.atEndOfMonth()
                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                    
                    val statsResult = getOrderStatistics(
                        startDate.format(formatter),
                        endDate.format(formatter)
                    )
                    
                    val monthStats = statsResult.getOrNull() ?: emptyList()
                    val monthRevenue = monthStats.sumOf { it.totalRevenue }
                    val monthOrderCount = monthStats.sumOf { it.totalOrders }
                    val avgTicket = if (monthOrderCount > 0) monthRevenue / monthOrderCount else 0.0
                    
                    val dayPerformances = generateDayPerformances(monthStats, startDate, endDate)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentMonth = selectedMonth,
                            monthRevenue = monthRevenue,
                            monthOrderCount = monthOrderCount,
                            avgTicket = avgTicket,
                            monthStats = monthStats,
                            dayPerformances = dayPerformances,
                            selectedDate = selectedDate
                        )
                    }
                    loadOrdersForDate(selectedDate)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
            }
        }
    }

    private fun loadOrdersForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOrders = true) }

            try {
                // Get all active orders (this might need to be modified if getActiveOrders only gets today's orders)
                // Assuming it gets history as well or we filter client-side for simplicity here
                val ordersResult = orderRepository.getActiveOrders() 
                val allOrders = ordersResult.getOrNull() ?: emptyList()

                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val filteredOrders = allOrders.filter { order ->
                    order.createdAt.startsWith(dateStr)
                }

                _uiState.update {
                    it.copy(
                        isLoadingOrders = false,
                        selectedDateOrders = filteredOrders
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingOrders = false,
                        selectedDateOrders = emptyList()
                    )
                }
            }
        }
    }

    private fun generateDayPerformances(
        stats: List<OrderStatistic>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<DayPerformance> {
        val statsMap = stats.associateBy { it.orderDate }
        val performances = mutableListOf<DayPerformance>()

        var current = startDate
        while (!current.isAfter(endDate)) {
            val dateStr = current.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val stat = statsMap[dateStr]
            val revenue = stat?.totalRevenue ?: 0.0

            performances.add(
                DayPerformance(
                    date = current,
                    dayLabel = getVietnameseDayOfWeek(current),
                    dayOfMonth = current.dayOfMonth,
                    revenue = revenue,
                    revenueLabel = formatShortCurrency(revenue)
                )
            )
            current = current.plusDays(1)
        }

        return performances
    }

    private fun minDate(a: LocalDate, b: LocalDate): LocalDate {
        return if (a.isBefore(b)) a else b
    }

    private fun getVietnameseDayOfWeek(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "T2"
            2 -> "T3"
            3 -> "T4"
            4 -> "T5"
            5 -> "T6"
            6 -> "T7"
            7 -> "CN"
            else -> ""
        }
    }

    private fun formatShortCurrency(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> String.format(Locale.US, "%.1ftỷ", amount / 1_000_000_000).replace(".0", "")
            amount >= 1_000_000 -> String.format(Locale.US, "%.1ftr", amount / 1_000_000).replace(".0", "")
            amount >= 1_000 -> String.format(Locale.US, "%.0fk", amount / 1_000)
            amount > 0 -> String.format(Locale.US, "%.0f₫", amount)
            else -> "0₫"
        }
    }
}
