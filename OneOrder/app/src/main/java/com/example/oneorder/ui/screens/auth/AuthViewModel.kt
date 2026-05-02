package com.example.oneorder.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.local.SecureLoginCredentials
import com.example.oneorder.data.local.SecurePreferencesManager
import com.example.oneorder.data.local.UserPreferencesManager
import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.utils.SecurityLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val requiresMfa: Boolean = false,
    val newDeviceDetected: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val securePreferencesManager: SecurePreferencesManager
) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthState())
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthState())
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _loginState.value = AuthState(isLoading = true)

            try {
                val result = authRepository.login(email, password)

                if (result.isSuccess) {
                    // Update last login time for session tracking
                    securePreferencesManager.updateLastLoginTime()

                    // Save credentials securely if remember me is checked
                    if (rememberMe) {
                        securePreferencesManager.saveLoginCredentials(
                            rememberMe = true,
                            email = email,
                            password = password
                        )
                    }

                    // Log successful login (without sensitive data)
                    SecurityLogger.logLoginSuccess(email)

                    // Check if MFA is enabled and device is registered
                    val mfaEnabled = securePreferencesManager.isMfaEnabled()
                    val deviceRegistered = securePreferencesManager.isDeviceRegistered(email)

                    if (mfaEnabled && !deviceRegistered) {
                        // New device detected - require MFA verification
                        _loginState.value = AuthState(
                            requiresMfa = true,
                            newDeviceDetected = true
                        )
                    } else {
                        // Same device or MFA disabled - login directly
                        _loginState.value = AuthState(isSuccess = true)
                    }
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Unknown Error"

                    // Log failed login attempt
                    SecurityLogger.logLoginFailed(email, errorMessage)

                    // Check if error is due to unconfirmed email
                    val message = if (errorMessage.contains("Email not confirmed", ignoreCase = true)) {
                        "Vui lòng xác nhận email trước khi đăng nhập"
                    } else {
                        errorMessage
                    }

                    _loginState.value = AuthState(error = message)
                }
            } catch (e: Exception) {
                // Log unexpected exception
                SecurityLogger.logLoginFailed(email, e.message ?: "Unexpected error")
                _loginState.value = AuthState(error = "Đăng nhập thất bại: ${e.message}")
            }
        }
    }

    /**
     * Confirm MFA verification and complete login
     * Called after user successfully verifies MFA code
     */
    fun confirmMfaAndLogin(email: String) {
        // Register this device for future logins
        securePreferencesManager.registerDevice(email)
        _loginState.value = AuthState(isSuccess = true)
    }

    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _registerState.value = AuthState(isLoading = true)
            
            // Log registration attempt (without password)
            SecurityLogger.logRegistrationAttempt(email)
            
            val result = authRepository.register(email, password, fullName)
            if (result.isSuccess) {
                // Log successful registration
                SecurityLogger.logRegistrationSuccess(email)
                
                _registerState.value = AuthState(
                    isSuccess = true,
                    successMessage = "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản."
                )
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Registration Failed"
                
                // Log failed registration
                SecurityLogger.logRegistrationFailed(email, errorMessage)
                
                _registerState.value = AuthState(error = errorMessage)
            }
        }
    }
    
    fun resetState() {
        _loginState.value = AuthState()
        _registerState.value = AuthState()
    }

    fun setLoginMessage(message: String) {
        _loginState.value = AuthState(successMessage = message)
    }

    /**
     * Logout and clear device registration
     */
    fun logout() {
        securePreferencesManager.clearDeviceRegistration()
    }

    fun getSavedCredentials(): Flow<SecureLoginCredentials> {
        return userPreferencesManager.loginPreferences
    }

    suspend fun clearSavedCredentials() {
        userPreferencesManager.clearSavedCredentials()
    }
}
