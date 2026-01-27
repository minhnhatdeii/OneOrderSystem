package com.example.oneorder_sm.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.domain.repository.AuthRepository
import com.example.oneorder_sm.domain.repository.TenantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val fullName: String = "",
    val restaurantName: String = "",
    val restaurantAddress: String = "",
    val restaurantPhone: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    val isValid: Boolean
        get() = email.isNotBlank() &&
                password.length >= 6 &&
                password == confirmPassword &&
                fullName.isNotBlank() &&
                restaurantName.isNotBlank()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tenantRepository: TenantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun updateEmail(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun updateConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, error = null) }
    }

    fun updateFullName(value: String) {
        _uiState.update { it.copy(fullName = value, error = null) }
    }

    fun updateRestaurantName(value: String) {
        _uiState.update { it.copy(restaurantName = value, error = null) }
    }

    fun updateRestaurantAddress(value: String) {
        _uiState.update { it.copy(restaurantAddress = value, error = null) }
    }

    fun updateRestaurantPhone(value: String) {
        _uiState.update { it.copy(restaurantPhone = value, error = null) }
    }

    fun register() {
        val state = _uiState.value
        
        android.util.Log.d("RegisterViewModel", "=== STARTING REGISTRATION ===")
        android.util.Log.d("RegisterViewModel", "Email: ${state.email}")
        android.util.Log.d("RegisterViewModel", "Full Name: ${state.fullName}")
        android.util.Log.d("RegisterViewModel", "Restaurant Name: ${state.restaurantName}")
        
        if (!state.isValid) {
            android.util.Log.e("RegisterViewModel", "Validation failed")
            _uiState.update { it.copy(error = "Vui lòng điền đầy đủ thông tin") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Register user account with metadata
                // The is_restaurant_owner and restaurant_name are saved in user metadata
                // Tenant will be created automatically on first login (see AuthViewModel)
                android.util.Log.d("RegisterViewModel", "Registering user account...")
                android.util.Log.d("RegisterViewModel", "Email: ${state.email}")
                android.util.Log.d("RegisterViewModel", "Restaurant Name: ${state.restaurantName}")
                
                val registerResult = authRepository.register(
                    email = state.email,
                    password = state.password,
                    fullName = state.fullName,
                    restaurantName = state.restaurantName
                )

                if (registerResult.isFailure) {
                    val error = registerResult.exceptionOrNull()?.message ?: "Đăng ký thất bại"
                    android.util.Log.e("RegisterViewModel", "Registration FAILED: $error")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error
                        )
                    }
                    return@launch
                }
                
                android.util.Log.d("RegisterViewModel", "Registration SUCCESS!")
                android.util.Log.d("RegisterViewModel", "User metadata saved with is_restaurant_owner=true")
                android.util.Log.d("RegisterViewModel", "Tenant will be created on first login")
                
                // Success! User account created with restaurant owner metadata
                // Tenant will be created automatically when user logs in for the first time
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        error = null
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("RegisterViewModel", "UNEXPECTED ERROR during registration", e)
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Có lỗi xảy ra")
                }
            }
        }
    }
}
