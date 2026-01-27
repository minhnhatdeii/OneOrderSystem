package com.example.oneorder.data.repository

import com.example.oneorder.data.model.CartItem
import com.example.oneorder.data.model.Order
import com.example.oneorder.data.model.OrderItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order as PostgrestOrder
import javax.inject.Inject

interface OrderRepository {
    suspend fun createOrder(
        items: List<CartItem>, 
        totalAmount: Double,
        tenantId: String,
        tableId: Long
    ): Result<String> // Returns Order ID
    suspend fun getOrders(): Result<List<Order>>
    suspend fun getOrderById(orderId: String): Result<Order>
    suspend fun getOrderItems(orderId: String): Result<List<com.example.oneorder.data.model.OrderItemWithDetails>>
}

class OrderRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : OrderRepository {

    override suspend fun createOrder(
        items: List<CartItem>, 
        totalAmount: Double,
        tenantId: String,
        tableId: Long
    ): Result<String> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

            // 1. Create Order with tenant_id and table_id
            val order = Order(
                userId = currentUser.id,
                tenantId = tenantId,
                tableId = tableId,
                totalAmount = totalAmount,
                status = "pending"
            )

            // Insert and return single to get ID (assuming RLS allows read after write)
            val orderResponse = supabase.postgrest.from("orders").insert(order) {
                select()
            }.decodeSingle<Order>()

            val orderId = orderResponse.id ?: throw Exception("Failed to retrieve Order ID")

            // 2. Create Order Items
            val orderItems = items.map { cartItem ->
                OrderItem(
                    orderId = orderId,
                    menuItemId = cartItem.menuItem.id,
                    quantity = cartItem.quantity,
                    priceAtTime = cartItem.menuItem.price,
                    note = cartItem.note
                )
            }

            supabase.postgrest.from("order_items").insert(orderItems)

            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrders(): Result<List<Order>> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val orders = supabase.postgrest.from("orders").select {
                filter {
                    eq("user_id", currentUser.id)
                }
                order(column = "created_at", order = PostgrestOrder.DESCENDING)
            }.decodeList<Order>()
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOrderById(orderId: String): Result<Order> {
        return try {
            val order = supabase.postgrest.from("orders").select {
                filter {
                    eq("id", orderId)
                }
            }.decodeSingle<Order>()
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getOrderItems(orderId: String): Result<List<com.example.oneorder.data.model.OrderItemWithDetails>> {
        return try {
            // Fetch order items
            val orderItems = supabase.postgrest.from("order_items").select {
                filter {
                    eq("order_id", orderId)
                }
            }.decodeList<OrderItem>()
            
            // For each order item, fetch the menu item details
            val itemsWithDetails = orderItems.map { orderItem ->
                val menuItem = supabase.postgrest.from("menu_items").select {
                    filter {
                        eq("id", orderItem.menuItemId)
                    }
                }.decodeSingleOrNull<com.example.oneorder.data.model.MenuItem>()
                
                com.example.oneorder.data.model.OrderItemWithDetails(
                    orderItem = orderItem,
                    menuItemName = menuItem?.name ?: "Unknown Item",
                    menuItemImage = menuItem?.image_url
                )
            }
            
            Result.success(itemsWithDetails)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
