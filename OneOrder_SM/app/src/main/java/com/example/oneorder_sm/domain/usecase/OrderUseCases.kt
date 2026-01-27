package com.example.oneorder_sm.domain.usecase

import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

data class OrderUseCases @Inject constructor(
    val getActiveOrders: GetActiveOrdersUseCase,
    val getOrderDetails: GetOrderDetailsUseCase,
    val updateOrderStatus: UpdateOrderStatusUseCase,
    val subscribeToOrders: SubscribeToOrdersUseCase
)

class GetActiveOrdersUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(): Result<List<Order>> {
        return repository.getActiveOrders()
    }
}

class GetOrderDetailsUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(orderId: String): Result<Order?> {
        return repository.getOrderById(orderId)
    }
}

class UpdateOrderStatusUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, status: OrderStatus): Result<Unit> {
        return repository.updateOrderStatus(orderId, status)
    }
}

class SubscribeToOrdersUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    operator fun invoke(): Flow<Order> {
        return repository.subscribeToOrders()
    }
}
