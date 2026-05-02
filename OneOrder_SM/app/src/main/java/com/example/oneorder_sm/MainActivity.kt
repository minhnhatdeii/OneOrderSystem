package com.example.oneorder_sm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import com.example.oneorder_sm.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var pendingDeepLink: Uri? = null
    
    companion object {
        /** Static deep link state for password reset to be read by navigation */
        var pendingPasswordResetLink: PasswordResetDeepLink? = null
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Handle deep link if present
        handleDeepLink(intent)
        
        enableEdgeToEdge()
        setContent {
            OneOrder_SMTheme {
                AppNavigation(
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            android.util.Log.d("MainActivity", "Deep link received: $uri")
            
            when {
                uri.scheme == "oneorder" && uri.host == "confirm" -> {
                    android.util.Log.d("MainActivity", "Email confirmation deep link detected")
                    pendingDeepLink = uri
                    // Will be handled by AppNavigation composable
                }
                uri.scheme == "oneorder" && uri.host == "password-reset" -> {
                    android.util.Log.d("MainActivity", "Password reset deep link detected")
                    val token = uri.getQueryParameter("token")
                    val email = uri.getQueryParameter("email")
                    pendingPasswordResetLink = PasswordResetDeepLink(
                        token = token,
                        email = email
                    )
                    pendingDeepLink = uri
                }
                else -> {
                    android.util.Log.w("MainActivity", "Unknown deep link: $uri")
                }
            }
        }
    }
}

/**
 * Deep link state for password reset
 */
data class PasswordResetDeepLink(
    val token: String? = null,
    val email: String? = null
)