package com.example.oneorder.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SecurityUtils - Rate limiting and abuse prevention utilities
 * 
 * Features:
 * - Login attempt rate limiting (max 5 attempts per 5 minutes)
 * - Account lockout after repeated failures (15 minutes after 10 failed attempts)
 * - Registration attempt rate limiting (3 attempts per hour)
 * - Input sanitization for XSS prevention
 * - Exponential backoff for failed attempts
 */
@Singleton
class SecurityUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Login rate limiting
        private const val LOGIN_MAX_ATTEMPTS = 5
        private const val LOGIN_WINDOW_MS = 5 * 60 * 1000L // 5 minutes

        // Account lockout
        private const val LOCKOUT_MAX_ATTEMPTS = 10
        private const val LOCKOUT_DURATION_MS = 15 * 60 * 1000L // 15 minutes

        // Registration rate limiting
        private const val REGISTER_MAX_ATTEMPTS = 3
        private const val REGISTER_WINDOW_MS = 60 * 60 * 1000L // 1 hour

        private const val PREFS_NAME = "security_prefs"

        // Storage keys
        const val KEY_LOCKOUT_PREFIX = "lockout_"
        const val KEY_ATTEMPTS_PREFIX = "attempts_"
        const val KEY_LAST_REGISTER_ATTEMPT = "last_register_attempt"
        const val KEY_REGISTER_ATTEMPT_COUNT = "register_attempt_count"
        const val ATTEMPT_TYPE_LOGIN = "login"
        const val ATTEMPT_TYPE_REGISTER = "register"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    // In-memory tracking for current session
    private val _loginAttempts = mutableMapOf<String, MutableList<Long>>()
    private val _lockoutStatus = MutableStateFlow<LockoutStatus>(LockoutStatus.NotLocked)
    
    val lockoutStatus: StateFlow<LockoutStatus> = _lockoutStatus.asStateFlow()

    data class LockoutStatus(
        val isLocked: Boolean = false,
        val remainingTimeMs: Long = 0L,
        val retryAfterMs: Long = 0L
    ) {
        companion object {
            val NotLocked = LockoutStatus(isLocked = false)
        }
    }

    /**
     * Check if login is allowed based on rate limiting
     */
    fun isLoginAllowed(email: String): RateLimitResult {
        val emailLower = email.lowercase()
        
        // Check if currently locked out
        if (isLockedOut(emailLower)) {
            val remainingTime = getRemainingLockoutTime(emailLower)
            _lockoutStatus.value = LockoutStatus(
                isLocked = true,
                remainingTimeMs = remainingTime,
                retryAfterMs = remainingTime
            )
            return RateLimitResult.Blocked(
                reason = "Tài khoản tạm khóa do đăng nhập sai nhiều lần",
                retryAfterMs = remainingTime
            )
        }
        
        // Check rate limit
        val attempts = _loginAttempts[emailLower] ?: mutableListOf()
        val now = System.currentTimeMillis()
        
        // Remove old attempts outside the window
        attempts.removeAll { now - it > LOGIN_WINDOW_MS }
        
        if (attempts.size >= LOGIN_MAX_ATTEMPTS) {
            val oldestAttempt = attempts.minOrNull() ?: now
            val retryAfter = LOGIN_WINDOW_MS - (now - oldestAttempt)
            
            return RateLimitResult.RateLimited(
                message = "Quá nhiều lần thử. Vui lòng đợi ${retryAfter / 1000} giây.",
                retryAfterMs = retryAfter
            )
        }
        
        return RateLimitResult.Allowed
    }

    /**
     * Record a failed login attempt
     */
    fun recordFailedLoginAttempt(email: String) {
        val emailLower = email.lowercase()
        val now = System.currentTimeMillis()
        
        // Get or create attempt list
        val attempts = _loginAttempts.getOrPut(emailLower) { mutableListOf() }
        attempts.add(now)
        
        // Save to persistent storage for cross-session tracking
        saveAttemptToStorage(emailLower, now, ATTEMPT_TYPE_LOGIN)
        
        // Check if should trigger lockout
        val totalAttempts = getTotalAttempts(emailLower, ATTEMPT_TYPE_LOGIN)
        if (totalAttempts >= LOCKOUT_MAX_ATTEMPTS) {
            setLockout(emailLower)
        }
        
        // Log suspicious activity if too many failures
        if (totalAttempts >= 5) {
            SecurityLogger.logSuspiciousActivity("Multiple login failures for ${maskEmail(email)}: $totalAttempts attempts")
        }
    }

    /**
     * Record a successful login - clears attempts
     */
    fun recordSuccessfulLogin(email: String) {
        val emailLower = email.lowercase()
        
        // Clear in-memory attempts
        _loginAttempts[emailLower]?.clear()
        
        // Clear persistent attempts
        clearAttemptsFromStorage(emailLower, ATTEMPT_TYPE_LOGIN)
        
        // Clear lockout
        clearLockout(emailLower)
        
        // Update lockout status
        _lockoutStatus.value = LockoutStatus.NotLocked
    }

    /**
     * Check if registration is allowed
     */
    fun isRegistrationAllowed(): RateLimitResult {
        val lastAttempt = securePrefs.getLong(KEY_LAST_REGISTER_ATTEMPT, 0L)
        val attemptCount = securePrefs.getInt(KEY_REGISTER_ATTEMPT_COUNT, 0)
        val now = System.currentTimeMillis()
        
        // Check if within rate limit window
        if (now - lastAttempt < REGISTER_WINDOW_MS && attemptCount >= REGISTER_MAX_ATTEMPTS) {
            val retryAfter = REGISTER_WINDOW_MS - (now - lastAttempt)
            return RateLimitResult.RateLimited(
                message = "Quá nhiều lần đăng ký. Vui lòng thử lại sau.",
                retryAfterMs = retryAfter
            )
        }
        
        return RateLimitResult.Allowed
    }

    /**
     * Record a failed registration attempt
     */
    fun recordFailedRegistration() {
        val now = System.currentTimeMillis()
        val lastAttempt = securePrefs.getLong(KEY_LAST_REGISTER_ATTEMPT, 0L)
        val attemptCount = securePrefs.getInt(KEY_REGISTER_ATTEMPT_COUNT, 0)
        
        // Reset counter if window expired
        if (now - lastAttempt > REGISTER_WINDOW_MS) {
            securePrefs.edit()
                .putLong(KEY_LAST_REGISTER_ATTEMPT, now)
                .putInt(KEY_REGISTER_ATTEMPT_COUNT, 1)
                .apply()
        } else {
            securePrefs.edit()
                .putInt(KEY_REGISTER_ATTEMPT_COUNT, attemptCount + 1)
                .apply()
        }
    }

    /**
     * Record a successful registration
     */
    fun recordSuccessfulRegistration() {
        securePrefs.edit()
            .putLong(KEY_LAST_REGISTER_ATTEMPT, 0L)
            .putInt(KEY_REGISTER_ATTEMPT_COUNT, 0)
            .apply()
    }

    /**
     * Calculate backoff time for failed attempts
     */
    fun getBackoffTime(failedAttempts: Int): Long {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s, 32s, 64s...
        return minOf(1000L * (1 shl failedAttempts), 5 * 60 * 1000L) // Max 5 minutes
    }

    /**
     * Sanitize input to prevent XSS and injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
            .trim()
    }

    /**
     * Validate string doesn't contain dangerous patterns
     */
    fun isInputSafe(input: String): Boolean {
        val dangerousPatterns = listOf(
            Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE),
            Regex("javascript:", RegexOption.IGNORE_CASE),
            Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE),
            Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE),
            Regex("union\\s+select", RegexOption.IGNORE_CASE),
            Regex("drop\\s+table", RegexOption.IGNORE_CASE),
            Regex("insert\\s+into", RegexOption.IGNORE_CASE)
        )
        
        return dangerousPatterns.none { it.containsMatchIn(input) }
    }

    private fun isLockedOut(email: String): Boolean {
        val lockoutEnd = securePrefs.getLong("${KEY_LOCKOUT_PREFIX}$email", 0L)
        return lockoutEnd > System.currentTimeMillis()
    }

    private fun getRemainingLockoutTime(email: String): Long {
        val lockoutEnd = securePrefs.getLong("${KEY_LOCKOUT_PREFIX}$email", 0L)
        return maxOf(0L, lockoutEnd - System.currentTimeMillis())
    }

    private fun setLockout(email: String) {
        val lockoutEnd = System.currentTimeMillis() + LOCKOUT_DURATION_MS
        securePrefs.edit()
            .putLong("${KEY_LOCKOUT_PREFIX}$email", lockoutEnd)
            .apply()
        
        SecurityLogger.logAccountLocked(email, "${LOCKOUT_DURATION_MS / 60000} minutes")
    }

    private fun clearLockout(email: String) {
        securePrefs.edit()
            .remove("${KEY_LOCKOUT_PREFIX}$email")
            .apply()
    }

    private fun getTotalAttempts(email: String, type: String): Int {
        return securePrefs.getInt("${KEY_ATTEMPTS_PREFIX}${type}_$email", 0)
    }

    private fun saveAttemptToStorage(email: String, timestamp: Long, type: String) {
        val key = "${KEY_ATTEMPTS_PREFIX}${type}_$email"
        val current = securePrefs.getInt(key, 0)
        securePrefs.edit().putInt(key, current + 1).apply()
    }

    private fun clearAttemptsFromStorage(email: String, type: String) {
        val key = "${KEY_ATTEMPTS_PREFIX}${type}_$email"
        securePrefs.edit().remove(key).apply()
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
 * Result of rate limit check
 */
sealed class RateLimitResult {
    data object Allowed : RateLimitResult()
    
    data class RateLimited(
        val message: String,
        val retryAfterMs: Long
    ) : RateLimitResult()
    
    data class Blocked(
        val reason: String,
        val retryAfterMs: Long
    ) : RateLimitResult()
}
