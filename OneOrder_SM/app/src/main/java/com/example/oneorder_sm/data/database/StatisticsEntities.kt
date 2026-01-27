package com.example.oneorder_sm.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.PopularItem

@Entity(tableName = "dashboard_summary")
data class DashboardSummaryEntity(
    @PrimaryKey val id: Int = 0, // Singleton
    val todayRevenue: Double,
    val todayOrders: Int,
    val activeOrders: Int,
    val occupiedTables: Int,
    val totalTables: Int,
    val totalStaff: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "order_statistics")
data class OrderStatisticEntity(
    @PrimaryKey val orderDate: String,
    val totalOrders: Int,
    val totalRevenue: Double,
    val pendingOrders: Int,
    val completedOrders: Int,
    val cancelledOrders: Int
)

@Entity(tableName = "popular_items")
data class PopularItemEntity(
    @PrimaryKey val menuItemId: Int,
    val itemName: String,
    val categoryName: String,
    val totalQuantity: Int,
    val totalRevenue: Double,
    val rank: Int // To maintain order
)

// Mappers
fun DashboardSummary.toEntity() = DashboardSummaryEntity(
    todayRevenue = todayRevenue,
    todayOrders = todayOrders,
    activeOrders = activeOrders,
    occupiedTables = occupiedTables,
    totalTables = totalTables,
    totalStaff = totalStaff
)

fun DashboardSummaryEntity.toModel() = DashboardSummary(
    todayRevenue = todayRevenue,
    todayOrders = todayOrders,
    activeOrders = activeOrders,
    occupiedTables = occupiedTables,
    totalTables = totalTables,
    totalStaff = totalStaff
)

fun OrderStatistic.toEntity() = OrderStatisticEntity(
    orderDate = orderDate,
    totalOrders = totalOrders,
    totalRevenue = totalRevenue,
    pendingOrders = pendingOrders,
    completedOrders = completedOrders,
    cancelledOrders = cancelledOrders
)

fun OrderStatisticEntity.toModel() = OrderStatistic(
    orderDate = orderDate,
    totalOrders = totalOrders,
    totalRevenue = totalRevenue,
    pendingOrders = pendingOrders,
    completedOrders = completedOrders,
    cancelledOrders = cancelledOrders
)

fun PopularItem.toEntity(rank: Int) = PopularItemEntity(
    menuItemId = menuItemId,
    itemName = itemName,
    categoryName = categoryName,
    totalQuantity = totalQuantity,
    totalRevenue = totalRevenue,
    rank = rank
)

fun PopularItemEntity.toModel() = PopularItem(
    menuItemId = menuItemId,
    itemName = itemName,
    categoryName = categoryName,
    totalQuantity = totalQuantity,
    totalRevenue = totalRevenue
)
