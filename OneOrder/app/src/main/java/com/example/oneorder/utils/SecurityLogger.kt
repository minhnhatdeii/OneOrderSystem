package com.example.oneorder.utils

import android.util.Log
import androidx.annotation.RequiresApi
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SecurityLogger - Secure audit logging for security-related events
 * 
 * This logger tracks:
 * - Successful login attempts
 * - Failed login attempts
 * - Password changes
 * - MFA enable/disable
 * - Biometric enrollment changes
 * - Suspicious activity detection
 * 
 * IMPORTANT: Never log passwords, tokens, or PII (Personally Identifiable Information)
 */
object SecurityLogger {
    
    private const val TAG = "SecurityLogger"
    
    // Event types for categorization
    enum class SecurityEvent {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILED,
        PASSWORD_CHANGE,
        PASSWORD_CHANGE_FAILED,
        PASSWORD_RESET_REQUESTED,
        MFA_ENABLED,
        MFA_DISABLED,
        MFA_VERIFICATION_SUCCESS,
        MFA_VERIFICATION_FAILED,
        BIOMETRIC_ENABLED,
        BIOMETRIC_DISABLED,
        SESSION_EXPIRED,
        ACCOUNT_LOCKED,
        SUSPICIOUS_ACTIVITY
    }

    /**
     * Log successful login
     */
    fun logLoginSuccess(email: String) {
        logEvent(SecurityEvent.LOGIN_SUCCESS, "Login successful for user: ${maskEmail(email)}")
    }

    /**
     * Log failed login attempt
     */
    fun logLoginFailed(email: String, reason: String) {
        val sanitizedReason = sanitizeLogMessage(reason)
        logEvent(SecurityEvent.LOGIN_FAILED, "Login failed for user: ${maskEmail(email)}, reason: $sanitizedReason")
        
        // Check for suspicious patterns
        if (isSuspiciousLoginFailure(reason)) {
            logEvent(SecurityEvent.SUSPICIOUS_ACTIVITY, "Suspicious login failure pattern detected for: ${maskEmail(email)}")
        }
    }

    /**
     * Log registration attempt
     */
    fun logRegistrationAttempt(email: String) {
        logEvent(SecurityEvent.REGISTRATION_SUCCESS, "Registration attempt for: ${maskEmail(email)}")
    }

    /**
     * Log successful registration
     */
    fun logRegistrationSuccess(email: String) {
        logEvent(SecurityEvent.REGISTRATION_SUCCESS, "Registration successful for: ${maskEmail(email)}")
    }

    /**
     * Log failed registration
     */
    fun logRegistrationFailed(email: String, reason: String) {
        val sanitizedReason = sanitizeLogMessage(reason)
        logEvent(SecurityEvent.REGISTRATION_FAILED, "Registration failed for: ${maskEmail(email)}, reason: $sanitizedReason")
    }

    /**
     * Log successful password change
     */
    fun logPasswordChange(email: String) {
        logEvent(SecurityEvent.PASSWORD_CHANGE, "Password changed successfully for: ${maskEmail(email)}")
    }

    /**
     * Log failed password change
     */
    fun logPasswordChangeFailed(email: String, reason: String) {
        val sanitizedReason = sanitizeLogMessage(reason)
        logEvent(SecurityEvent.PASSWORD_CHANGE_FAILED, "Password change failed for: ${maskEmail(email)}, reason: $sanitizedReason")
    }

    /**
     * Log password reset requested
     */
    fun logPasswordResetRequested(email: String) {
        logEvent(SecurityEvent.PASSWORD_RESET_REQUESTED, "Password reset requested for: ${maskEmail(email)}")
    }

    /**
     * Log successful password change
     * Alias for logPasswordChange
     */
    fun logPasswordChanged(email: String) {
        logPasswordChange(email)
    }

    /**
     * Log MFA enable
     */
    fun logMfaEnabled(email: String) {
        logEvent(SecurityEvent.MFA_ENABLED, "MFA enabled for: ${maskEmail(email)}")
    }

    /**
     * Log MFA disable
     */
    fun logMfaDisabled(email: String) {
        logEvent(SecurityEvent.MFA_DISABLED, "MFA disabled for: ${maskEmail(email)}")
    }

