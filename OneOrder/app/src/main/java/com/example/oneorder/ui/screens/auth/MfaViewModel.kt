package com.example.oneorder.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.local.SecurePreferencesManager
import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.utils.SecurityLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MFA (Multi-Factor Authentication) ViewModel
 * Handles OTP verification for enhanced security
 */
@HiltViewModel
class MfaViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val securePreferencesManager: SecurePreferencesManager
) : ViewModel() {

    private val _mfaState = MutableStateFlow(MfaState())
    val mfaState: StateFlow<MfaState> = _mfaState.asStateFlow()

    private val _setupState = MutableStateFlow(MfaSetupState())
    val setupState: StateFlow<MfaSetupState> = _setupState.asStateFlow()

    private val _changePasswordState = MutableStateFlow(ChangePasswordState())
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    private val _resetPasswordState = MutableStateFlow(ResetPasswordState())
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()

    /**
     * Send OTP code to user's email
     * Uses Supabase's built-in OTP functionality
     */
    fun sendOtp(email: String) {
        viewModelScope.launch {
            _mfaState.value = MfaState(isLoading = true)
            
            try {
                val result = authRepository.sendOtp(email)
                if (result.isSuccess) {
                    _mfaState.value = MfaState(
                        isLoading = false,
                        otpSent = true,
                        message = "Mã xác thực đã được gửi đến email của bạn"
                    )
                    startResendCountdown()
                } else {
                    _mfaState.value = MfaState(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Không thể gửi mã xác thực"
                    )
                }
            } catch (e: Exception) {
                SecurityLogger.logSecurityEvent(
                    SecurityLogger.SecurityEvent.MFA_VERIFICATION_FAILED,
                    "Error sending OTP: ${e.message}"
                )
                _mfaState.value = MfaState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Verify OTP code
     */
    fun verifyOtp(email: String, code: String) {
        viewModelScope.launch {
            _mfaState.value = _mfaState.value.copy(isLoading = true, error = null)
            
            try {
                val result = authRepository.verifyOtp(email, code)
                if (result.isSuccess) {
                    SecurityLogger.logMfaVerificationSuccess(email)
                    _mfaState.value = MfaState(
                        isLoading = false,
                        isVerified = true,
                        message = "Xác thực thành công!"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Mã xác thực không hợp lệ"
                    SecurityLogger.logMfaVerificationFailed(email)
                    _mfaState.value = MfaState(
                        isLoading = false,
                        error = errorMsg,
                        attemptsRemaining = (_mfaState.value.attemptsRemaining - 1).coerceAtLeast(0)
                    )
                    
                    // Lock after too many failed attempts
                    if (_mfaState.value.attemptsRemaining <= 1) {
                        _mfaState.value = _mfaState.value.copy(
                            isLocked = true,
                            lockedUntil = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes
                        )
                        SecurityLogger.logAccountLocked(email, "5 minutes (MFA verification)")
                    }
                }
            } catch (e: Exception) {
                SecurityLogger.logSecurityEvent(
                    SecurityLogger.SecurityEvent.MFA_VERIFICATION_FAILED,
                    "Error verifying OTP: ${e.message}"
                )
                _mfaState.value = _mfaState.value.copy(
                    isLoading = false,
                    error = "Lỗi xác thực. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Verify using backup code
     */
    fun verifyBackupCode(email: String, backupCode: String) {
        viewModelScope.launch {
            _mfaState.value = _mfaState.value.copy(isLoading = true, error = null)
            
            try {
                val isValid = authRepository.verifyBackupCode(email, backupCode)
                if (isValid) {
                    SecurityLogger.logMfaVerificationSuccess(email)
                    _mfaState.value = MfaState(
                        isLoading = false,
                        isVerified = true,
                        message = "Xác thực bằng mã dự phòng thành công!"
                    )
                } else {
                    SecurityLogger.logMfaVerificationFailed(email)
                    _mfaState.value = _mfaState.value.copy(
                        isLoading = false,
                        error = "Mã dự phòng không hợp lệ"
                    )
                }
            } catch (e: Exception) {
                _mfaState.value = _mfaState.value.copy(
                    isLoading = false,
                    error = "Lỗi xác thực. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Resend OTP with countdown
     */
    private fun startResendCountdown() {
        viewModelScope.launch {
            _mfaState.value = _mfaState.value.copy(resendCooldown = 60)
            while (_mfaState.value.resendCooldown > 0) {
                delay(1000)
                _mfaState.value = _mfaState.value.copy(
                    resendCooldown = _mfaState.value.resendCooldown - 1
                )
            }
        }
    }

    /**
     * Resend OTP
     */
    fun resendOtp(email: String) {
        if (_mfaState.value.resendCooldown > 0) return
        sendOtp(email)
    }

    /**
     * Enable MFA for the current user
     */
    fun enableMfa() {
        viewModelScope.launch {
            _setupState.value = MfaSetupState(isLoading = true)
            
            try {
                val result = authRepository.enableMfa()
                if (result.isSuccess) {
                    val backupCodes = generateBackupCodes()
                    securePreferencesManager.setMfaEnabled(true)
                    SecurityLogger.logMfaEnabled(authRepository.getCurrentUser()?.email ?: "unknown")
                    
                    _setupState.value = MfaSetupState(
                        isLoading = false,
                        isEnabled = true,
                        backupCodes = backupCodes,
                        message = "Xác thực hai yếu tố đã được bật thành công!"
                    )
                } else {
                    _setupState.value = MfaSetupState(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Không thể bật MFA"
                    )
                }
            } catch (e: Exception) {
                _setupState.value = MfaSetupState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Disable MFA for the current user
     */
    fun disableMfa() {
        viewModelScope.launch {
            _setupState.value = MfaSetupState(isLoading = true)
            
            try {
                val result = authRepository.disableMfa()
                if (result.isSuccess) {
                    securePreferencesManager.setMfaEnabled(false)
                    SecurityLogger.logMfaDisabled(authRepository.getCurrentUser()?.email ?: "unknown")
                    
                    _setupState.value = MfaSetupState(
                        isLoading = false,
                        isDisabled = true,
                        message = "Xác thực hai yếu tố đã được tắt"
                    )
                } else {
                    _setupState.value = MfaSetupState(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Không thể tắt MFA"
                    )
                }
            } catch (e: Exception) {
                _setupState.value = MfaSetupState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Generate backup codes for MFA recovery
     */
    private fun generateBackupCodes(): List<String> {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8).map {
            (1..8).map { chars.random() }.joinToString("")
        }
    }

    /**
     * Check if MFA is enabled for current user
     */
    fun isMfaEnabled(): Boolean {
        return securePreferencesManager.isMfaEnabled()
    }

    /**
     * Reset MFA state
     */
    fun resetState() {
        _mfaState.value = MfaState()
        _setupState.value = MfaSetupState()
    }

    /**
     * Clear error
     */
    fun clearError() {
        _mfaState.value = _mfaState.value.copy(error = null)
    }

    /**
     * Change password for current user
     * Requires MFA verification when changing password from security settings
     */
    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState(isLoading = true)

            try {
                val result = authRepository.changePassword(currentPassword, newPassword)
                if (result.isSuccess) {
                    // Clear device registration to require MFA on next login from new device
                    securePreferencesManager.clearDeviceRegistration()
                    SecurityLogger.logPasswordChanged(authRepository.getCurrentUser()?.email ?: "unknown")

                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        isSuccess = true,
                        message = "Đổi mật khẩu thành công!"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Không thể đổi mật khẩu"
                    SecurityLogger.logPasswordChangeFailed(authRepository.getCurrentUser()?.email ?: "unknown", errorMsg)
                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                SecurityLogger.logSecurityEvent(
                    SecurityLogger.SecurityEvent.PASSWORD_CHANGE_FAILED,
                    "Error changing password: ${e.message}"
                )
                _changePasswordState.value = ChangePasswordState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Send password reset email
     */
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _resetPasswordState.value = ResetPasswordState(isLoading = true)

            try {
                val result = authRepository.resetPassword(email)
                if (result.isSuccess) {
                    SecurityLogger.logSecurityEvent(
                        SecurityLogger.SecurityEvent.PASSWORD_RESET_REQUESTED,
                        "Password reset requested for: ${maskEmail(email)}"
                    )
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
     * Reset password with token from email deep link.
     * Exchanges the one-time recovery token for a session before updating the password.
     */
    fun resetPasswordWithToken(newPassword: String, token: String) {
        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordState(isLoading = true)

            try {
                val result = authRepository.changePasswordWithToken(token, newPassword)
                if (result.isSuccess) {
                    SecurityLogger.logPasswordChanged("password_reset")
                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        isSuccess = true,
                        message = "Đặt lại mật khẩu thành công!"
                    )
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Không thể đặt lại mật khẩu"
                    _changePasswordState.value = ChangePasswordState(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                _changePasswordState.value = ChangePasswordState(
                    isLoading = false,
                    error = "Lỗi kết nối. Vui lòng thử lại."
                )
            }
        }
    }

    /**
     * Reset change password state
     */
    fun resetChangePasswordState() {
        _changePasswordState.value = ChangePasswordState()
    }

    /**
     * Reset reset password state
     */
    fun resetResetPasswordState() {
        _resetPasswordState.value = ResetPasswordState()
    }

    private fun maskEmail(email: String): String {
        return if (email.contains("@")) {
            val parts = email.split("@")
            val localPart = parts[0]
            val maskedLocal = if (localPart.length <= 2) {
                localPart.first() + "***"
            } else {
                localPart.first() + "***" + localPart.last()
            }
            "$maskedLocal@${parts.getOrNull(1)}"
        } else {
            "***masked***"
        }
    }
}

/**
 * MFA Verification State
 */
data class MfaState(
    val isLoading: Boolean = false,
    val isVerified: Boolean = false,
    val otpSent: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val attemptsRemaining: Int = 3,
    val resendCooldown: Int = 0,
    val isLocked: Boolean = false,
    val lockedUntil: Long = 0L
)

/**
 * MFA Setup State
 */
data class MfaSetupState(
    val isLoading: Boolean = false,
    val isEnabled: Boolean = false,
    val isDisabled: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val backupCodes: List<String> = emptyList()
)

/**
 * Change Password State
 */
data class ChangePasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

/**
 * Reset Password State
 */
data class ResetPasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
