package com.example.oneorder_sm.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.oneorder_sm.ui.screens.auth.WelcomeScreen
import com.example.oneorder_sm.ui.screens.auth.RestaurantLoginScreen
import com.example.oneorder_sm.ui.screens.auth.StaffLoginScreen
import com.example.oneorder_sm.ui.screens.auth.RegisterRestaurantScreen
import com.example.oneorder_sm.ui.screens.main.MainScreen
import com.example.oneorder_sm.ui.screens.orders.OrderDetailScreen

@Composable
fun AppNavigation(
    pendingDeepLink: Uri? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    
    // Handle email confirmation deep link
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { uri ->
            android.util.Log.d("AppNavigation", "Processing deep link: $uri")
            
            if (uri.scheme == "oneorder" && uri.host == "confirm") {
                // Email confirmation link
                // Navigate to login screen and show confirmation message
                android.util.Log.d("AppNavigation", "Email confirmed! Navigating to login...")
                navController.navigate(Screen.RestaurantLogin)
                onDeepLinkHandled()
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
