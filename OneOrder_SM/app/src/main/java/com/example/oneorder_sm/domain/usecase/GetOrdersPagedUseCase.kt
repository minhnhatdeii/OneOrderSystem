package com.example.oneorder_sm.domain.usecase

import androidx.paging.PagingData
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOrdersPagedUseCase @Inject constructor(
    private val repository: OrderRepository
) {
    operator fun invoke(status: OrderStatus? = null, pageSize: Int = 20): Flow<PagingData<Order>> {
        return repository.getOrdersPaged(status, pageSize)
    }
}
