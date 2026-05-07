package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.PopularItem
import com.example.oneorder_sm.domain.repository.StatisticsRepository
import com.example.oneorder_sm.data.database.*
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

// Resilience4j
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.retry.Retry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration

class StatisticsRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val circuitBreaker: CircuitBreaker,
    private val retry: Retry,
    private val statisticsDao: StatisticsDao
) : StatisticsRepository {

    // Simple in‑memory cache entries
    private data class CacheEntry<T>(val value: T, val timestamp: Long)
    private val dashboardCache = mutableMapOf<String, CacheEntry<DashboardSummary>>()
    private val orderStatsCache = mutableMapOf<String, CacheEntry<List<OrderStatistic>>>()
    private val popularItemsCache = mutableMapOf<Int, CacheEntry<List<PopularItem>>>()
    private val ttlMillis = 30 * 1000L // 30 seconds (reduced from 5 minutes for fresher data)

    private suspend fun <T> withResilience(block: suspend () -> T): Result<T> = try {
        // Since we don't have resilience4j-kotlin, we use runBlocking inside IO
        // This is safe because we are already in Dispatchers.IO
        val result = withContext(Dispatchers.IO) {
            circuitBreaker.executeSupplier {
                retry.executeSupplier {
                    kotlinx.coroutines.runBlocking {
                        block()
                    }
                }
            }
        }
        Result.success(result)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getDashboardSummary(): Result<DashboardSummary> {
        // Query directly from orders and tables — avoids RPC return-type mismatch and stale cache
        return try {
            val vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh")
            val todayVn = java.time.LocalDate.now(vnZone)

            val allOrders = postgrest.from("orders")
                .select()
                .decodeList<Order>()

            // Parse createdAt safely — DB can return ISO-8601 with offset (+07:00) or trailing Z
            fun parseOrderDate(createdAt: String): java.time.LocalDate? = try {
                java.time.OffsetDateTime.parse(createdAt)
                    .atZoneSameInstant(vnZone)
                    .toLocalDate()
            } catch (_: Exception) {
                try {
                    java.time.Instant.parse(createdAt)
                        .atZone(vnZone)
                        .toLocalDate()
                } catch (_: Exception) { null }
            }

            val todayRevenueOrders = allOrders.filter { order ->
                parseOrderDate(order.createdAt) == todayVn &&
                    order.status != OrderStatus.CANCELLED
            }

            val todayRevenue = todayRevenueOrders.sumOf { it.totalAmount }
            val todayOrderCount = allOrders.count { parseOrderDate(it.createdAt) == todayVn }
            val activeOrderCount = allOrders.count { order ->
                order.status == OrderStatus.PENDING ||
                order.status == OrderStatus.CONFIRMED ||
                order.status == OrderStatus.PREPARING ||
                order.status == OrderStatus.SERVED
            }

            // Query total tables count from the tables table
            val totalTablesCount = try {
                postgrest.from("tables").select().decodeList<kotlinx.serialization.json.JsonObject>().size
            } catch (_: Exception) { 0 }

            val summary = DashboardSummary(
                todayRevenue = todayRevenue,
                todayOrders = todayOrderCount,
                activeOrders = activeOrderCount,
                occupiedTables = 0, // Will be recalculated in ViewModel from real-time active orders
                totalTables = totalTablesCount,
                totalStaff = 0
            )

            try { statisticsDao.insertDashboardSummary(summary.toEntity()) } catch (_: Exception) {}

            Result.success(summary)
        } catch (e: Exception) {
            try {
                val localData = statisticsDao.getDashboardSummary()
                if (localData != null) return Result.success(localData.toModel())
            } catch (_: Exception) {}
            Result.failure(e)
        }
    }

    override suspend fun getOrderStatistics(
        startDate: String,
        endDate: String
    ): Result<List<OrderStatistic>> {
        val cacheKey = "${startDate}_$endDate"
        val now = System.currentTimeMillis()
        
        // 1. Memory Cache
        orderStatsCache[cacheKey]?.let { entry ->
            if (now - entry.timestamp < ttlMillis) return Result.success(entry.value)
        }
        
        // 2. Network
        val networkResult = withResilience {
            val params = buildJsonObject {
                put("p_start_date", startDate)
                put("p_end_date", endDate)
            }
            val result = postgrest.rpc("get_order_statistics", params)
                .decodeList<OrderStatistic>()
            result
        }

        if (networkResult.isSuccess) {
            val data = networkResult.getOrThrow()
            orderStatsCache[cacheKey] = CacheEntry(data, now)
            try {
                // Note: This replaces all stats for this range. 
                // A better approach might be more granular, but for cache this works.
                statisticsDao.insertOrderStatistics(data.map { it.toEntity() })
            } catch (e: Exception) { e.printStackTrace() }
            return Result.success(data)
        }

        // 3. Fallback Local (Only for this exact range? Or query range?)
        // For simplicity, we query the DB for the range.
        try {
            val localData = statisticsDao.getOrderStatistics(startDate, endDate)
            if (localData.isNotEmpty()) {
                return Result.success(localData.map { it.toModel() })
            }
        } catch (e: Exception) { e.printStackTrace() }

        return networkResult
    }

    override suspend fun getPopularItems(limit: Int): Result<List<PopularItem>> {
        val now = System.currentTimeMillis()
        
        // 1. Memory
        popularItemsCache[limit]?.let { entry ->
            if (now - entry.timestamp < ttlMillis) return Result.success(entry.value)
        }
        
        // 2. Network
        val networkResult = withResilience {
            val params = buildJsonObject {
                put("p_limit", limit)
            }
            val result = postgrest.rpc("get_popular_items", params)
                .decodeList<PopularItem>()
            result
        }
        
        if (networkResult.isSuccess) {
            val data = networkResult.getOrThrow()
            popularItemsCache[limit] = CacheEntry(data, now)
            try {
                // Refresh local cache for popular items
                statisticsDao.refreshPopularItems(data.mapIndexed { index, item -> item.toEntity(index) })
            } catch (e: Exception) { e.printStackTrace() }
            return Result.success(data)
        }
        
        // 3. Fallback Local
        try {
            val localData = statisticsDao.getPopularItems(limit)
            if (localData.isNotEmpty()) {
                return Result.success(localData.map { it.toModel() })
            }
        } catch (e: Exception) { e.printStackTrace() }

        return networkResult
    }
}
