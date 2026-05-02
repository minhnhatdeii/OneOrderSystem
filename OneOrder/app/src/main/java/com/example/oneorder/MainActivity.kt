package com.example.oneorder

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.oneorder.data.local.UserPreferencesManager
import com.example.oneorder.data.local.dataStore
import com.example.oneorder.ui.theme.OneOrderTheme
import com.example.oneorder.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import androidx.datastore.preferences.core.stringPreferencesKey

/** CompositionLocal để truyền dark-mode state xuống toàn bộ cây Compose */
val LocalDarkTheme = compositionLocalOf { false }

/** Deep link state holder */
data class DeepLinkState(
    val passwordResetToken: String? = null,
    val passwordResetEmail: String? = null,
    val mfaToken: String? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    companion object {
        /**
         * Lưu lại ngôn ngữ đã áp dụng khi activity khởi động.
         * Dùng để phát hiện khi user thực sự đổi ngôn ngữ, tránh vòng lặp recreate.
         */
        private var appliedLanguage: String = "vi"

        /** Static deep link state to be read by navigation on start */
        var pendingDeepLink: DeepLinkState? = null
    }

    /**
     * Override attachBaseContext để apply locale ĐỒNG BỘ ngay khi activity tạo.
     * Đây là cách chuẩn Android để thay đổi ngôn ngữ — không cần recreate trong Compose.
     */
    override fun attachBaseContext(newBase: Context) {
        // Đọc ngôn ngữ đã lưu từ DataStore đồng bộ (chỉ gọi 1 lần khi attach)
        val savedLang = runBlocking {
            newBase.dataStore.data
                .map { prefs -> prefs[stringPreferencesKey("app_language")] ?: "vi" }
                .first()
        }
        appliedLanguage = savedLang

        val locale = Locale(savedLang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)

        // createConfigurationContext áp dụng locale vào Context — chuẩn nhất
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_OneOrder)

        super.onCreate(savedInstanceState)

        // Handle deep link from intent
        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            val themeMode by userPreferencesManager.themeMode.collectAsState(initial = "SYSTEM")
            val appLanguage by userPreferencesManager.appLanguage.collectAsState(initial = appliedLanguage)

            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            // Chỉ recreate khi user THỰC SỰ đổi ngôn ngữ khác với ngôn ngữ đang áp dụng.
            // Dùng companion object appliedLanguage thay vì initial/remember để tránh vòng lặp.
            LaunchedEffect(appLanguage) {
                if (appLanguage != appliedLanguage) {
                    // Gọi recreate 1 lần → attachBaseContext sẽ pick up locale mới từ DataStore
                    recreate()
                }
            }

            CompositionLocalProvider(
                LocalDarkTheme provides darkTheme
            ) {
                OneOrderTheme(darkTheme = darkTheme) {
                    androidx.compose.material3.Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            // Handle oneorder://password-reset?token=xxx&email=xxx
            if (uri.scheme == "oneorder" && uri.host == "password-reset") {
                val token = uri.getQueryParameter("token")
                val email = uri.getQueryParameter("email")
                pendingDeepLink = DeepLinkState(
                    passwordResetToken = token,
                    passwordResetEmail = email
                )
            }
            // Handle oneorder://mfa-verify?token=xxx
            else if (uri.scheme == "oneorder" && uri.host == "mfa-verify") {
                val token = uri.getQueryParameter("token")
                pendingDeepLink = DeepLinkState(mfaToken = token)
            }
        }
    }
}