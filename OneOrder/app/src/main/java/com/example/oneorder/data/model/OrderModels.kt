package com.example.oneorder.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String? = null, // UUID
    @SerialName("user_id") val userId: String,
    @SerialName("tenant_id") val tenantId: String? = null, // Restaurant ID
    @SerialName("table_id") val tableId: Long? = null,
    @SerialName("total_amount") val totalAmount: Double,
    val status: String = "pending",
    @SerialName("payment_status") val paymentStatus: String = "unpaid",
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class OrderItem(
    val id: String? = null,
    @SerialName("order_id") val orderId: String,
    @SerialName("menu_item_id") val menuItemId: Long,
    val quantity: Int,
    @SerialName("price_at_time") val priceAtTime: Double,
    val note: String? = null
)

/**
 * Combined model for displaying order items with menu details
 */
data class OrderItemWithDetails(
    val orderItem: OrderItem,
    val menuItemName: String,
    val menuItemImage: String?
)

