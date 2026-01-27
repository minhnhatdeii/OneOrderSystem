package com.example.oneorder_sm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Dashboard summary statistics
 */
@Serializable
data class DashboardSummary(
    @SerialName("today_revenue")
    val todayRevenue: Double = 0.0,
    @SerialName("today_orders")
    val todayOrders: Int = 0,
    @SerialName("active_orders")
    val activeOrders: Int = 0,
    @SerialName("occupied_tables")
    val occupiedTables: Int = 0,
    @SerialName("total_tables")
    val totalTables: Int = 0,
    @SerialName("total_staff")
    val totalStaff: Int = 0
)

/**
 * Order statistics for a specific date
 */
@Serializable
data class OrderStatistic(
    @SerialName("order_date")
    val orderDate: String,
    @SerialName("total_orders")
    val totalOrders: Int = 0,
    @SerialName("total_revenue")
    val totalRevenue: Double = 0.0,
    @SerialName("pending_orders")
    val pendingOrders: Int = 0,
    @SerialName("completed_orders")
    val completedOrders: Int = 0,
    @SerialName("cancelled_orders")
    val cancelledOrders: Int = 0
)

/**
 * Popular menu item statistics
 */
@Serializable
data class PopularItem(
    @SerialName("menu_item_id")
    val menuItemId: Int,
    @SerialName("item_name")
    val itemName: String,
    @SerialName("category_name")
    val categoryName: String,
    @SerialName("total_quantity")
    val totalQuantity: Int = 0,
    @SerialName("total_revenue")
    val totalRevenue: Double = 0.0
)
