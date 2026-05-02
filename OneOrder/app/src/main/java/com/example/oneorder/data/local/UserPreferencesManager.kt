package com.example.oneorder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * UserPreferencesManager - Handles non-sensitive user preferences
 *
 * NOTE: For secure credential storage (email/password), use SecurePreferencesManager instead.
 * This class only handles theme and language preferences which are non-sensitive.
 */
@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePreferencesManager: SecurePreferencesManager
) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    /**
     * Get stored login preferences from secure storage
     */
    val loginPreferences: Flow<SecureLoginCredentials> = securePreferencesManager.credentialsFlow

    /**
     * Save login preferences securely using EncryptedSharedPreferences
     */
    suspend fun saveLoginPreferences(
        rememberMe: Boolean,
        email: String = "",
        password: String = ""
    ) {
        securePreferencesManager.saveLoginCredentials(rememberMe, email, password)
    }

    /**
     * Clear saved credentials from secure storage
     */
    suspend fun clearSavedCredentials() {
        securePreferencesManager.clearCredentials()
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "vi" // Default to Vietnamese
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun saveAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = languageCode
        }
    }
}
