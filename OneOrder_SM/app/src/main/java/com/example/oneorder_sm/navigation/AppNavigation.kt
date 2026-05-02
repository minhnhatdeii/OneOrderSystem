package com.example.oneorder_sm.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.oneorder_sm.MainActivity
import com.example.oneorder_sm.ui.screens.auth.WelcomeScreen
import com.example.oneorder_sm.ui.screens.auth.RestaurantLoginScreen
import com.example.oneorder_sm.ui.screens.auth.StaffLoginScreen
import com.example.oneorder_sm.ui.screens.auth.RegisterRestaurantScreen
import com.example.oneorder_sm.ui.screens.auth.ForgotPasswordScreen
import com.example.oneorder_sm.ui.screens.auth.PasswordResetScreen
import com.example.oneorder_sm.ui.screens.main.MainScreen
import com.example.oneorder_sm.ui.screens.orders.OrderDetailScreen

@Composable
fun AppNavigation(
    pendingDeepLink: Uri? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Handle deep links
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { uri ->
            android.util.Log.d("AppNavigation", "Processing deep link: $uri")
            
            when {
                uri.scheme == "oneorder" && uri.host == "confirm" -> {
                    // Email confirmation link
                    android.util.Log.d("AppNavigation", "Email confirmed! Navigating to login...")
                    navController.navigate(Screen.RestaurantLogin)
                    onDeepLinkHandled()
                }
                uri.scheme == "oneorder" && uri.host == "password-reset" -> {
                    // Password reset link — navigate to reset screen
                    android.util.Log.d("AppNavigation", "Password reset! Navigating to reset screen...")
                    val token = uri.getQueryParameter("token")
                    val email = uri.getQueryParameter("email")
                    navController.navigate(Screen.PasswordReset(
                        token = token,
                        email = email
                    )) {
                        popUpTo(0) { inclusive = true }
                    }
                    onDeepLinkHandled()
                    // Also clear the static variable since we've passed via route
                    MainActivity.pendingPasswordResetLink = null
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome
    ) {
        // Welcome screen - entry point with 2 login options
        composable<Screen.Welcome> {
            WelcomeScreen(
                onRestaurantLogin = {
                    navController.navigate(Screen.RestaurantLogin)
                },
                onStaffLogin = {
                    navController.navigate(Screen.StaffLogin)
                }
            )
        }
        
        // Restaurant Login (for managers)
        composable<Screen.RestaurantLogin> {
            RestaurantLoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main) {
                        popUpTo(Screen.Welcome) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword)
                }
            )
        }
        
        // Staff Login (for employees)
        composable<Screen.StaffLogin> {
            StaffLoginScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Main) {
                        popUpTo(Screen.Welcome) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword)
                }
            )
        }
        
        // Registration screen for new restaurant owners
        composable<Screen.Register> {
            RegisterRestaurantScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onRegistrationSuccess = {
                    // Navigate to login screen after registration
                    // User needs to confirm email and then log in
                    navController.navigate(Screen.RestaurantLogin) {
                        popUpTo(Screen.Welcome) { inclusive = false }
                    }
                }
            )
        }
        
        // Forgot Password Screen
        composable<Screen.ForgotPassword> {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.RestaurantLogin) {
                        popUpTo(Screen.Welcome) { inclusive = false }
                    }
                }
            )
        }
        
        // Password Reset Screen (from email deep link)
        composable<Screen.PasswordReset> { backStackEntry ->
            val route: Screen.PasswordReset = backStackEntry.toRoute()
            PasswordResetScreen(
                token = route.token,
                email = route.email,
                onNavigateBack = {
                    navController.navigate(Screen.RestaurantLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPasswordResetComplete = {
                    navController.navigate(Screen.RestaurantLogin) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Main screen with sidebar navigation
        composable<Screen.Main> {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Welcome) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Order detail screen (opened from main screen)
        composable<Screen.OrderDetail> { backStackEntry ->
            val orderDetail: Screen.OrderDetail = backStackEntry.toRoute()
            OrderDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
