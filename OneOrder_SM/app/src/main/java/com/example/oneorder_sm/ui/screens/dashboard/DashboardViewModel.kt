package com.example.oneorder_sm.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import com.example.oneorder_sm.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val summary: DashboardSummary? = null,
    val recentOrders: List<Order> = emptyList(),
    val lastRefreshTime: Long = System.currentTimeMillis()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    private val orderRepository: OrderRepository
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
                // Load summary (today's data)
                val summaryResult = getDashboardSummary()
                var summary = summaryResult.getOrNull()

                // Load recent orders (take 5 most recent)
                val ordersResult = orderRepository.getActiveOrders()
                val allOrders = ordersResult.getOrNull() ?: emptyList()

                // Recalculate active orders and occupied tables from real-time data
                if (summary != null) {
                    val activeOrderList = allOrders.filter { 
                        it.status == OrderStatus.PENDING ||
                        it.status == OrderStatus.CONFIRMED ||
                        it.status == OrderStatus.PREPARING ||
                        it.status == OrderStatus.SERVED
                    }
                    summary = summary.copy(
                        activeOrders = activeOrderList.size,
                        occupiedTables = activeOrderList.mapNotNull { it.tableId }.distinct().size
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        recentOrders = allOrders.take(5),
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
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val summaryResult = getDashboardSummary()
                var summary = summaryResult.getOrNull()
                
                val ordersResult = orderRepository.getActiveOrders()
                val allOrders = ordersResult.getOrNull() ?: emptyList()

                if (summary != null) {
                    val activeOrderList = allOrders.filter { 
                        it.status == OrderStatus.PENDING ||
                        it.status == OrderStatus.CONFIRMED ||
                        it.status == OrderStatus.PREPARING ||
                        it.status == OrderStatus.SERVED
                    }
                    summary = summary.copy(
                        activeOrders = activeOrderList.size,
                        occupiedTables = activeOrderList.mapNotNull { it.tableId }.distinct().size
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        recentOrders = allOrders.take(5),
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
}
