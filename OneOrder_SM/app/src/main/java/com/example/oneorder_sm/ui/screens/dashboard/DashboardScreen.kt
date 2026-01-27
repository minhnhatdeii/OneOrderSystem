package com.example.oneorder_sm.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.ui.components.StatCard
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }
    
    // Refresh data when screen becomes visible
    androidx.compose.runtime.DisposableEffect(Unit) {
        viewModel.refreshData()
        onDispose { }
    }

    Scaffold { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with refresh button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tổng quan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Làm mới",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            // Pull to refresh indicator
            item {
                if (uiState.isLoading && uiState.summary == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Error state
            if (uiState.error != null && uiState.summary == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { viewModel.refreshData() }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }

            // Summary Cards - Revenue on top (full width), then 3 stats below
            item {
                val summary = uiState.summary ?: com.example.oneorder_sm.data.model.DashboardSummary()
                
                // Calculate total revenue from orderStats based on selected period
                val totalRevenue = uiState.orderStats.sumOf { it.totalRevenue }
                val revenueTitle = when (uiState.selectedDateRange) {
                    DateRange.TODAY -> "Doanh thu hôm nay"
                    DateRange.LAST_7_DAYS -> "Doanh thu 7 ngày"
                    DateRange.LAST_30_DAYS -> "Doanh thu 30 ngày"
                }
                
                // Fixed logic: Only use todayRevenue fallback when selected period is TODAY
                val revenueValue = if (uiState.orderStats.isNotEmpty()) {
                    // Have stats data - use calculated total
                    currencyFormat.format(totalRevenue)
                } else {
                    // No stats data - show 0 OR use today revenue ONLY if TODAY is selected
                    if (uiState.selectedDateRange == DateRange.TODAY && uiState.summary != null) {
                        currencyFormat.format(summary.todayRevenue)
                    } else {
                        currencyFormat.format(0)  // No data = 0 revenue
                    }
                }
                
                // Debug logging
                android.util.Log.d("Dashboard", """
                    selectedDateRange: ${uiState.selectedDateRange}
                    orderStats.size: ${uiState.orderStats.size}
                    orderStats.sum: $totalRevenue
                    summary.todayRevenue: ${summary.todayRevenue}
                    finalRevenueValue: $revenueValue
                """.trimIndent())
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Revenue Card - Full Width
                    StatCard(
                        title = revenueTitle,
                        value = revenueValue,
                        icon = Icons.Default.AttachMoney,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    // Calculate total orders from statistics (matching the chart)
                    val totalOrdersInPeriod = uiState.orderStats.sumOf { it.totalOrders }
                    
                    // Other Stats - 3 columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Đơn hàng",
                            value = "$totalOrdersInPeriod",
                            icon = Icons.Default.Receipt,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Bàn",
                            value = "${summary.occupiedTables}/${summary.totalTables}",
                            icon = Icons.Default.TableBar,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Nhân viên",
                            value = "${summary.totalStaff}",
                            icon = Icons.Default.People,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Date Range Filter
            item {
                DateRangeSelector(
                    selectedRange = uiState.selectedDateRange,
                    onRangeSelected = { viewModel.selectDateRange(it) }
                )
            }

            // Order Statistics Section
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Doanh thu & Đơn hàng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (uiState.orderStats.isNotEmpty()) {
                            Text(
                                text = "Doanh Thu",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            RevenueChart(
                                orderStats = uiState.orderStats,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Text(
                                text = "Số lượng đơn",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            OrderCountChart(
                                orderStats = uiState.orderStats,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            val totalOrders = uiState.orderStats.sumOf { it.totalOrders }
                            val totalRevenue = uiState.orderStats.sumOf { it.totalRevenue }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatsRow("Tổng doanh thu (trong kỳ)", currencyFormat.format(totalRevenue))
                                StatsRow("Tổng đơn hàng (trong kỳ)", "$totalOrders")
                            }
                        } else {
                            // Empty State
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Chưa có dữ liệu thống kê.\nThử chọn khoảng thời gian khác hoặc tạo đơn hàng mới.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Popular Items Section
            if (uiState.popularItems.isNotEmpty()) {
                item {
                    Card {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Món bán chạy",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                items(uiState.popularItems.take(5)) { item ->
                    PopularItemCard(
                        item = item,
                        currencyFormat = currencyFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeSelector(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRange.entries.forEach { range ->
            FilterChip(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        text = when (range) {
                            DateRange.TODAY -> "Hôm nay"
                            DateRange.LAST_7_DAYS -> "7 ngày"
                            DateRange.LAST_30_DAYS -> "30 ngày"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PopularItemCard(
    item: com.example.oneorder_sm.data.model.PopularItem,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${item.totalQuantity} phần",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = currencyFormat.format(item.totalRevenue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
