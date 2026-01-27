package com.example.oneorder.data.repository

import com.example.oneorder.data.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject

interface ProfileRepository {
    suspend fun getProfile(): Result<Profile>
    suspend fun updateProfile(fullName: String, phoneNumber: String): Result<Unit>
    suspend fun signOut()
}

class ProfileRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ProfileRepository {

    override suspend fun getProfile(): Result<Profile> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            val profile = supabase.postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", currentUser.id)
                    }
                }.decodeSingle<Profile>()
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateProfile(fullName: String, phoneNumber: String): Result<Unit> {
        return try {
            val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
            supabase.postgrest.from("profiles").update({
                set("full_name", fullName)
                set("phone_number", phoneNumber)
            }) {
                filter {
                    eq("id", currentUser.id)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }
}
