package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.domain.model.Tenant

/**
 * Repository interface for tenant (restaurant) operations.
 */
interface TenantRepository {
    
    /**
     * Creates a new restaurant/tenant after user registration.
     * This also updates the user's profile to 'manager' role and assigns tenant_id.
     */
    suspend fun createRestaurant(
        restaurantName: String,
        address: String? = null,
        phone: String? = null,
        email: String? = null
    ): Result<Tenant>
    
    /**
     * Gets the current user's tenant information.
     */
    suspend fun getCurrentTenant(): Result<Tenant?>
    
    /**
     * Updates tenant information.
     */
    suspend fun updateTenant(
        name: String? = null,
        address: String? = null,
        phone: String? = null,
        email: String? = null,
        logoUrl: String? = null,
        coverUrl: String? = null,
        description: String? = null
    ): Result<Unit>
    
    /**
     * Uploads a new logo for the tenant.
     */
    suspend fun uploadTenantLogo(
        imageBytes: ByteArray,
        extension: String
    ): Result<String>

    /**
     * Uploads a new cover image for the tenant.
     */
    suspend fun uploadTenantCover(
        imageBytes: ByteArray,
        extension: String
    ): Result<String>
    
    /**
     * Gets tenant statistics (staff count, table count, menu item count).
     */
    suspend fun getTenantStatistics(): Result<Map<String, Any>>
}

