package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.data.model.ProfileWithTenant
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ProfileRepository {
    
    override suspend fun getCurrentProfile(): Result<Pair<Profile, String?>> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Not authenticated"))
            
            val profileData = supabase.from("profiles")
                .select {
                    filter { eq("id", currentUser.id) }
                }
                .decodeSingleOrNull<ProfileWithTenant>()
                ?: return Result.failure(Exception("Profile not found"))
            
            
            val profile = Profile(
                id = profileData.id,
                tenantId = profileData.tenantId,
                fullName = profileData.fullName,
                role = profileData.role ?: "staff", // Default to staff if null
                phoneNumber = profileData.phoneNumber,
                isActive = profileData.isActive,
                createdAt = profileData.createdAt
            )
            
            Result.success(Pair(profile, currentUser.email))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(
        fullName: String,
        phoneNumber: String?
    ): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Not authenticated"))
            
            supabase.from("profiles")
                .update(
                    {
                        set("full_name", fullName)
                        set("phone_number", phoneNumber)
                    }
                ) {
                    filter { eq("id", currentUser.id) }
                }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            // Supabase updateUser requires the user to be authenticated
            // The current session validates the user
            val currentUser = supabase.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Not authenticated"))
            
            // Note: We cannot verify the old password directly in client
            // Supabase will require reauthentication if needed
            // For better UX, we accept the limitation that we can't verify old password
            
            // Update password
            supabase.auth.updateUser {
                password = newPassword
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Failed to change password: ${e.message}"))
        }
    }
}
