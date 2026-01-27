package com.example.oneorder.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder.ui.screens.auth.LoginScreen
import com.example.oneorder.ui.screens.auth.RegisterScreen
import com.example.oneorder.ui.screens.main.MainScreen
import com.example.oneorder.ui.screens.menu.MenuScreen
import com.example.oneorder.ui.screens.checkout.CheckoutScreen
import com.example.oneorder.ui.screens.qr.QRScannerScreen
import com.example.oneorder.ui.screens.order.OrderDetailScreen
import androidx.navigation.toRoute

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login()
    ) {
        composable<Screen.Login> { backStackEntry ->
            val login = backStackEntry.toRoute<Screen.Login>()
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onNavigateToHome = { navController.navigate(Screen.Home) { popUpTo(Screen.Login()) { inclusive = true } } },
                successMessage = login.message
            )
        }
        composable<Screen.Register> {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { message ->
                    navController.navigate(Screen.Login(message = message)) {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                }
            )
        }
        
        // Main screen with bottom navigation
        composable<Screen.Home> {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.Home)
            }
            val homeViewModel = hiltViewModel<com.example.oneorder.ui.screens.home.HomeViewModel>(parentEntry)
            
            MainScreen(
                homeViewModel = homeViewModel,
                onNavigateToMenu = { categoryId -> navController.navigate(Screen.Menu(categoryId)) },
                onNavigateToQRScanner = { navController.navigate(Screen.QRScanner) },
                onNavigateToCheckout = { navController.navigate(Screen.Checkout) },
                onNavigateToOrderDetail = { orderId -> navController.navigate(Screen.OrderDetail(orderId)) },
                onSignedOut = { 
                    navController.navigate(Screen.Login()) {
                        popUpTo(0) { inclusive = true } // Clear all backstack
                    }
                }
            )
        }
        
        // Full screen destinations (no bottom nav)
        composable<Screen.Menu> { backStackEntry ->
            val menu = backStackEntry.toRoute<Screen.Menu>()
            MenuScreen(
                categoryId = menu.categoryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Checkout> {
            CheckoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrderHistory = { 
                     navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                     }
                }
            )
        }
        
        composable<Screen.QRScanner> {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.Home)
            }
            val homeViewModel = hiltViewModel<com.example.oneorder.ui.screens.home.HomeViewModel>(parentEntry)
            
            QRScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanSuccess = { restaurantId, tableId ->
                    // Set restaurant and table info in HomeViewModel
                    homeViewModel.setRestaurantAndTable(restaurantId, tableId)
                    // Navigate back to home
                    navController.popBackStack()
                }
            )
        }
        
        // Order Detail
        composable<Screen.OrderDetail> { backStackEntry ->
            val orderDetail = backStackEntry.toRoute<Screen.OrderDetail>()
            OrderDetailScreen(
                orderId = orderDetail.orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
