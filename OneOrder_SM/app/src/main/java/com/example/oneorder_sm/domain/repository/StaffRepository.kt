package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.domain.model.Profile

/**
 * Repository interface for staff management operations.
 * Direct account creation - no invitation flow.
 */
interface StaffRepository {
    
    /**
     * Gets list of all staff members in the current tenant.
     */
    suspend fun getStaffList(): Result<List<Profile>>
    
    /**
     * Creates a staff account directly.
     * @return The temporary password (123456)
     */
    suspend fun createStaffAccount(
        email: String,
        fullName: String,
        phone: String?,
        role: String // "staff" or "manager"
    ): Result<String>
    
    /**
     * Deactivates a staff account (soft delete).
     */
    suspend fun deactivateStaff(staffId: String): Result<Unit>
    
    /**
     * Reactivates a previously deactivated staff account.
     */
    suspend fun reactivateStaff(staffId: String): Result<Unit>
    
    /**
     * Updates staff information.
     */
    suspend fun updateStaff(
        staffId: String,
        fullName: String? = null,
        phone: String? = null,
        role: String? = null
    ): Result<Unit>
}

