package com.example.oneorder.ui.screens.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.repository.CartManager
import com.example.oneorder.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val orderId: String? = null,
    val error: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartManager: CartManager,
    private val orderRepository: OrderRepository,
    private val restaurantStateManager: com.example.oneorder.data.repository.RestaurantStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutState())
    val uiState: StateFlow<CheckoutState> = _uiState.asStateFlow()

    fun placeOrder() {
        viewModelScope.launch {
            _uiState.value = CheckoutState(isLoading = true)
            val items = cartManager.cartItems.value
            val total = cartManager.getTotalPrice()

            if (items.isEmpty()) {
                _uiState.value = CheckoutState(error = "Cart is empty")
                return@launch
            }
            
            // Get tenant_id and table_id from RestaurantStateManager
            val restaurant = restaurantStateManager.restaurant.value
            val table = restaurantStateManager.table.value
            
            if (restaurant == null || table == null) {
                _uiState.value = CheckoutState(error = "Restaurant or table information not available")
                return@launch
            }

            val result = orderRepository.createOrder(
                items = items,
                totalAmount = total,
                tenantId = restaurant.id,
                tableId = table.id
            )
            
            if (result.isSuccess) {
                cartManager.clearCart()
                _uiState.value = CheckoutState(isSuccess = true, orderId = result.getOrNull())
            } else {
                _uiState.value = CheckoutState(error = result.exceptionOrNull()?.message ?: "Order Placement Failed")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = CheckoutState()
    }
}
