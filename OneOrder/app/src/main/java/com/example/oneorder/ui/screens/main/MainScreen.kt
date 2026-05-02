package com.example.oneorder.ui.screens.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.oneorder.R
import com.example.oneorder.ui.screens.cart.CartScreen
import com.example.oneorder.ui.screens.home.HomeScreen
import com.example.oneorder.ui.screens.order.OrderHistoryScreen

/**
 * Bottom navigation destinations
 */
sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        titleResId = R.string.nav_menu,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Cart : BottomNavItem(
        route = "cart",
        titleResId = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    )

    data object Orders : BottomNavItem(
        route = "orders",
        titleResId = R.string.nav_orders,
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )
}

/**
 * Main screen with bottom navigation — 3 tabs only (Profile removed)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: com.example.oneorder.ui.screens.home.HomeViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMenu: (Long) -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateToCheckout: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit
) {
    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Cart,
        BottomNavItem.Orders,
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
                    onNavigateBack = onNavigateBack,
                    onNavigateToMenu = onNavigateToMenu,
                    onNavigateToCart = { navController.navigate(BottomNavItem.Cart.route) },
                    onNavigateToProfile = { /* Profile is in FoodFeed */ },
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
                    onNavigateToDetail = { orderId -> onNavigateToOrderDetail(orderId) }
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
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            val title = stringResource(item.titleResId)

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = title
                    )
                },
                label = { Text(title) },
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(BottomNavItem.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
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
