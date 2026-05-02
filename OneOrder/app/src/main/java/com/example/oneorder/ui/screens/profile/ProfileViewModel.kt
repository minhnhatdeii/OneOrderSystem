package com.example.oneorder.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.local.SecurePreferencesManager
import com.example.oneorder.data.local.UserPreferencesManager
import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.data.repository.ProfileRepository
import com.example.oneorder.utils.SecurityLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: com.example.oneorder.data.model.Profile? = null,
    val email: String? = null,
    val isSignedOut: Boolean = false,
    val error: String? = null,
    val passwordChangeSuccess: Boolean = false,
    val isVerifyingPassword: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val supabase: SupabaseClient,
    private val userPreferencesManager: UserPreferencesManager,
    private val securePreferencesManager: SecurePreferencesManager,
    private val followingRepository: FollowingRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = userPreferencesManager.themeMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "SYSTEM")

    val appLanguage: StateFlow<String> = userPreferencesManager.appLanguage
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), "vi")

    // Expose the repository's shared StateFlow directly — no copy needed.
    // This is updated whenever invalidateCache() is called anywhere in the app.
    val followingCount: StateFlow<Int> = followingRepository.getFollowingCountFlow()

    private val _uiState = MutableStateFlow(ProfileState())
    val uiState: StateFlow<ProfileState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileState(isLoading = true)
            
            try {
                val result = profileRepository.getProfile()
                val email = supabase.auth.currentUserOrNull()?.email
                
                if (result.isSuccess) {
                    val profile = result.getOrNull()
                    
                    _uiState.value = ProfileState(
                        profile = profile,
                        email = email,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    _uiState.value = ProfileState(
                        isLoading = false,
                        error = error?.message ?: "Unknown error loading profile"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = ProfileState(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Clear saved credentials
            securePreferencesManager.clearCredentials()
            
            profileRepository.signOut()
            
            // Log sign out
            SecurityLogger.logSecurityEvent(
                SecurityLogger.SecurityEvent.SESSION_EXPIRED,
                "User signed out"
            )
            
            _uiState.value = ProfileState(isSignedOut = true)
        }
    }
    
    fun updateProfile(fullName: String, phoneNumber: String) {
        viewModelScope.launch {
            val result = profileRepository.updateProfile(fullName, phoneNumber)
            
            if (result.isSuccess) {
                loadProfile()
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update profile: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
    
    /**
     * Change password with current password verification
     * This is a secure implementation that verifies the current password
     * before allowing the change
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                passwordChangeSuccess = false,
                isVerifyingPassword = true
            )
            
            val email = supabase.auth.currentUserOrNull()?.email
            if (email == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isVerifyingPassword = false,
                    error = "Không tìm thấy thông tin người dùng"
                )
                return@launch
            }
            
            try {
                // Step 1: Verify current password by attempting to sign in
                try {
                    supabase.auth.signInWith(Email) {
                        this.email = email
                        this.password = currentPassword
                    }
                } catch (e: Exception) {
                    // If sign in fails, current password is incorrect
                    SecurityLogger.logPasswordChangeFailed(email, "Current password verification failed")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerifyingPassword = false,
                        error = "Mật khẩu hiện tại không đúng"
                    )
                    return@launch
                }
                
                // Step 2: Update to new password
                try {
                    supabase.auth.updateUser {
                        password = newPassword
                    }
                    
                    // Step 3: Sign out all other sessions for security
                    // In production, you would call supabase.auth.signOutAll() here
                    
                    SecurityLogger.logPasswordChange(email)
                    
                    // Clear local saved credentials since password changed
                    securePreferencesManager.clearCredentials()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerifyingPassword = false,
                        passwordChangeSuccess = true,
                        error = null
                    )
                } catch (e: Exception) {
                    SecurityLogger.logPasswordChangeFailed(email, e.message ?: "Unknown error")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isVerifyingPassword = false,
                        error = "Không thể đổi mật khẩu: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                SecurityLogger.logPasswordChangeFailed(email, e.message ?: "Unexpected error")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isVerifyingPassword = false,
                    error = "Lỗi: ${e.message}"
                )
            }
        }
    }

    fun uploadAvatar(imageBytes: ByteArray, extension: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = profileRepository.uploadAvatar(imageBytes, extension)
            if (result.isSuccess) {
                loadProfile()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to upload avatar: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun setAppLanguage(languageCode: String) {
        viewModelScope.launch {
            userPreferencesManager.saveAppLanguage(languageCode)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            userPreferencesManager.saveThemeMode(mode)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearPasswordChangeSuccess() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false)
    }

    /**
     * Check if biometric is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return securePreferencesManager.isBiometricEnabled()
    }

    /**
     * Enable or disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePreferencesManager.setBiometricEnabled(enabled)
        if (enabled) {
            SecurityLogger.logBiometricEnabled()
        } else {
            SecurityLogger.logBiometricDisabled()
        }
    }
}
