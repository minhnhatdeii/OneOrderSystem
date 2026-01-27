package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.PopularItem

/**
 * Repository for statistics and dashboard data
 */
interface StatisticsRepository {
    
    /**
     * Get dashboard summary statistics
     */
    suspend fun getDashboardSummary(): Result<DashboardSummary>
    
    /**
     * Get order statistics for a date range
     * @param startDate Format: YYYY-MM-DD
     * @param endDate Format: YYYY-MM-DD
     */
    suspend fun getOrderStatistics(
        startDate: String,
        endDate: String
    ): Result<List<OrderStatistic>>
    
    /**
     * Get popular menu items
     * @param limit Number of items to return (default: 10)
     */
    suspend fun getPopularItems(limit: Int = 10): Result<List<PopularItem>>
}
