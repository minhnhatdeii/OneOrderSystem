package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun getActiveOrders(): Result<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order?>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>
    fun subscribeToOrders(): Flow<Order>
    
    // Paging
    fun getOrdersPaged(status: OrderStatus? = null, pageSize: Int = 20): Flow<androidx.paging.PagingData<Order>>
}
