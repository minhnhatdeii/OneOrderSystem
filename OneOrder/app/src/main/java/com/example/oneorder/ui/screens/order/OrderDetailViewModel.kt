package com.example.oneorder.ui.screens.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.Order
import com.example.oneorder.data.model.OrderItemWithDetails
import com.example.oneorder.data.model.Restaurant
import com.example.oneorder.data.repository.OrderRepository
import com.example.oneorder.data.repository.RestaurantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailState(
    val isLoading: Boolean = false,
    val order: Order? = null,
    val restaurant: Restaurant? = null,
    val items: List<OrderItemWithDetails> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val restaurantRepository: RestaurantRepository
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
                    val order = orderResult.getOrNull()
                    var restaurant: Restaurant? = null
                    if (order?.tenantId != null) {
                        val restResult = restaurantRepository.getRestaurantById(order.tenantId)
                        restaurant = restResult.getOrNull()
                    }
                    
                    _uiState.value = OrderDetailState(
                        order = order,
                        restaurant = restaurant,
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
