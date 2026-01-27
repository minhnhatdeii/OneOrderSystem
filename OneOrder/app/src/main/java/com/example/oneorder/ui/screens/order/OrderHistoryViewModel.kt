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

data class OrderHistoryState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryState())
    val uiState: StateFlow<OrderHistoryState> = _uiState.asStateFlow()

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
                    orders.forEachIndexed { index, order ->
                        android.util.Log.d("OrderHistoryViewModel", "Order #$index: id=${order.id}, status=${order.status}, total=${order.totalAmount}")
                    }
                    
                    _uiState.value = OrderHistoryState(
                        orders = orders,
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
