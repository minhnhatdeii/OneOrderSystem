package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.domain.model.Tenant
import com.example.oneorder_sm.domain.repository.TenantRepository
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class TenantRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val storage: Storage
) : TenantRepository {

    override suspend fun createRestaurant(
        restaurantName: String,
        address: String?,
        phone: String?,
        email: String?
    ): Result<Tenant> {
        return try {
            android.util.Log.d("TenantRepository", "=== CREATE RESTAURANT START ===")
            
            val userId = auth.currentUserOrNull()?.id 
            if (userId == null) {
                android.util.Log.e("TenantRepository", "User not authenticated")
                return Result.failure(Exception("Not authenticated"))
            }
            
            android.util.Log.d("TenantRepository", "User ID: $userId")
            android.util.Log.d("TenantRepository", "Restaurant Name: $restaurantName")
            android.util.Log.d("TenantRepository", "Address: $address")
            android.util.Log.d("TenantRepository", "Phone: $phone")
            android.util.Log.d("TenantRepository", "Email: $email")
            
            // Call the create_restaurant_account function
            val params = buildJsonObject {
                put("p_restaurant_name", restaurantName)
                address?.let { put("p_address", it) }
                phone?.let { put("p_phone", it) }
                email?.let { put("p_email", it) }
            }
            
            android.util.Log.d("TenantRepository", "Calling RPC: create_restaurant_account")
            android.util.Log.d("TenantRepository", "Params: $params")
            
            val tenantId = postgrest.rpc("create_restaurant_account", params)
                .decodeAs<String>()
            
            android.util.Log.d("TenantRepository", "RPC returned tenant ID: $tenantId")
            
            // Fetch the created tenant
            android.util.Log.d("TenantRepository", "Fetching created tenant...")
            val tenant = postgrest.from("tenants")
                .select {
                    filter { eq("id", tenantId) }
                }
                .decodeSingle<Tenant>()
            
            android.util.Log.d("TenantRepository", "Tenant fetched successfully: ${tenant.restaurantName}")
            android.util.Log.d("TenantRepository", "=== CREATE RESTAURANT SUCCESS ===")
            
            Result.success(tenant)
        } catch (e: Exception) {
            android.util.Log.e("TenantRepository", "=== CREATE RESTAURANT FAILED ===")
            android.util.Log.e("TenantRepository", "Error message: ${e.message}")
            android.util.Log.e("TenantRepository", "Exception details: ", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getCurrentTenant(): Result<Tenant?> {
        return try {
            val userId = auth.currentUserOrNull()?.id 
                ?: return Result.failure(Exception("Not authenticated"))
            
            // Get user's tenant_id from profile
            val profile = postgrest.from("profiles")
                .select {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<com.example.oneorder_sm.domain.model.Profile>()
            
            if (profile?.tenantId == null) {
                return Result.success(null)
            }
            
            // Fetch tenant info
            val tenant = postgrest.from("tenants")
                .select {
                    filter { eq("id", profile.tenantId) }
                }
                .decodeSingleOrNull<Tenant>()
            
            Result.success(tenant)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateTenant(
        name: String?,
        address: String?,
        phone: String?,
        email: String?,
        logoUrl: String?,
        coverUrl: String?,
        description: String?
    ): Result<Unit> {
        return try {
            // Call the update_tenant_info function
            val params = buildJsonObject {
                name?.let { put("p_restaurant_name", it) }
                address?.let { put("p_address", it) }
                phone?.let { put("p_phone", it) }
                email?.let { put("p_email", it) }
                logoUrl?.let { put("p_logo_url", it) }
                coverUrl?.let { put("p_cover_url", it) }
                description?.let { put("p_description", it) }
            }
            
            postgrest.rpc("update_tenant_info", params)
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getTenantStatistics(): Result<Map<String, Any>> {
        return try {
            val result = postgrest.rpc("get_tenant_statistics")
                .decodeAs<Map<String, Int>>()
            
            Result.success(result.mapValues { it.value as Any })
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty stats on error
            Result.success(
                mapOf(
                    "staff_count" to 0,
                    "table_count" to 0,
                    "menu_item_count" to 0
                )
            )
        }
    }

    override suspend fun uploadTenantLogo(
        imageBytes: ByteArray,
        extension: String
    ): Result<String> {
        return try {
            val tenant = getCurrentTenant().getOrNull()
                ?: return Result.failure(Exception("No tenant found"))
            
            val fileName = "tenant_${tenant.id}_${java.util.UUID.randomUUID()}.$extension"
            
            val bucket = storage["avatars"]
            bucket.upload(fileName, imageBytes, upsert = true)
            
            val publicUrl = bucket.publicUrl(fileName)
            
            // Update tenant info with new logo
            updateTenant(logoUrl = publicUrl)
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun uploadTenantCover(
        imageBytes: ByteArray,
        extension: String
    ): Result<String> {
        return try {
            val tenant = getCurrentTenant().getOrNull()
                ?: return Result.failure(Exception("No tenant found"))
            
            val fileName = "tenant_cover_${tenant.id}_${java.util.UUID.randomUUID()}.$extension"
            
            val bucket = storage["avatars"] // Using avatars bucket as we don't have a dedicated covers bucket
            bucket.upload(fileName, imageBytes, upsert = true)
            
            val publicUrl = bucket.publicUrl(fileName)
            
            // Update tenant info with new cover
            updateTenant(coverUrl = publicUrl)
            
            Result.success(publicUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}

