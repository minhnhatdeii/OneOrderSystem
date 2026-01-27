package com.example.oneorder.ui.screens.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.Order
import com.example.oneorder.data.model.OrderItemWithDetails
import com.example.oneorder.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val items: List<OrderItemWithDetails> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailState())
    val uiState: StateFlow<OrderDetailState> = _uiState.asStateFlow()

    fun loadOrderDetail(orderId: String) {
        viewModelScope.launch {
            _uiState.value = OrderDetailState(isLoading = true)
            
            try {
                // Fetch order and items in parallel
                val orderResult = orderRepository.getOrderById(orderId)
                val itemsResult = orderRepository.getOrderItems(orderId)
                
                if (orderResult.isSuccess && itemsResult.isSuccess) {
                    _uiState.value = OrderDetailState(
                        order = orderResult.getOrNull(),
                        items = itemsResult.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    val error = orderResult.exceptionOrNull() ?: itemsResult.exceptionOrNull()
                    _uiState.value = OrderDetailState(
                        isLoading = false,
                        error = error?.message ?: "Failed to load order details"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OrderDetailState(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
}
