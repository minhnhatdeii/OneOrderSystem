package com.example.oneorder.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.oneorder.ui.screens.cart.CartScreen
import com.example.oneorder.ui.screens.home.HomeScreen
import com.example.oneorder.ui.screens.order.OrderHistoryScreen
import com.example.oneorder.ui.screens.profile.ProfileScreen

/**
 * Bottom navigation destinations
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Cart : BottomNavItem(
        route = "cart",
        title = "Cart",
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    )
    
    data object Orders : BottomNavItem(
        route = "orders",
        title = "Orders",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )
    
    data object Profile : BottomNavItem(
        route = "profile",
        title = "Profile",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

/**
 * Main screen with bottom navigation
 */
@Composable
fun MainScreen(
    homeViewModel: com.example.oneorder.ui.screens.home.HomeViewModel,
    onNavigateToMenu: (Long) -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    onSignedOut: () -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Cart,
        BottomNavItem.Orders,
        BottomNavItem.Profile
    )
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                items = items,
                navController = navController
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToMenu = onNavigateToMenu,
                    onNavigateToCart = { navController.navigate(BottomNavItem.Cart.route) },
                    onNavigateToProfile = { navController.navigate(BottomNavItem.Profile.route) },
                    onNavigateToQRScanner = onNavigateToQRScanner
                )
            }
            
            composable(BottomNavItem.Cart.route) {
                CartScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onCheckout = onNavigateToCheckout
                )
            }
            
            composable(BottomNavItem.Orders.route) {
                OrderHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToDetail = { orderId ->
                        onNavigateToOrderDetail(orderId)
                    }
                )
            }
            
            composable(BottomNavItem.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSignedOut = onSignedOut
                )
            }
        }
    }
}

/**
 * Bottom navigation bar component
 */
@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    navController: NavHostController
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(item.title) },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            // Pop up to the start destination to avoid building up a large stack
                            popUpTo(BottomNavItem.Home.route) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
