package com.example.oneorder.data.repository

import android.util.Log
import com.example.oneorder.data.model.Restaurant
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Tenant/Restaurant model for Supabase
 */
@Serializable
private data class TenantDto(
    val id: String,
    @SerialName("owner_id")
    val ownerId: String? = null,
    @SerialName("restaurant_name")
    val restaurantName: String,
    @SerialName("business_type")
    val businessType: String? = null,
    val address: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    val email: String? = null,
    @SerialName("logo_url")
    val logoUrl: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    @SerialName("is_active")
    val isActive: Boolean? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

interface RestaurantRepository {
    suspend fun getRestaurantById(restaurantId: String): Result<Restaurant>
}

class RestaurantRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : RestaurantRepository {
    
    override suspend fun getRestaurantById(restaurantId: String): Result<Restaurant> {
        return try {
            Log.d("RestaurantRepository", "=== FETCHING RESTAURANT ===")
            Log.d("RestaurantRepository", "Restaurant ID: '$restaurantId'")
            Log.d("RestaurantRepository", "Restaurant ID length: ${restaurantId.length}")
            Log.d("RestaurantRepository", "Restaurant ID is blank: ${restaurantId.isBlank()}")
            Log.d("RestaurantRepository", "Restaurant ID equals 'null': ${restaurantId == "null"}")
            
            // First, try to fetch ALL tenants to compare IDs
            try {
                val allTenants = supabase.postgrest.from("tenants")
                    .select()
                    .decodeList<TenantDto>()
                
                Log.d("RestaurantRepository", "=== DATABASE TENANTS ===" )
                Log.d("RestaurantRepository", "Total tenants in database: ${allTenants.size}")
                
                if (allTenants.isEmpty()) {
                    Log.w("RestaurantRepository", "⚠️ NO TENANTS FOUND IN DATABASE!")
                } else {
                    allTenants.forEachIndexed { index, tenant ->
                        Log.d("RestaurantRepository", "Tenant #$index: id='${tenant.id}', name='${tenant.restaurantName}'")
                        // Check if this tenant ID matches what we're looking for
                        if (tenant.id == restaurantId) {
                            Log.d("RestaurantRepository", "✅ MATCH FOUND at index $index")
                        }
                    }
                }
            } catch (listError: Exception) {
                Log.e("RestaurantRepository", "Failed to list all tenants (for debugging)", listError)
            }
            
            // Now try the specific lookup
            Log.d("RestaurantRepository", "=== ATTEMPTING SPECIFIC LOOKUP ===")
            val tenant = supabase.postgrest.from("tenants")
                .select {
                    filter { eq("id", restaurantId) }
                }
                .decodeSingleOrNull<TenantDto>()
            
            if (tenant != null) {
                Log.d("RestaurantRepository", "=== RESTAURANT FOUND ===")
                Log.d("RestaurantRepository", "Restaurant name: ${tenant.restaurantName}")
                Log.d("RestaurantRepository", "Restaurant ID from DB: ${tenant.id}")
                Result.success(
                    Restaurant(
                        id = tenant.id,
                        name = tenant.restaurantName,
                        description = tenant.businessType,
                        imageUrl = tenant.logoUrl,
                        address = tenant.address
                    )
                )
            } else {
                Log.e("RestaurantRepository", "=== RESTAURANT NOT FOUND ===")
                Log.e("RestaurantRepository", "❌ No tenant found with ID: '$restaurantId'")
                Log.e("RestaurantRepository", "Possible causes:")
                Log.e("RestaurantRepository", "1. Table has NULL or incorrect tenant_id")
                Log.e("RestaurantRepository", "2. Tenant was deleted from database")
                Log.e("RestaurantRepository", "3. QR code format issue (e.g., contains 'null' as string)")
                Result.failure(Exception("Restaurant not found"))
            }
        } catch (e: Exception) {
            Log.e("RestaurantRepository", "=== ERROR FETCHING RESTAURANT ===", e)
            Log.e("RestaurantRepository", "Exception type: ${e.javaClass.simpleName}")
            Log.e("RestaurantRepository", "Exception message: ${e.message}")
            Result.failure(e)
        }
    }
}
