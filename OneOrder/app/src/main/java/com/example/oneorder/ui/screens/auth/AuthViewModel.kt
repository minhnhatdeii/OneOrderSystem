package com.example.oneorder.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(AuthState())
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow(AuthState())
    val registerState: StateFlow<AuthState> = _registerState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = AuthState(isLoading = true)
            Log.d("AuthViewModel", "=== LOGIN ATTEMPT ===")
            Log.d("AuthViewModel", "Email: $email")
            
            try {
                val result = authRepository.login(email, password)
                
                if (result.isSuccess) {
                    Log.d("AuthViewModel", "Login successful!")
                    _loginState.value = AuthState(isSuccess = true)
                } else {
                    val exception = result.exceptionOrNull()
                    val errorMessage = exception?.message ?: "Unknown Error"
                    
                    // Log full error details
                    Log.e("AuthViewModel", "=== LOGIN FAILED ===")
                    Log.e("AuthViewModel", "Error Message: $errorMessage")
                    Log.e("AuthViewModel", "Exception Type: ${exception?.javaClass?.simpleName}")
                    Log.e("AuthViewModel", "Stack Trace:", exception)
                    
                    // Log the complete error for debugging
                    Log.e("AuthViewModel", "Full Error Details: ${exception?.toString()}")
                    
                    // Check if error is due to unconfirmed email
                    val message = if (errorMessage.contains("Email not confirmed", ignoreCase = true)) {
                        "Vui lòng xác nhận email trước khi đăng nhập"
                    } else {
                        errorMessage
                    }
                    
                    _loginState.value = AuthState(error = message)
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "=== UNEXPECTED EXCEPTION ===")
                Log.e("AuthViewModel", "Exception during login:", e)
                _loginState.value = AuthState(error = "Login failed: ${e.message}")
            }
        }
    }

    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            _registerState.value = AuthState(isLoading = true)
            Log.d("AuthViewModel", "Starting registration for email: $email")
            val result = authRepository.register(email, password, fullName)
            if (result.isSuccess) {
                _registerState.value = AuthState(
                    isSuccess = true,
                    successMessage = "Đăng ký thành công! Vui lòng kiểm tra email để xác nhận tài khoản."
                )
                Log.d("AuthViewModel", "Registration successful")
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Registration Failed"
                Log.e("AuthViewModel", "Registration failed: $errorMessage", result.exceptionOrNull())
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
}
