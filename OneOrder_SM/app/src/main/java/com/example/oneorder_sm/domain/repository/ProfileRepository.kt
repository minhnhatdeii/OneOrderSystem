package com.example.oneorder_sm.domain.repository

import com.example.oneorder_sm.domain.model.Profile

interface ProfileRepository {
    /**
     * Get current user's profile and email
     * Returns Pair of Profile and email (from auth)
     */
    suspend fun getCurrentProfile(): Result<Pair<Profile, String?>>
    
    /**
     * Update user profile (name and phone)
     */
    suspend fun updateProfile(
        fullName: String,
        phoneNumber: String?
    ): Result<Unit>
    
    /**
     * Change user password
     */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>
    
    /**
     * Upload user avatar
     */
    suspend fun uploadAvatar(
        imageBytes: ByteArray,
        extension: String
    ): Result<String>
    
    /**
     * Sign out user
     */
    suspend fun signOut()
}
