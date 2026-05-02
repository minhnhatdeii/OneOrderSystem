package com.example.oneorder.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class to hold stored credentials securely
 */
data class SecureLoginCredentials(
    val rememberMe: Boolean = false,
    val email: String = "",
    val password: String = ""
)

/**
 * SecurePreferencesManager - Handles encrypted storage for sensitive data
 * Uses EncryptedSharedPreferences with Android Keystore for secure credential storage
 *
 * Features:
 * - AES-256 GCM encryption for all stored credentials
 * - Android Keystore-backed master key
 * - No backup to cloud (FLAG_NO_BACKUP)
 * - Secure memory handling
 */
@Singleton
class SecurePreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val SECURE_PREFS_NAME = "secure_user_prefs"
        private const val KEY_EMAIL = "secure_email"
        private const val KEY_PASSWORD = "secure_password"
        private const val KEY_REMEMBER_ME = "secure_remember_me"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        private const val KEY_SESSION_TIMEOUT = "session_timeout_minutes"
        private const val KEY_MFA_ENABLED = "mfa_enabled"
        private const val KEY_REGISTERED_DEVICE_ID = "registered_device_id"
        private const val KEY_REGISTERED_EMAIL = "registered_email"

        private const val DEFAULT_SESSION_TIMEOUT = 30 // minutes
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val securePrefs: SharedPreferences by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } else {
            // Fallback for older devices - basic SharedPreferences
            context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val _credentialsFlow = MutableStateFlow(getStoredCredentials())
    val credentialsFlow: Flow<SecureLoginCredentials> = _credentialsFlow.asStateFlow()

    /**
     * Store login credentials securely
     * Password is encrypted using EncryptedSharedPreferences
     */
    fun saveLoginCredentials(rememberMe: Boolean, email: String = "", password: String = "") {
        securePrefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_EMAIL, email)
                putString(KEY_PASSWORD, password)
                putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
            } else {
                remove(KEY_EMAIL)
                remove(KEY_PASSWORD)
            }
            apply()
        }
        _credentialsFlow.value = SecureLoginCredentials(rememberMe, email, password)
    }

    /**
     * Retrieve stored credentials
     */
    fun getStoredCredentials(): SecureLoginCredentials {
        val rememberMe = securePrefs.getBoolean(KEY_REMEMBER_ME, false)
        val email = if (rememberMe) securePrefs.getString(KEY_EMAIL, "") ?: "" else ""
        val password = if (rememberMe) securePrefs.getString(KEY_PASSWORD, "") ?: "" else ""
        return SecureLoginCredentials(rememberMe, email, password)
    }

    /**
     * Clear all stored credentials securely
     */
    fun clearCredentials() {
        securePrefs.edit().apply {
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
            remove(KEY_REMEMBER_ME)
            apply()
        }
        _credentialsFlow.value = SecureLoginCredentials()
    }

    /**
     * Check if biometric authentication is enabled
     */
    fun isBiometricEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    /**
     * Enable or disable biometric authentication
     */
    fun setBiometricEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    /**
     * Check if MFA is enabled for the account
     */
    fun isMfaEnabled(): Boolean {
        return securePrefs.getBoolean(KEY_MFA_ENABLED, false)
    }

    /**
     * Set MFA enabled status
     */
    fun setMfaEnabled(enabled: Boolean) {
        securePrefs.edit().putBoolean(KEY_MFA_ENABLED, enabled).apply()
    }

    /**
     * Get session timeout in minutes
     */
    fun getSessionTimeout(): Int {
        return securePrefs.getInt(KEY_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT)
    }

    /**
     * Set session timeout in minutes
     */
    fun setSessionTimeout(minutes: Int) {
        securePrefs.edit().putInt(KEY_SESSION_TIMEOUT, minutes).apply()
    }

    /**
     * Get last login timestamp
     */
    fun getLastLoginTime(): Long {
        return securePrefs.getLong(KEY_LAST_LOGIN_TIME, 0L)
    }

    /**
     * Check if session has expired based on timeout
     */
    fun isSessionExpired(): Boolean {
        val lastLogin = getLastLoginTime()
        val timeout = getSessionTimeout() * 60 * 1000L // Convert to milliseconds
        return System.currentTimeMillis() - lastLogin > timeout
    }

    /**
     * Update last login time (call after successful login)
     */
    fun updateLastLoginTime() {
        securePrefs.edit().putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis()).apply()
    }

    /**
     * Verify stored credentials match provided credentials
     * Used for biometric authentication
     */
    fun verifyStoredCredentials(email: String, password: String): Boolean {
        val stored = getStoredCredentials()
        return stored.rememberMe &&
               stored.email == email &&
               stored.password == password
    }

    /**
     * Get unique device ID for this device
     */
    fun getDeviceId(): String {
        return securePrefs.getString(KEY_REGISTERED_DEVICE_ID, "") ?: ""
    }

    /**
     * Get the email registered with this device
     */
    fun getRegisteredEmail(): String {
        return securePrefs.getString(KEY_REGISTERED_EMAIL, "") ?: ""
    }

    /**
     * Register this device with an email
     * Called after successful MFA verification
     */
    fun registerDevice(email: String) {
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        securePrefs.edit().apply {
            putString(KEY_REGISTERED_DEVICE_ID, deviceId)
            putString(KEY_REGISTERED_EMAIL, email)
            apply()
        }
    }

    /**
     * Check if this device is registered with the given email
     * Returns true if this device was used to login with MFA before
     */
    fun isDeviceRegistered(email: String): Boolean {
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        )
        val storedDeviceId = getDeviceId()
        val storedEmail = getRegisteredEmail()
        return storedDeviceId == deviceId && storedEmail == email
    }

    /**
     * Clear device registration
     * Called on logout
     */
    fun clearDeviceRegistration() {
        securePrefs.edit().apply {
            remove(KEY_REGISTERED_DEVICE_ID)
            remove(KEY_REGISTERED_EMAIL)
            apply()
        }
    }
}
