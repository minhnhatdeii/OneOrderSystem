package com.example.oneorder_sm.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class LoginPreferences(
    val rememberMe: Boolean = false,
    val savedEmail: String = "",
    val savedPassword: String = ""
)

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val REMEMBER_ME = booleanPreferencesKey("remember_me")
        val SAVED_EMAIL = stringPreferencesKey("saved_email")
        val SAVED_PASSWORD = stringPreferencesKey("saved_password")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val loginPreferences: Flow<LoginPreferences> = context.dataStore.data.map { preferences ->
        LoginPreferences(
            rememberMe = preferences[PreferencesKeys.REMEMBER_ME] ?: false,
            savedEmail = preferences[PreferencesKeys.SAVED_EMAIL] ?: "",
            savedPassword = preferences[PreferencesKeys.SAVED_PASSWORD] ?: ""
        )
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "vi"
    }

    suspend fun saveLoginPreferences(
        rememberMe: Boolean,
        email: String = "",
        password: String = ""
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_ME] = rememberMe
            if (rememberMe) {
                preferences[PreferencesKeys.SAVED_EMAIL] = email
                preferences[PreferencesKeys.SAVED_PASSWORD] = password
            } else {
                preferences.remove(PreferencesKeys.SAVED_EMAIL)
                preferences.remove(PreferencesKeys.SAVED_PASSWORD)
            }
        }
    }

    suspend fun clearSavedCredentials() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SAVED_EMAIL)
            preferences.remove(PreferencesKeys.SAVED_PASSWORD)
            preferences[PreferencesKeys.REMEMBER_ME] = false
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun saveAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = lang
        }
    }
}