    /**
     * Log MFA verification success
     */
    fun logMfaVerificationSuccess(email: String) {
        logEvent(SecurityEvent.MFA_VERIFICATION_SUCCESS, "MFA verification successful for: ${maskEmail(email)}")
    }

    /**
     * Log MFA verification failed
     */
    fun logMfaVerificationFailed(email: String) {
        logEvent(SecurityEvent.MFA_VERIFICATION_FAILED, "MFA verification failed for: ${maskEmail(email)}")
    }

    /**
     * Log biometric enable
     */
    fun logBiometricEnabled() {
        logEvent(SecurityEvent.BIOMETRIC_ENABLED, "Biometric authentication enabled")
    }

    /**
     * Log biometric disable
     */
    fun logBiometricDisabled() {
        logEvent(SecurityEvent.BIOMETRIC_DISABLED, "Biometric authentication disabled")
    }

    /**
     * Log session expiration
     */
    fun logSessionExpired(email: String) {
        logEvent(SecurityEvent.SESSION_EXPIRED, "Session expired for: ${maskEmail(email)}")
    }

    /**
     * Log account lockout
     */
    fun logAccountLocked(email: String, duration: String) {
        logEvent(SecurityEvent.ACCOUNT_LOCKED, "Account locked: ${maskEmail(email)} for $duration")
    }

    /**
     * Log suspicious activity
     */
    fun logSuspiciousActivity(description: String) {
        logEvent(SecurityEvent.SUSPICIOUS_ACTIVITY, "Suspicious activity detected: $description")
    }

    /**
     * Core logging function - logs to both Logcat and could be extended to send to backend
     */
    private fun logEvent(event: SecurityEvent, message: String) {
        val timestamp = getCurrentTimestamp()
        val logMessage = "[$timestamp] [${event.name}] $message"
        
        // Log to Logcat (debug level for non-critical events)
        when (event) {
            SecurityEvent.LOGIN_FAILED,
            SecurityEvent.REGISTRATION_FAILED,
            SecurityEvent.MFA_VERIFICATION_FAILED,
            SecurityEvent.ACCOUNT_LOCKED,
            SecurityEvent.SUSPICIOUS_ACTIVITY -> {
                Log.w(TAG, logMessage)
            }
            SecurityEvent.LOGIN_SUCCESS,
            SecurityEvent.PASSWORD_CHANGE,
            SecurityEvent.MFA_ENABLED,
            SecurityEvent.MFA_DISABLED -> {
                Log.i(TAG, logMessage)
            }
            else -> {
                Log.d(TAG, logMessage)
            }
        }
        
        // In a production app, you might also want to:
        // 1. Send logs to a secure backend service
        // 2. Store in encrypted local database for audit purposes
        // 3. Implement log rotation to prevent storage issues
    }

    /**
     * Mask email for logging - shows only first and last characters
     */
    private fun maskEmail(email: String): String {
        return if (email.contains("@")) {
            val parts = email.split("@")
            val localPart = parts[0]
            val domain = parts.getOrNull(1) ?: ""
            
            val maskedLocal = if (localPart.length <= 2) {
                localPart.first() + "***"
            } else {
                localPart.first() + "***" + localPart.last()
            }
            
            "$maskedLocal@$domain"
        } else {
            "***masked***"
        }
    }

    /**
     * Sanitize log message - remove potentially sensitive patterns
     */
    private fun sanitizeLogMessage(message: String): String {
        // Remove potential password patterns
        var sanitized = message.replace(Regex("password[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "password=***")
        // Remove potential token patterns
        sanitized = sanitized.replace(Regex("token[=:]\\s*\\S+", RegexOption.IGNORE_CASE), "token=***")
        // Remove potential JWT patterns
        sanitized = sanitized.replace(Regex("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "***JWT***")
        
        return sanitized
    }

    /**
     * Check if login failure reason indicates suspicious activity
     */
    private fun isSuspiciousLoginFailure(reason: String): Boolean {
        val suspiciousPatterns = listOf(
            "brute force",
            "rate limit",
            "too many attempts",
            "blocked",
            "suspicious"
        )
        
        return suspiciousPatterns.any { pattern ->
            reason.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Get current timestamp in ISO format
     */
    private fun getCurrentTimestamp(): String {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            LocalDateTime.now().format(formatter)
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * Log generic security event with custom message
     */
    fun logSecurityEvent(event: SecurityEvent, customMessage: String) {
        logEvent(event, customMessage)
    }
}
