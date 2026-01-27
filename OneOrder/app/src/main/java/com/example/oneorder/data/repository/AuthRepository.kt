package com.example.oneorder.data.repository

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import javax.inject.Inject

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Boolean>
    suspend fun register(email: String, password: String, fullName: String): Result<Boolean>
}

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Boolean> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(email: String, password: String, fullName: String): Result<Boolean> {
        Log.d("AuthRepository", "Starting registration for email: $email")
        return try {
            Log.d("AuthRepository", "Supabase client initialized: ${supabase.supabaseUrl}")
            Log.d("AuthRepository", "Calling signUpWith...")
            val user = supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                   put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                }
            }
            Log.d("AuthRepository", "SignUp successful, user: ${user?.id}")
            
            // Sign out immediately to prevent auto-login
            // User must confirm email before logging in
            supabase.auth.signOut()
            Log.d("AuthRepository", "Signed out after registration")
            
            Result.success(user != null)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
