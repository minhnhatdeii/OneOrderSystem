package com.example.oneorder_sm.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.ui.components.AppSidebar
import com.example.oneorder_sm.ui.screens.dashboard.DashboardScreen
import com.example.oneorder_sm.ui.screens.dashboard.IncomeHistoryScreen
import com.example.oneorder_sm.ui.screens.order.OrderDetailScreen
import com.example.oneorder_sm.ui.screens.menu.MenuManagementScreen
import com.example.oneorder_sm.ui.screens.orders.OrderListScreen
import com.example.oneorder_sm.ui.screens.foodpromotion.FoodPromotionViewModel
import com.example.oneorder_sm.ui.screens.staff.StaffManagementScreen
import com.example.oneorder_sm.ui.screens.table.TableManagementScreen
import com.example.oneorder_sm.ui.screens.profile.ProfileScreen
import com.example.oneorder_sm.ui.screens.foodpromotion.FoodPromotionScreen
import com.example.oneorder_sm.ui.screens.foodpromotion.RestaurantFeedScreen
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
    val foodPromotionViewModel: FoodPromotionViewModel = hiltViewModel()

    var showExitDialog by remember { mutableStateOf(false) }
    var showRestaurantMenu by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    val defaultScreen = if (uiState.userRole == "manager") "dashboard" else "orders"
    androidx.activity.compose.BackHandler(enabled = drawerState.isClosed) {
        if (uiState.selectedScreen != defaultScreen) {
            viewModel.selectScreen(defaultScreen)
        } else {
            showExitDialog = true
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Thoát ứng dụng", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn thoát ứng dụng không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Thoát")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

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
                if (uiState.selectedScreen != "edit_restaurant_profile" && 
                    uiState.selectedScreen != "income_history" && 
                    uiState.selectedScreen != "create_food_post" && 
                    uiState.selectedScreen != "restaurant_feed" &&
                    !uiState.selectedScreen.startsWith("post_detail/")
                ) {
                    TopAppBar(
                        title = { 
                            Text(
                                text = when (uiState.selectedScreen) {
                                    "dashboard" -> "Tổng quan"
                                    "orders" -> "Đơn hàng"
                                    "tables" -> "Quản lý bàn"
                                    "menu" -> "Quản lý Menu"
                                    "food_promotion" -> "Nhà hàng"
                                    "staff" -> "Quản lý nhân viên"
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
                        actions = {
                            if (uiState.selectedScreen == "food_promotion") {
                                Box {
                                    IconButton(onClick = { showRestaurantMenu = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "Thêm tùy chọn",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showRestaurantMenu,
                                        onDismissRequest = { showRestaurantMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Chỉnh sửa thông tin nhà hàng") },
                                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                                            onClick = {
                                                showRestaurantMenu = false
                                                viewModel.selectScreen("edit_restaurant_profile")
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Thêm nhân viên mới") },
                                            leadingIcon = { Icon(Icons.Default.PersonAdd, null) },
                                            onClick = {
                                                showRestaurantMenu = false
                                                foodPromotionViewModel.showAddStaffDialog()
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        ) { paddingValues ->
            val isEdgeToEdgeScreen = uiState.selectedScreen == "restaurant_feed"
            Box(
                modifier = Modifier.padding(
                    top = if (isEdgeToEdgeScreen) 0.dp else paddingValues.calculateTopPadding(),
                    bottom = if (isEdgeToEdgeScreen) 0.dp else paddingValues.calculateBottomPadding(),
                    start = paddingValues.calculateStartPadding(LayoutDirection.Ltr),
                    end = paddingValues.calculateEndPadding(LayoutDirection.Ltr)
                )
            ) {
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
                    uiState.selectedScreen == "dashboard" -> DashboardScreen(
                        onNavigateToOrders = { viewModel.selectScreen("orders") },
                        onNavigateToOrderDetail = { orderId -> viewModel.selectScreen("order_detail/$orderId") },
                        onNavigateToIncomeHistory = { viewModel.selectScreen("income_history") }
                    )
                    uiState.selectedScreen == "income_history" -> IncomeHistoryScreen(
                        onNavigateBack = { viewModel.selectScreen("dashboard") },
                        onNavigateToOrderDetail = { orderId -> viewModel.selectScreen("order_detail/$orderId") }
                    )
                    uiState.selectedScreen == "orders" -> {
                        OrderListScreen(
                            onNavigateToDetail = { orderId ->
                                viewModel.selectScreen("order_detail/$orderId")
                            }
                        )
                    }
                    uiState.selectedScreen == "tables" -> TableManagementScreenContent(
                        onNavigateToOrder = { orderId -> viewModel.selectScreen("order_detail/$orderId") }
                    )
                    uiState.selectedScreen == "menu" -> MenuManagementScreenContent()
                    uiState.selectedScreen == "food_promotion" -> FoodPromotionScreen(
                        viewModel = foodPromotionViewModel,
                        onNavigateToEditProfile = { viewModel.selectScreen("edit_restaurant_profile") },
                        onNavigateToCreatePost = { viewModel.selectScreen("create_food_post") },
                        onNavigateToPostDetail = { postId -> viewModel.selectScreen("post_detail/$postId") },
                        onNavigateToFeedView = { viewModel.selectScreen("restaurant_feed") }
                    )
                    uiState.selectedScreen == "create_food_post" -> com.example.oneorder_sm.ui.screens.foodpromotion.CreateFoodPostScreen(
                        onNavigateBack = {
                            foodPromotionViewModel.refreshPosts()
                            viewModel.selectScreen("food_promotion")
                        }
                    )
                    uiState.selectedScreen == "edit_restaurant_profile" -> com.example.oneorder_sm.ui.screens.foodpromotion.EditRestaurantProfileScreen(
                        viewModel = foodPromotionViewModel,
                        onNavigateBack = { viewModel.selectScreen("food_promotion") }
                    )
                    uiState.selectedScreen.startsWith("post_detail/") -> {
                        val postId = uiState.selectedScreen.removePrefix("post_detail/")
                        com.example.oneorder_sm.ui.screens.foodpromotion.FoodPromotionDetailScreen(
                            postId = postId,
                            viewModel = foodPromotionViewModel,
                            onNavigateBack = { viewModel.selectScreen("food_promotion") }
                        )
                    }
                    uiState.selectedScreen == "restaurant_feed" -> RestaurantFeedScreen(
                        viewModel = foodPromotionViewModel,
                        onNavigateBack = { viewModel.selectScreen("food_promotion") },
                        onNavigateToPostDetail = { postId -> viewModel.selectScreen("post_detail/$postId") }
                    )
                    uiState.selectedScreen == "staff" -> StaffManagementScreenContent()
                    uiState.selectedScreen == "profile" -> ProfileScreen()
                    else -> if (uiState.userRole == "manager") DashboardScreen(
                        onNavigateToOrders = { viewModel.selectScreen("orders") },
                        onNavigateToOrderDetail = { orderId -> viewModel.selectScreen("order_detail/$orderId") },
                        onNavigateToIncomeHistory = { viewModel.selectScreen("income_history") }
                    ) else {
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
private fun TableManagementScreenContent(onNavigateToOrder: (String) -> Unit) {
    TableManagementScreen(
        onNavigateBack = { },
        onNavigateToOrder = onNavigateToOrder
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
