package com.example.oneorder.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.Profile
import com.example.oneorder.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: Profile? = null,
    val email: String? = null, // User's email from auth
    val isSignedOut: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabase: io.github.jan.supabase.SupabaseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            android.util.Log.d("ProfileViewModel", "=== LOADING PROFILE ===")
            _uiState.value = ProfileState(isLoading = true)
            
            try {
                val result = profileRepository.getProfile()
                val email = supabase.auth.currentUserOrNull()?.email
                
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    android.util.Log.d("ProfileViewModel", "✅ Successfully loaded profile")
                    android.util.Log.d("ProfileViewModel", "Profile ID: ${profile?.id}")
                    android.util.Log.d("ProfileViewModel", "Full Name: ${profile?.fullName}")
                    android.util.Log.d("ProfileViewModel", "Email: $email")
                    android.util.Log.d("ProfileViewModel", "Role: ${profile?.role}")
                    
                    _uiState.value = ProfileState(
                        profile = profile,
                        email = email,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    android.util.Log.e("ProfileViewModel", "=== ERROR LOADING PROFILE ===")
                    android.util.Log.e("ProfileViewModel", "Error type: ${error?.javaClass?.simpleName}")
                    android.util.Log.e("ProfileViewModel", "Error message: ${error?.message}")
                    android.util.Log.e("ProfileViewModel", "Full error:", error)
                    
                    _uiState.value = ProfileState(
                        isLoading = false,
                        error = error?.message ?: "Unknown error loading profile"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "=== UNEXPECTED ERROR ===", e)
                _uiState.value = ProfileState(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            profileRepository.signOut()
            _uiState.value = ProfileState(isSignedOut = true)
        }
    }
    
    fun updateProfile(fullName: String, phoneNumber: String) {
        viewModelScope.launch {
            android.util.Log.d("ProfileViewModel", "Updating profile: name=$fullName, phone=$phoneNumber")
            val result = profileRepository.updateProfile(fullName, phoneNumber)
            
            if (result.isSuccess) {
                android.util.Log.d("ProfileViewModel", "Profile updated successfully")
                // Reload profile to get updated data
                loadProfile()
            } else {
                android.util.Log.e("ProfileViewModel", "Failed to update profile: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    fun changePassword(newPassword: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProfileViewModel", "Changing password...")
                supabase.auth.updateUser {
                    password = newPassword
                }
                android.util.Log.d("ProfileViewModel", "✅ Password changed successfully")
                _uiState.value = _uiState.value.copy(
                    error = null // Clear any errors
                )
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Failed to change password: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to change password: ${e.message}"
                )
            }
        }
    }
}
