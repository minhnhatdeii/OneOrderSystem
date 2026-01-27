package com.example.oneorder_sm.data.repository

import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) : AuthRepository {

    private val _currentUser = MutableStateFlow<Profile?>(null)
    override val currentUser: Flow<Profile?> = _currentUser.asStateFlow()

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val profile = getCurrentProfile()
            if (profile != null) {
                // Check if account is active
                if (!profile.isActive) {
                    auth.signOut()
                    return Result.failure(Exception("Account has been deactivated"))
                }
                
                if (profile.role == "staff" || profile.role == "manager") {
                    _currentUser.value = profile
                    Result.success(Unit)
                } else {
                    auth.signOut()
                    Result.failure(Exception("Unauthorized role: ${profile.role}"))
                }
            } else {
                 // Profile not found, maybe sync issue or trigger failed?
                 // For now, let's assume it failed.
                 auth.signOut()
                 Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        auth.signOut()
        _currentUser.value = null
    }

    override suspend fun getCurrentProfile(): Profile? {
        val user = auth.currentUserOrNull() ?: return null
        return try {
            val profile = postgrest.from("profiles")
                .select(columns = Columns.ALL) {
                    filter {
                        eq("id", user.id)
                    }
                }
                .decodeSingleOrNull<Profile>()
            profile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override suspend fun refreshSession(): Result<Unit> {
         val user = auth.currentUserOrNull()
         if(user != null) {
             val profile = getCurrentProfile()
             _currentUser.value = profile
             return Result.success(Unit)
         }
         return Result.failure(Exception("No session"))
    }
    
    override suspend fun register(
        email: String,
        password: String, 
        fullName: String,
        restaurantName: String?
    ): Result<Unit> {
        return try {
            android.util.Log.d("AuthRepository", "=== REGISTRATION START ===")
            android.util.Log.d("AuthRepository", "Email: $email")
            android.util.Log.d("AuthRepository", "Restaurant: $restaurantName")
            
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                this.data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                    put("is_restaurant_owner", kotlinx.serialization.json.JsonPrimitive(true))
                    restaurantName?.let { 
                        put("restaurant_name", kotlinx.serialization.json.JsonPrimitive(it))
                    }
                }
            }
            
            android.util.Log.d("AuthRepository", "signUpWith completed")
            
            // Check if user is authenticated
            val currentUser = auth.currentUserOrNull()
            if (currentUser != null) {
                android.util.Log.d("AuthRepository", "User IS authenticated after signup")
                android.util.Log.d("AuthRepository", "User ID: ${currentUser.id}")
                android.util.Log.d("AuthRepository", "Email confirmed: ${currentUser.emailConfirmedAt != null}")
            } else {
                android.util.Log.w("AuthRepository", "User NOT authenticated after signup")
                android.util.Log.w("AuthRepository", "Email confirmation is required")
                // This is OK - user will be authenticated after confirming email
                // Return success anyway
            }
            
            // Return success regardless of email confirmation status
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Registration failed", e)
            Result.failure(e)
        }
    }
    
    override suspend fun hasTenant(): Boolean {
        val profile = getCurrentProfile() ?: return false
        return profile.tenantId != null
    }
}

