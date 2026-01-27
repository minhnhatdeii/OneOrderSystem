package com.example.oneorder_sm.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface StatisticsDao {

    // Dashboard Summary
    @Query("SELECT * FROM dashboard_summary WHERE id = 0")
    suspend fun getDashboardSummary(): DashboardSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboardSummary(summary: DashboardSummaryEntity)

    // Order Statistics
    @Query("SELECT * FROM order_statistics WHERE orderDate BETWEEN :startDate AND :endDate ORDER BY orderDate ASC")
    suspend fun getOrderStatistics(startDate: String, endDate: String): List<OrderStatisticEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderStatistics(stats: List<OrderStatisticEntity>)

    // Popular Items
    @Query("SELECT * FROM popular_items ORDER BY rank ASC LIMIT :limit")
    suspend fun getPopularItems(limit: Int): List<PopularItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPopularItems(items: List<PopularItemEntity>)

    @Query("DELETE FROM popular_items")
    suspend fun clearPopularItems()

    @Transaction
    suspend fun refreshPopularItems(items: List<PopularItemEntity>) {
        clearPopularItems()
        insertPopularItems(items)
    }
}
