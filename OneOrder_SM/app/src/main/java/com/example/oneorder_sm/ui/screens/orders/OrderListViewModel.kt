package com.example.oneorder_sm.ui.screens.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.usecase.GetActiveOrdersUseCase
import com.example.oneorder_sm.domain.usecase.SubscribeToOrdersUseCase
import com.example.oneorder_sm.domain.usecase.UpdateOrderStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.oneorder_sm.domain.usecase.GetOrdersPagedUseCase
import kotlinx.coroutines.flow.Flow

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val subscribeToOrdersUseCase: SubscribeToOrdersUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase,
    private val getOrdersPagedUseCase: GetOrdersPagedUseCase
) : ViewModel() {

    fun getOrdersHistory(): Flow<PagingData<Order>> {
        return getOrdersPagedUseCase().cachedIn(viewModelScope)
    }

    private val _uiState = MutableStateFlow(OrderListUiState())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init {
        fetchOrders()
        subscribeToRealtimeUpdates()
    }

    fun fetchOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = getActiveOrdersUseCase()
            result.onSuccess { orders ->
                _uiState.update { it.copy(isLoading = false, orders = orders, error = null) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
        }
    }

    private fun subscribeToRealtimeUpdates() {
        viewModelScope.launch {
            subscribeToOrdersUseCase().collectLatest { updatedOrder ->
                // On any update, refresh the whole list for consistency, 
                // or optimally update the specific item in the list.
                // For simplicity and correctness with sorting, we'll re-fetch or merge.
                // Merging locally:
                _uiState.update { currentState ->
                    val newOrders = currentState.orders.toMutableList()
                    val index = newOrders.indexOfFirst { it.id == updatedOrder.id }
                    if (index != -1) {
                         // Update existing
                         newOrders[index] = updatedOrder
                    } else {
                        // Insert new (at top if sorted by time)
                        // Ideally checking if it matches the active filter criteria?
                        // If updatedOrder status is "served" and we show served, keep it.
                        // Ideally we just re-fetch to be safe about sort order and filtering logic on server
                        // But to be snappy, we can try to merge.
                        
                        // Let's just re-fetch for now to guarantee consistency with server query
                        fetchOrders() 
                        return@collectLatest
                    }
                     currentState.copy(orders = newOrders)
                }
            }
        }
    }
}

data class OrderListUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
