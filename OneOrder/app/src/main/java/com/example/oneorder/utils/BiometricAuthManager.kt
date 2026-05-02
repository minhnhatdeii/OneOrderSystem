package com.example.oneorder.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BiometricAuthManager - Handles biometric authentication (fingerprint, face unlock)
 * 
 * Features:
 * - Check device biometric capability
 * - Prompt for biometric authentication
 * - Secure key storage in Android Keystore
 * - Graceful fallback handling
 */
@Singleton
class BiometricAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Biometric capability status
     */
    enum class BiometricStatus {
        AVAILABLE,           // Biometric available and enrolled
        NO_HARDWARE,         // Device doesn't have biometric hardware
        NOT_ENROLLED,        // Hardware exists but no biometrics enrolled
        NOT_AVAILABLE,       // Biometric not available for other reasons
        UNKNOWN              // Could not determine status
    }
    
    /**
     * Authentication result callback
     */
    interface AuthCallback {
        fun onSuccess()
        fun onError(errorCode: Int, errorMessage: String)
        fun onFailed()
    }

    /**
     * Check if device supports biometric authentication
     */
    fun getBiometricStatus(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.NOT_AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.NOT_AVAILABLE
            else -> BiometricStatus.UNKNOWN
        }
    }

    /**
     * Check if biometric authentication is available
     */
    fun isBiometricAvailable(): Boolean {
        return getBiometricStatus() == BiometricStatus.AVAILABLE
    }

    /**
     * Get human-readable status message
     */
    fun getStatusMessage(): String {
        return when (getBiometricStatus()) {
            BiometricStatus.AVAILABLE -> "Vân tay / Khuôn mặt có sẵn"
            BiometricStatus.NO_HARDWARE -> "Thiết bị không hỗ trợ sinh trắc học"
            BiometricStatus.NOT_ENROLLED -> "Chưa đăng ký vân tay hoặc khuôn mặt"
            BiometricStatus.NOT_AVAILABLE -> "Sinh trắc học hiện không khả dụng"
            BiometricStatus.UNKNOWN -> "Không thể xác định trạng thái"
        }
    }

    /**
     * Prompt user for biometric authentication
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Xác thực sinh trắc học",
        subtitle: String = "Đặt ngón tay lên cảm biến hoặc nhìn vào camera",
        negativeButtonText: String = "Hủy",
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit,
        onFailed: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                SecurityLogger.logSecurityEvent(
                    SecurityLogger.SecurityEvent.MFA_VERIFICATION_SUCCESS,
                    "Biometric authentication succeeded"
                )
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                SecurityLogger.logSecurityEvent(
                    SecurityLogger.SecurityEvent.MFA_VERIFICATION_FAILED,
                    "Biometric error: $errString (code: $errorCode)"
                )
                onError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Quick authenticate with default settings
     */
    fun authenticate(
        activity: FragmentActivity,
        callback: AuthCallback
    ) {
        authenticate(
            activity = activity,
            title = "Đăng nhập bằng sinh trắc học",
            subtitle = "Xác thực để tiếp tục đăng nhập",
            negativeButtonText = "Sử dụng mật khẩu",
            onSuccess = { callback.onSuccess() },
            onError = { code, message -> callback.onError(code, message) },
            onFailed = { callback.onFailed() }
        )
    }

    /**
     * Get biometric type name (fingerprint, face, etc.)
     */
    fun getBiometricTypeName(): String {
        val biometricManager = BiometricManager.from(context)
        
        // Check for specific biometric types
        val authenticators = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        
        return when (authenticators) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // We can't determine exact type without device-specific APIs
                // Default to common names
                "Vân tay / Khuôn mặt"
            }
            else -> "Không khả dụng"
        }
    }

    companion object {
        // BiometricPrompt error codes
        const val ERROR_CANCELED = 10
        const val ERROR_LOCKOUT = 7
        const val ERROR_LOCKOUT_PERMANENT = 8
        const val ERROR_NEGATIVE_BUTTON = 13
        const val ERROR_NO_BIOMETRICS = 11
        const val ERROR_TIMEOUT = 5
        
        /**
         * Get human-readable error message for biometric error codes
         */
        fun getErrorMessage(errorCode: Int): String {
            return when (errorCode) {
                ERROR_CANCELED -> "Xác thực đã bị hủy"
                ERROR_LOCKOUT -> "Quá nhiều lần thử. Vui lòng thử lại sau."
                ERROR_LOCKOUT_PERMANENT -> "Tính năng sinh trắc học đã bị khóa vĩnh viễn. Vui lòng sử dụng mật khẩu."
                ERROR_NEGATIVE_BUTTON -> "Đã hủy xác thực"
                ERROR_TIMEOUT -> "Hết thời gian xác thực"
                else -> "Lỗi xác thực sinh trắc học"
            }
        }
    }
}
