package com.example.oneorder_sm.ui.screens.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import com.example.oneorder_sm.domain.usecase.UpdateOrderNoteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    private val updateOrderNoteUseCase: UpdateOrderNoteUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()
    
    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            orderRepository.getOrderById(orderId)
                .onSuccess { order ->
                    _uiState.update {
                        it.copy(
                            order = order,
                            isLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to load order"
                        )
                    }
                }
        }
    }
    
    fun updateStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            orderRepository.updateOrderStatus(orderId, newStatus)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Đã cập nhật trạng thái đơn hàng"
                        )
                    }
                    // Reload order to get updated status
                    loadOrder(orderId)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to update status"
                        )
                    }
                }
        }
    }

    fun saveNote(orderId: String, note: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingNote = true, error = null) }
            val noteToSave = note.trim().ifBlank { null }
            updateOrderNoteUseCase(orderId, noteToSave)
                .onSuccess {
                    // Update local state immediately for instant feedback
                    _uiState.update { state ->
                        state.copy(
                            isSavingNote = false,
                            successMessage = "Đã lưu ghi chú",
                            order = state.order?.copy(staffNote = noteToSave)
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSavingNote = false,
                            error = "Lưu ghi chú thất bại: ${error.message}"
                        )
                    }
                }
        }
    }
    
    fun clearMessages() {
        _uiState.update { it.copy(successMessage = null, error = null) }
    }
}
