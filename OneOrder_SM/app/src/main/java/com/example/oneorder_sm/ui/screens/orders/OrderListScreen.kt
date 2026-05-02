package com.example.oneorder_sm.ui.screens.orders

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val selectedTabIndex = uiState.selectedTabIndex
    var showDatePicker by remember { mutableStateOf(false) }
    val tabs = listOf("Tất cả đơn", "Chờ", "Đang nấu", "Đã phục vụ", "Đã hủy", "Đã hoàn tất")


    // Content only - no Scaffold/TopAppBar (handled by MainScreen)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.weight(1f),
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { viewModel.setSelectedTab(index) },
                        text = { Text(title) }
                    )
                }
            }
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Lọc theo ngày",
                    tint = if (uiState.filterDateMillis != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.filterDateMillis)
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateFilterDate(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    }) {
                        Text("Lọc")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.updateFilterDate(null)
                        showDatePicker = false
                    }) {
                        Text("Bỏ lọc")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
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
            var filteredOrders = when (selectedTabIndex) {
                0 -> uiState.orders
                1 -> uiState.orders.filter { it.status == OrderStatus.PENDING }
                2 -> uiState.orders.filter { it.status == OrderStatus.PREPARING }
                3 -> uiState.orders.filter { it.status == OrderStatus.SERVED }
                4 -> uiState.orders.filter { it.status == OrderStatus.CANCELLED }
                5 -> uiState.orders.filter { it.status == OrderStatus.PAID }
                else -> uiState.orders
            }

            if (uiState.filterDateMillis != null) {
                // DatePicker returns UTC millis.
                val filterLocalDate = Instant.ofEpochMilli(uiState.filterDateMillis!!).atZone(ZoneId.of("UTC")).toLocalDate()
                filteredOrders = filteredOrders.filter { order ->
                    try {
                        val orderLocalDate = Instant.parse(order.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
                        orderLocalDate == filterLocalDate
                    } catch (e: Exception) {
                        false
                    }
                }
            }

            OrderList(
                orders = filteredOrders,
                onOrderClick = onNavigateToDetail
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OrderList(
    orders: List<Order>,
    onOrderClick: (String) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Không có đơn hàng nào",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val groupedOrders = remember(orders) {
            orders.groupBy { getOrderDateString(it.createdAt) }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedOrders.forEach { (dateStr, ordersForDate) ->
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
                        Text(
                            text = "Ngày: $dateStr",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    }
                }
                items(ordersForDate, key = { it.id }) { order ->
                    OrderCard(order = order, onClick = { onOrderClick(order.id) })
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (color, text) = when (status) {
        OrderStatus.PENDING -> Color(0xFFF57C00) to "Chờ" // Cam
        OrderStatus.CONFIRMED -> Color(0xFF1976D2) to "Đã xác nhận" // Xanh dương
        OrderStatus.PREPARING -> Color(0xFF7B1FA2) to "Đang nấu" // Tím
        OrderStatus.SERVED -> Color(0xFF388E3C) to "Đã phục vụ" // Xanh lá
        OrderStatus.CANCELLED -> Color(0xFFD32F2F) to "Đã hủy" // Đỏ
        OrderStatus.PAID -> Color(0xFF2E7D32) to "Hoàn tất" // Xanh đậm
    }
    Surface(
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
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
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bàn: ${order.tableName ?: "Chưa chọn (Mang đi)"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                OrderStatusChip(status = order.status)
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // Format Total
            val formattedTotal = try {
                val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
                format.format(order.totalAmount).replace("₫", "đ").trim()
            } catch (e: Exception) {
                "${order.totalAmount} đ"
            }

            Text(
                text = "Tổng cộng: $formattedTotal", 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tạo lúc: ${parseCreatedAt(order.createdAt)}", 
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

fun parseCreatedAt(dateString: String): String {
    return try {
        val instant = try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            // Xử lý các format như "2024-05-12 10:30:00" hoặc không có timezone
            val cleanString = dateString.replace(" ", "T").substringBefore("+").substringBefore("Z")
            java.time.LocalDateTime.parse(cleanString).atZone(ZoneId.of("UTC")).toInstant()
        }
        val formatter = DateTimeFormatter.ofPattern("hh:mm a 'ngày' dd - MM - yyyy", Locale.US)
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

fun getOrderDateString(dateString: String): String {
    return try {
        val instant = try {
            Instant.parse(dateString)
        } catch (e: Exception) {
            val cleanString = dateString.replace(" ", "T").substringBefore("+").substringBefore("Z")
            java.time.LocalDateTime.parse(cleanString).atZone(ZoneId.of("UTC")).toInstant()
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        instant.atZone(ZoneId.systemDefault()).format(formatter)
    } catch (e: Exception) {
        "Không xác định"
    }
}
