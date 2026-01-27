package com.example.oneorder_sm.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    viewModel: OrderListViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToMenu: () -> Unit = {},
    onNavigateToTables: () -> Unit = {},
    onNavigateToStaff: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Tất cả đơn", "Chờ", "Đã xác nhận", "Đang nấu", "Đã phục vụ")

    // Refresh data when screen becomes visible
    DisposableEffect(Unit) {
        android.util.Log.d("OrderListScreen", "Screen visible - fetching orders")
        viewModel.fetchOrders()
        onDispose {
            android.util.Log.d("OrderListScreen", "Screen disposed")
        }
    }

    // Content only - no Scaffold/TopAppBar (handled by MainScreen)
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Lỗi: ${uiState.error}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.fetchOrders() }) {
                        Text("Thử lại")
                    }
                }
            }
        } else {
            val filteredOrders = when (selectedTabIndex) {
                0 -> uiState.orders.filter { it.status != OrderStatus.CANCELLED && it.status != OrderStatus.PAID }
                1 -> uiState.orders.filter { it.status == OrderStatus.PENDING }
                2 -> uiState.orders.filter { it.status == OrderStatus.CONFIRMED }
                3 -> uiState.orders.filter { it.status == OrderStatus.PREPARING }
                4 -> uiState.orders.filter { it.status == OrderStatus.SERVED }
                else -> uiState.orders
            }

            OrderList(
                orders = filteredOrders,
                onOrderClick = onNavigateToDetail
            )
        }
    }
}

@Composable
fun OrderList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No orders found")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orders) { order ->
                OrderCard(order = order, onClick = { onOrderClick(order.id) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bàn: ${order.tableName ?: "Chưa chọn"}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = order.status.name, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Total: $${order.totalAmount}", style = MaterialTheme.typography.bodyLarge)
            Text(text = "Created: ${order.createdAt}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
