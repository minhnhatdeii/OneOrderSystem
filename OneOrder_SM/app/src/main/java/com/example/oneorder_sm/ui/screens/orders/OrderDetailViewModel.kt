package com.example.oneorder_sm.ui.screens.orders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.usecase.GetOrderDetailsUseCase
import com.example.oneorder_sm.domain.usecase.UpdateOrderStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val getOrderDetailsUseCase: GetOrderDetailsUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])
    
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init {
        fetchOrderDetails()
    }

    fun fetchOrderDetails() {
         viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getOrderDetailsUseCase(orderId)
            result.onSuccess { order ->
                if (order != null) {
                    _uiState.update { it.copy(isLoading = false, order = order, error = null) }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Order not found") }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    fun updateStatus(newStatus: OrderStatus) {
        Log.d("OrderDetailVM", "=== UPDATE STATUS CALLED ===")
        Log.d("OrderDetailVM", "Order ID: $orderId")
        Log.d("OrderDetailVM", "Current Status: ${_uiState.value.order?.status}")
        Log.d("OrderDetailVM", "New Status: $newStatus")
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            Log.d("OrderDetailVM", "Calling updateOrderStatusUseCase...")
            
            val result = updateOrderStatusUseCase(orderId, newStatus)
            
            result.onSuccess {
                Log.d("OrderDetailVM", "✅ UseCase returned success")
                Log.d("OrderDetailVM", "Fetching updated order details...")
                // Refresh details to reflect changes
                fetchOrderDetails() 
            }.onFailure { error ->
                Log.e("OrderDetailVM", "❌ UseCase returned failure")
                Log.e("OrderDetailVM", "Error: ${error.message}", error)
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }
}

data class OrderDetailUiState(
    val order: Order? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
