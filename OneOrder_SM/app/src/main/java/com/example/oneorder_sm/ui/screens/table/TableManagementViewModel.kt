package com.example.oneorder_sm.ui.screens.table

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.Table
import com.example.oneorder_sm.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TableListState(
    val isLoading: Boolean = false,
    val tables: List<Table> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val generatedQRCode: Bitmap? = null,
    val selectedTableOrder: Order? = null,
    val isLoadingOrder: Boolean = false
)

@HiltViewModel
class TableManagementViewModel @Inject constructor(
    private val getTablesUseCase: GetTablesUseCase,
    private val addTableUseCase: AddTableUseCase,
    private val updateTableUseCase: UpdateTableUseCase,
    private val deleteTableUseCase: DeleteTableUseCase,
    private val updateTableStatusUseCase: UpdateTableStatusUseCase,
    private val generateQRCodeUseCase: GenerateQRCodeUseCase,
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase,
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableListState())
    val uiState: StateFlow<TableListState> = _uiState.asStateFlow()

    init {
        loadTables()
    }

    fun loadTables() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = getTablesUseCase()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    tables = result.getOrDefault(emptyList())
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun updateStatus(tableId: Long, newStatus: String) {
        viewModelScope.launch {
            val result = updateTableStatusUseCase(tableId, newStatus)
            if (result.isSuccess) {
                // Update local state immediately for better UX
                val updatedTables = _uiState.value.tables.map {
                    if (it.id == tableId) it.copy(status = newStatus) else it
                }
                _uiState.value = _uiState.value.copy(tables = updatedTables)
            } else {
                _uiState.value = _uiState.value.copy(error = "Failed to update: " + result.exceptionOrNull()?.message)
            }
        }
    }

    fun saveTable(
        id: Long?,
        tableName: String,
        capacity: Int = 4,
        location: String? = null,
        status: String = "free"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            val table = Table(
                id = id ?: 0,
                name = tableName,
                status = status,
                capacity = capacity,
                location = location
            )

            val result = if (id == null || id == 0L) {
                addTableUseCase(table)
            } else {
                updateTableUseCase(table)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (id == null) "Table added successfully" else "Table updated successfully"
                )
                loadTables()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun deleteTable(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = deleteTableUseCase(id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Table deleted successfully")
                loadTables()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun generateQRCode(tableId: Long) {
        viewModelScope.launch {
            val result = generateQRCodeUseCase(tableId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(generatedQRCode = result.getOrNull())
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun clearQRCode() {
        _uiState.value = _uiState.value.copy(generatedQRCode = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun loadOrderForTable(tableId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingOrder = true, selectedTableOrder = null)
            val result = getActiveOrdersUseCase()
            if (result.isSuccess) {
                val activeOrders = result.getOrDefault(emptyList())
                val tableOrder = activeOrders
                    .filter { it.status != com.example.oneorder_sm.data.model.OrderStatus.CANCELLED && it.status != com.example.oneorder_sm.data.model.OrderStatus.PAID }
                    .find { it.tableId?.toLong() == tableId }
                _uiState.value = _uiState.value.copy(
                    selectedTableOrder = tableOrder,
                    isLoadingOrder = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoadingOrder = false,
                    error = "Failed to load order: " + result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearSelectedOrder() {
        _uiState.value = _uiState.value.copy(selectedTableOrder = null)
    }

    fun checkoutTable(tableId: Long, orderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = updateOrderStatusUseCase(orderId, com.example.oneorder_sm.data.model.OrderStatus.PAID)
            if (result.isSuccess) {
                // Now mark table as free
                updateStatus(tableId, "free")
                _uiState.value = _uiState.value.copy(successMessage = "Thanh toán thành công!")
                clearSelectedOrder()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi thanh toán: " + result.exceptionOrNull()?.message
                )
            }
        }
    }
}
