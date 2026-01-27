package com.example.oneorder_sm.domain.usecase

import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.PopularItem
import com.example.oneorder_sm.domain.repository.StatisticsRepository
import javax.inject.Inject

/**
 * Use case to get dashboard summary statistics
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(): Result<DashboardSummary> {
        return repository.getDashboardSummary()
    }
}

/**
 * Use case to get order statistics for a date range
 */
class GetOrderStatisticsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(
        startDate: String,
        endDate: String
    ): Result<List<OrderStatistic>> {
        return repository.getOrderStatistics(startDate, endDate)
    }
}

/**
 * Use case to get popular menu items
 */
class GetPopularItemsUseCase @Inject constructor(
    private val repository: StatisticsRepository
) {
    suspend operator fun invoke(limit: Int = 10): Result<List<PopularItem>> {
        return repository.getPopularItems(limit)
    }
}
