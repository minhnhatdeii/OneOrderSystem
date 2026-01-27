package com.example.oneorder_sm.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    @SerialName("pending") PENDING,
    @SerialName("confirmed") CONFIRMED,
    @SerialName("preparing") PREPARING,
    @SerialName("served") SERVED,
    @SerialName("cancelled") CANCELLED,
    @SerialName("paid") PAID
}

@Serializable
enum class PaymentStatus {
    @SerialName("unpaid") UNPAID,
    @SerialName("paid") PAID
}

@Serializable
data class Order(
    val id: String,
    @SerialName("tenant_id") val tenantId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("table_id") val tableId: Int? = null,
    @SerialName("table_name") val tableName: String? = null,  // Table name for display
    @SerialName("total_amount") val totalAmount: Double,
    val status: OrderStatus,
    @SerialName("payment_status") val paymentStatus: PaymentStatus,
    val note: String? = null, // Customer note for entire order
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("order_items") val orderItems: List<OrderItem> = emptyList()
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("menu_item_id") val menuItemId: Int? = null,
    @SerialName("item_name") val itemName: String? = null,  // Item name for display
    val quantity: Int,
    @SerialName("price_at_time") val priceAtTime: Double,
    val note: String? = null
)
