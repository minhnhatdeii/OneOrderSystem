package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order as PostgrestOrder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import android.util.Log
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : OrderRepository {

    override suspend fun getActiveOrders(): Result<List<Order>> {
        return try {
            val orders = supabaseClient.postgrest.from("orders_with_details")
                .select() {
                    filter {
                         isIn("status", listOf("pending", "confirmed", "preparing", "served"))
                    }
                    order("created_at", order = PostgrestOrder.DESCENDING)
                }.decodeList<Order>()
            Result.success(orders)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getOrderById(orderId: String): Result<Order?> {
        return try {
             val order = supabaseClient.postgrest.from("orders_with_details")
                .select() {
                    filter {
                        eq("id", orderId)
                    }
                }.decodeSingleOrNull<Order>()
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        Log.d("OrderRepository", "=== UPDATE ORDER STATUS ===")
        Log.d("OrderRepository", "Order ID: '$orderId'")
        Log.d("OrderRepository", "New Status: $status")
        Log.d("OrderRepository", "Status Type: ${status::class.simpleName}")
        Log.d("OrderRepository", "Status String Value: '${status.toString()}'")
        
        return try {
            // First, check if order exists
            val existingOrder = supabaseClient.postgrest.from("orders")
                .select {
                    filter { eq("id", orderId) }
                }.decodeSingleOrNull<Order>()
            
            Log.d("OrderRepository", "Existing order found: ${existingOrder != null}")
            if (existingOrder != null) {
                Log.d("OrderRepository", "Current order status: ${existingOrder.status}")
            } else {
                Log.e("OrderRepository", "Order not found in database!")
                return Result.failure(Exception("Order not found: $orderId"))
            }
            
            // Perform the update
            Log.d("OrderRepository", "Attempting to update order...")
            supabaseClient.postgrest.from("orders").update(
                {
                    set("status", status)
                }
            ) {
                filter {
                    eq("id", orderId)
                }
            }
            
            Log.d("OrderRepository", "✅ Update successful")
            
            // Verify the update
            val updatedOrder = supabaseClient.postgrest.from("orders")
                .select {
                    filter { eq("id", orderId) }
                }.decodeSingleOrNull<Order>()
            
            if (updatedOrder != null) {
                Log.d("OrderRepository", "Verified new status: ${updatedOrder.status}")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("OrderRepository", "❌ UPDATE FAILED", e)
            Log.e("OrderRepository", "Error type: ${e::class.simpleName}")
            Log.e("OrderRepository", "Error message: ${e.message}")
            Log.e("OrderRepository", "Stack trace:")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override fun subscribeToOrders(): Flow<Order> {
        val channel = supabaseClient.realtime.channel("orders")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "orders"
        }.mapNotNull {
            try {
                when (it) {
                    is PostgresAction.Insert -> it.decodeRecord<Order>()
                    is PostgresAction.Update -> it.decodeRecord<Order>()
                    else -> null
                }
            } catch(e: Exception) {
                e.printStackTrace()
                null 
            }
        }
    }

    override fun getOrdersPaged(status: OrderStatus?, pageSize: Int): Flow<androidx.paging.PagingData<Order>> {
        return androidx.paging.Pager(
            config = androidx.paging.PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { com.example.oneorder_sm.data.pagination.OrderPagingSource(supabaseClient.postgrest, status) }
        ).flow
    }
}
