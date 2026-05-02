package com.example.oneorder.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SessionManager - Manages user session lifecycle
 * 
 * Features:
 * - Track session start time
 * - Configurable session timeout
 * - Auto-logout on session expiration
 * - Activity lifecycle monitoring
 * - Session extension on activity
 */
@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager
) {
    companion object {
        private const val SESSION_PREFS_NAME = "session_prefs"
        private const val KEY_SESSION_START = "session_start_time"
        private const val KEY_LAST_ACTIVITY = "last_activity_time"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
        private const val KEY_IS_SESSION_ACTIVE = "is_session_active"
        
        // Default session timeout: 30 minutes
        const val DEFAULT_SESSION_TIMEOUT_MS = 30 * 60 * 1000L
        
        // Warning time before session expires: 5 minutes
        const val SESSION_WARNING_BEFORE_MS = 5 * 60 * 1000L
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sessionPrefs by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                SESSION_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(SESSION_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /**
     * Session state data class
     */
    data class SessionState(
        val isActive: Boolean = false,
        val sessionStartTime: Long = 0L,
        val lastActivityTime: Long = 0L,
        val timeRemainingMs: Long = 0L,
        val isExpiringSoon: Boolean = false,
        val shouldShowWarning: Boolean = false
    )

    /**
     * Start a new session
     */
    fun startSession() {
        val now = System.currentTimeMillis()
        
        sessionPrefs.edit().apply {
            putLong(KEY_SESSION_START, now)
            putLong(KEY_LAST_ACTIVITY, now)
            putBoolean(KEY_IS_SESSION_ACTIVE, true)
            apply()
        }
        
        updateSessionState()
    }

    /**
     * Update last activity time (call on user interaction)
     */
    fun updateLastActivity() {
        val now = System.currentTimeMillis()
        
        sessionPrefs.edit().apply {
            putLong(KEY_LAST_ACTIVITY, now)
            apply()
        }
        
        updateSessionState()
    }

    /**
     * End the current session (logout)
     */
    fun endSession() {
        sessionPrefs.edit().apply {
            putBoolean(KEY_IS_SESSION_ACTIVE, false)
            remove(KEY_SESSION_START)
            remove(KEY_LAST_ACTIVITY)
            apply()
        }
        
        _sessionState.value = SessionState(isActive = false)
    }

    /**
     * Check if session is still valid
     */
    fun isSessionValid(): Boolean {
        if (!sessionPrefs.getBoolean(KEY_IS_SESSION_ACTIVE, false)) {
            return false
        }
        
        val lastActivity = sessionPrefs.getLong(KEY_LAST_ACTIVITY, 0L)
        val timeout = getSessionTimeoutMs()
        val now = System.currentTimeMillis()
        
        return (now - lastActivity) < timeout
    }

    /**
     * Check if session is expired
     */
    fun isSessionExpired(): Boolean {
        return !isSessionValid()
    }

    /**
     * Get remaining time in session
     */
    fun getTimeRemaining(): Long {
        if (!isSessionValid()) return 0L
        
        val lastActivity = sessionPrefs.getLong(KEY_LAST_ACTIVITY, 0L)
        val timeout = getSessionTimeoutMs()
        val now = System.currentTimeMillis()
        
        return maxOf(0L, timeout - (now - lastActivity))
    }

    /**
     * Get session timeout in milliseconds
     */
    private fun getSessionTimeoutMs(): Long {
        val timeoutMinutes = sessionPrefs.getInt(KEY_SESSION_TIMEOUT, 30)
        return timeoutMinutes * 60 * 1000L
    }

    /**
     * Set session timeout in minutes
     */
    fun setSessionTimeout(minutes: Int) {
        sessionPrefs.edit().apply {
            putInt(KEY_SESSION_TIMEOUT, minutes)
            apply()
        }
        updateSessionState()
    }

    /**
     * Extend session (reset activity time)
     */
    fun extendSession() {
        updateLastActivity()
    }

    /**
     * Check if session should show warning (5 minutes before expiry)
     */
    fun shouldShowWarning(): Boolean {
        if (!isSessionValid()) return false
        
        val remaining = getTimeRemaining()
        return remaining > 0 && remaining <= SESSION_WARNING_BEFORE_MS
    }

    /**
     * Check if session is expiring soon (1 minute before expiry)
     */
    fun isExpiringSoon(): Boolean {
        if (!isSessionValid()) return false
        
        val remaining = getTimeRemaining()
        return remaining > 0 && remaining <= 60 * 1000L
    }

    /**
     * Update internal session state
     */
    private fun updateSessionState() {
        val isActive = isSessionValid()
        val startTime = sessionPrefs.getLong(KEY_SESSION_START, 0L)
        val lastActivity = sessionPrefs.getLong(KEY_LAST_ACTIVITY, 0L)
        val remaining = if (isActive) getTimeRemaining() else 0L
        
        _sessionState.value = SessionState(
            isActive = isActive,
            sessionStartTime = startTime,
            lastActivityTime = lastActivity,
            timeRemainingMs = remaining,
            isExpiringSoon = remaining > 0 && remaining <= 60 * 1000L,
            shouldShowWarning = remaining > 0 && remaining <= SESSION_WARNING_BEFORE_MS
        )
    }

    /**
     * Refresh session state
     */
    fun refreshSessionState() {
        updateSessionState()
    }

    /**
     * Get formatted time remaining string
     */
    fun getTimeRemainingFormatted(): String {
        val remaining = getTimeRemaining()
        val minutes = (remaining / 1000 / 60).toInt()
        val seconds = ((remaining / 1000) % 60).toInt()
        
        return if (minutes > 0) {
            "$minutes phút $seconds giây"
        } else {
            "$seconds giây"
        }
    }

    /**
     * Check if user is logged in (has valid session)
     */
    fun isLoggedIn(): Boolean {
        return securePreferencesManager.getLastLoginTime() > 0 && !isSessionExpired()
    }

    /**
     * Handle app going to background
     */
    fun onAppBackground() {
        // Optionally lock session when app goes to background
        // For now, we just record the time
    }

    /**
     * Handle app coming to foreground
     */
    fun onAppForeground() {
        // Check if session expired while in background
        if (isSessionExpired()) {
            endSession()
        } else {
            updateSessionState()
        }
    }
}
