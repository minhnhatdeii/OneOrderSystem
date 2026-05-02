package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<Profile?>
    
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun getCurrentProfile(): Profile?
    suspend fun refreshSession(): Result<Unit>
    
    /**
     * Registers a new user account.
     * This creates an auth.users entry and a basic profile.
     * After registration, call TenantRepository.createRestaurant() to complete setup.
     */
    suspend fun register(
        email: String, 
        password: String, 
        fullName: String,
        restaurantName: String? = null
    ): Result<Unit>
    
    /**
     * Checks if the current user has a tenant (restaurant) associated.
     * Returns true if the user is a manager/staff with a tenant_id.
     */
    suspend fun hasTenant(): Boolean
    
    /**
     * Sends a password reset email to the given address.
     * Uses Supabase's built-in resetPasswordForEmail with redirect to auth-callback.
     */
    suspend fun resetPassword(email: String): Result<Unit>
    
    /**
     * Changes the current user's password.
     * Used when user is already logged in and wants to change their password.
     */
    suspend fun changePassword(newPassword: String): Result<Unit>

    /**
     * Changes password using a recovery token from a password reset email.
     * This exchanges the one-time recovery token for a session before updating the password.
     */
    suspend fun changePasswordWithToken(token: String, newPassword: String): Result<Unit>
}

