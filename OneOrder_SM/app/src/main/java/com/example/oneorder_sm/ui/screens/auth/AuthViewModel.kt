package com.example.oneorder_sm.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.local.LoginPreferences
import com.example.oneorder_sm.domain.usecase.LoginUseCase
import com.example.oneorder_sm.domain.repository.TenantRepository
import com.example.oneorder_sm.domain.repository.AuthRepository
import io.github.jan.supabase.gotrue.Auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject

data class LoginState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val role: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val getCurrentUserUseCase: com.example.oneorder_sm.domain.usecase.GetCurrentUserUseCase,
    private val authRepository: AuthRepository,
    private val tenantRepository: TenantRepository,
    private val auth: Auth,
    private val userPreferencesManager: com.example.oneorder_sm.data.local.UserPreferencesManager
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _loginState.value = LoginState(isLoading = true)
            val result = loginUseCase(email, password)
            if (result.isSuccess) {
                // Save credentials if remember me is checked
                if (rememberMe) {
                    userPreferencesManager.saveLoginPreferences(
                        rememberMe = true,
                        email = email,
                        password = password
                    )
                }

                // Fetch the current user to get the role
                try {
                    val user = getCurrentUserUseCase().firstOrNull()
                    android.util.Log.d("AuthViewModel", "=== LOGIN SUCCESS ===")
                    android.util.Log.d("AuthViewModel", "User role from profile: ${user?.role}")
                    android.util.Log.d("AuthViewModel", "User fullName: ${user?.fullName}")
                    android.util.Log.d("AuthViewModel", "User tenantId: ${user?.tenantId}")
                    
                    // Check if this is a restaurant owner who needs setup
                    val currentUser = auth.currentUserOrNull()
                    android.util.Log.d("AuthViewModel", "Supabase user ID: ${currentUser?.id}")
                    android.util.Log.d("AuthViewModel", "User metadata: ${currentUser?.userMetadata}")
                    
                    val isRestaurantOwnerElement = currentUser?.userMetadata?.get("is_restaurant_owner")
                    val isRestaurantOwner = (isRestaurantOwnerElement as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull ?: false
                    
                    android.util.Log.d("AuthViewModel", ">>> Is restaurant owner element: $isRestaurantOwnerElement")
                    android.util.Log.d("AuthViewModel", ">>> Current role: ${user?.role}")
                    android.util.Log.d("AuthViewModel", ">>> Has tenant: ${user?.tenantId != null}")
                    
                    // If restaurant owner but role is still customer, need to setup
                    if (isRestaurantOwner) {
                        android.util.Log.d("AuthViewModel", "✓ User IS a restaurant owner")
                        
                        if (user?.tenantId == null) {
                            android.util.Log.d("AuthViewModel", "✓ No tenant yet - NEEDS SETUP")
                            android.util.Log.d("AuthViewModel", ">>> Setting up restaurant owner on first login...")
                            
                            // This should have been saved during registration
                            val restaurantNameElement = currentUser?.userMetadata?.get("restaurant_name")
                            val restaurantName = (restaurantNameElement as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull 
                                ?: user?.fullName ?: "My Restaurant"
                            
                            android.util.Log.d("AuthViewModel", "Restaurant name from metadata: $restaurantName")
                            
                            // Create tenant and set manager role
                            val tenantResult = tenantRepository.createRestaurant(
                                restaurantName = restaurantName,
                                address = null,
                                phone = null,
                                email = email
                            )
                            
                            if (tenantResult.isSuccess) {
                                android.util.Log.d("AuthViewModel", "✓✓✓ Restaurant setup complete!")
                                // Refresh user profile to get updated role
                                val updatedUser = getCurrentUserUseCase().firstOrNull()
                                android.util.Log.d("AuthViewModel", "Updated role: ${updatedUser?.role}")
                                _loginState.value = LoginState(isSuccess = true, role = updatedUser?.role ?: "manager")
                            } else {
                                android.util.Log.e("AuthViewModel", "✗✗✗ Failed to setup restaurant: ${tenantResult.exceptionOrNull()?.message}")
                                android.util.Log.e("AuthViewModel", "Exception: ", tenantResult.exceptionOrNull())
                                _loginState.value = LoginState(isSuccess = true, role = user?.role)
                            }
                        } else {
                            android.util.Log.d("AuthViewModel", "✗ Tenant already exists (${user?.tenantId}), skip setup")
                            _loginState.value = LoginState(isSuccess = true, role = user?.role)
                        }
                    } else {
                        android.util.Log.d("AuthViewModel", "✗ NOT a restaurant owner, skip setup")
                        _loginState.value = LoginState(isSuccess = true, role = user?.role)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "✗✗✗ Error during login", e)
                    _loginState.value = LoginState(isSuccess = true, role = null)
                }
            } else {
                _loginState.value = LoginState(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun getSavedCredentials(): Flow<LoginPreferences> {
        return userPreferencesManager.loginPreferences
    }

    suspend fun clearSavedCredentials() {
        userPreferencesManager.clearSavedCredentials()
    }
    
    // ─── Password Reset ───
    
    private val _resetPasswordState = MutableStateFlow(ResetPasswordState())
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()
    
    private val _changePasswordState = MutableStateFlow(ChangePasswordState())
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()
    
    /**
     * Send password reset email
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState(isLoading = true)
            
            try {
                val result = authRepository.resetPassword(email)
                if (result.isSuccess) {
                    android.util.Log.d("AuthViewModel", "Password reset email sent to: $email")
                    _resetPasswordState.value = ResetPasswordState(
                        isLoading = false,
                        isSuccess = true,
                        message = "Email đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư."
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Không thể gửi email đặt lại mật khẩu"
                    _resetPasswordState.value = ResetPasswordState(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _resetPasswordState.value = ResetPasswordState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }
    
    /**
     * Reset password with new password (after deep link redirect establishes session)
     */
    fun resetPasswordWithNewPassword(newPassword: String, token: String? = null) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState(isLoading = true)
            
            try {
                val result = if (!token.isNullOrBlank()) {
                    android.util.Log.d("AuthViewModel", "Resetting password using recovery token")
                    authRepository.changePasswordWithToken(token, newPassword)
                } else {
                    android.util.Log.d("AuthViewModel", "Resetting password for logged-in user")
                    authRepository.changePassword(newPassword)
                }
                if (result.isSuccess) {
                    android.util.Log.d("AuthViewModel", "Password reset successful")
                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        isSuccess = true,
                        message = "Đặt lại mật khẩu thành công!"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Không thể đặt lại mật khẩu"
                    android.util.Log.e("AuthViewModel", "Password reset failed: $errorMsg")
                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Password reset exception", e)
                _changePasswordState.value = ChangePasswordState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }
    
    fun resetResetPasswordState() {
        _resetPasswordState.value = ResetPasswordState()
    }
    
    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState()
    }
}

/**
 * Reset Password State (for sending reset email)
 */
data class ResetPasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Change Password State (for setting new password)
 */
data class ChangePasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
