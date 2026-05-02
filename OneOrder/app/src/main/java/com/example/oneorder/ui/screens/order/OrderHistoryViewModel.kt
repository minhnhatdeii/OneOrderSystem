package com.example.oneorder.ui.screens.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.Order
import com.example.oneorder.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.oneorder.data.model.Restaurant
import com.example.oneorder.data.repository.RestaurantRepository

data class OrderHistoryState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val restaurants: Map<String, Restaurant> = emptyMap(),
    val searchQuery: String = "",
    val filterStatus: String? = null,
    val filterDate: String? = null,
    val error: String? = null
) {
    val visibleOrders: List<Order>
        get() = orders.filter { order ->
            val matchesSearch = order.id?.contains(searchQuery, ignoreCase = true) == true ||
                order.totalAmount.toString().contains(searchQuery)
            val matchesFilter = filterStatus == null || order.status.equals(filterStatus, ignoreCase = true)
            val matchesDate = filterDate == null || order.createdAt?.take(10) == filterDate
            matchesSearch && matchesFilter && matchesDate
        }
}

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryState())
    val uiState: StateFlow<OrderHistoryState> = _uiState.asStateFlow()

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateFilterStatus(status: String?) {
        _uiState.value = _uiState.value.copy(filterStatus = status)
    }

    fun updateFilterDate(date: String?) {
        _uiState.value = _uiState.value.copy(filterDate = date)
    }

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            android.util.Log.d("OrderHistoryViewModel", "=== LOADING ORDERS ===")
            _uiState.value = OrderHistoryState(isLoading = true)
            
            try {
                val result = orderRepository.getOrders()
                
                if (result.isSuccess) {
                    val orders = result.getOrDefault(emptyList())
                    android.util.Log.d("OrderHistoryViewModel", "✅ Successfully loaded ${orders.size} orders")
                    
                    // Fetch restaurants for these orders
                    val tenantIds = orders.mapNotNull { it.tenantId }.distinct()
                    val restaurantsMap = mutableMapOf<String, Restaurant>()
                    
                    for (tenantId in tenantIds) {
                        try {
                            val restResult = restaurantRepository.getRestaurantById(tenantId)
                            if (restResult.isSuccess) {
                                restResult.getOrNull()?.let { restaurantsMap[tenantId] = it }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("OrderHistoryViewModel", "Failed to fetch restaurant $tenantId", e)
                        }
                    }

                    _uiState.value = OrderHistoryState(
                        orders = orders,
                        restaurants = restaurantsMap,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    android.util.Log.e("OrderHistoryViewModel", "=== ERROR LOADING ORDERS ===")
                    android.util.Log.e("OrderHistoryViewModel", "Error type: ${error?.javaClass?.simpleName}")
                    android.util.Log.e("OrderHistoryViewModel", "Error message: ${error?.message}")
                    android.util.Log.e("OrderHistoryViewModel", "Full error:", error)
                    
                    _uiState.value = OrderHistoryState(
                        isLoading = false,
                        error = error?.message ?: "Unknown error loading orders"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OrderHistoryViewModel", "=== UNEXPECTED ERROR ===", e)
                _uiState.value = OrderHistoryState(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
}
