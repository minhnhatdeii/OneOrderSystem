package com.example.oneorder_sm.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.ui.components.AppSidebar
import com.example.oneorder_sm.ui.screens.dashboard.DashboardScreen
import com.example.oneorder_sm.ui.screens.order.OrderDetailScreen
import com.example.oneorder_sm.ui.screens.menu.MenuManagementScreen
import com.example.oneorder_sm.ui.screens.orders.OrderListScreen
import com.example.oneorder_sm.ui.screens.settings.RestaurantSettingsScreen
import com.example.oneorder_sm.ui.screens.staff.StaffManagementScreen
import com.example.oneorder_sm.ui.screens.table.TableManagementScreen
import com.example.oneorder_sm.ui.screens.profile.ProfileScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                restaurantName = uiState.restaurantName,
                userRole = uiState.userRole,
                selectedItemId = uiState.selectedScreen,
                onItemSelected = { screenId ->
                    viewModel.selectScreen(screenId)
                    scope.launch { drawerState.close() }
                },
                onLogout = {
                    scope.launch { 
                        drawerState.close()
                        viewModel.logout()
                        onLogout()
                    }
                }
            )
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = when (uiState.selectedScreen) {
                                "dashboard" -> "Tổng quan"
                                "orders" -> "Đơn hàng"
                                "tables" -> "Quản lý bàn"
                                "menu" -> "Quản lý Menu"
                                "staff" -> "Nhân viên"
                                "settings" -> "Cài đặt nhà hàng"
                                "profile" -> "Hồ sơ cá nhân"
                                else -> if (uiState.selectedScreen.startsWith("order_detail/")) "Chi tiết đơn hàng" else "OneOrder"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when {
                    uiState.selectedScreen.startsWith("order_detail/") -> {
                        val orderId = uiState.selectedScreen.removePrefix("order_detail/")
                        com.example.oneorder_sm.ui.screens.order.OrderDetailScreen(
                            orderId = orderId,
                            onNavigateBack = { 
                                viewModel.selectScreen("orders")
                            }
                        )
                    }
                    uiState.selectedScreen == "dashboard" -> DashboardScreen()
                    uiState.selectedScreen == "orders" -> {
                        // Create a key to force recomposition when coming back from detail
                        key(uiState.selectedScreen) {
                            OrderListScreen(
                                onNavigateToDetail = { orderId ->
                                    viewModel.selectScreen("order_detail/$orderId")
                                }
                            )
                        }
                    }
                    uiState.selectedScreen == "tables" -> TableManagementScreenContent()
                    uiState.selectedScreen == "menu" -> MenuManagementScreenContent()
                    uiState.selectedScreen == "staff" -> StaffManagementScreenContent()
                    uiState.selectedScreen == "settings" -> RestaurantSettingsScreen(
                        onNavigateBack = { viewModel.selectScreen("orders") }
                    )
                    uiState.selectedScreen == "profile" -> ProfileScreen()
                    else -> if (uiState.userRole == "manager") DashboardScreen() else {
                        key(uiState.selectedScreen) {
                            OrderListScreen(
                                onNavigateToDetail = { orderId ->
                                    viewModel.selectScreen("order_detail/$orderId")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun OrderListScreenContent() {
    // Simplified version without navigation callbacks since we're using sidebar
    OrderListScreen(
        onNavigateToDetail = { /* TODO: Show order detail dialog */ },
        onNavigateToMenu = { },
        onNavigateToTables = { },
        onNavigateToStaff = { },
        onNavigateToSettings = { }
    )
}

@Composable
private fun TableManagementScreenContent() {
    TableManagementScreen(
        onNavigateBack = { }
    )
}

@Composable
private fun MenuManagementScreenContent() {
    MenuManagementScreen(
        onNavigateBack = { }
    )
}

@Composable
private fun StaffManagementScreenContent() {
    StaffManagementScreen(
        onNavigateBack = { }
    )
}
