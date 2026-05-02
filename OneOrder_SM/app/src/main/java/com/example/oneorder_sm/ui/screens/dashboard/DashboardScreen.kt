package com.example.oneorder_sm.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToOrders: () -> Unit = {},
    onNavigateToOrderDetail: (String) -> Unit = {},
    onNavigateToIncomeHistory: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    // Refresh data when screen becomes visible
    DisposableEffect(Unit) {
        viewModel.refreshData()
        onDispose { }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Greeting
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Xin chào, Quản lý",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đây là tình hình hôm nay.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Loading State
        if (uiState.isLoading && uiState.summary == null) {
            item {
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

        // Error State
        if (uiState.error != null && uiState.summary == null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

        // Main Content
        if (uiState.summary != null || !uiState.isLoading) {
            val summary = uiState.summary ?: com.example.oneorder_sm.data.model.DashboardSummary()

            // Hero Revenue Card
            item {
                HeroRevenueCard(
                    revenue = summary.todayRevenue,
                    currencyFormat = currencyFormat,
                    onDetailClick = onNavigateToIncomeHistory
                )
            }

            // Two Stat Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniStatCard(
                        title = "SỐ ĐƠN",
                        value = "${summary.activeOrders}",
                        icon = Icons.Default.Receipt,
                        iconTint = Color(0xFF228BE2),
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatCard(
                        title = "BÀN",
                        value = "${summary.occupiedTables}/${summary.totalTables}",
                        icon = Icons.Default.TableBar,
                        iconTint = Color(0xFF228BE2),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent Orders Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đơn hàng gần đây",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = onNavigateToOrders) {
                        Text(
                            text = "Xem tất cả",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Recent Orders List
            if (uiState.recentOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có đơn hàng nào.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(uiState.recentOrders) { order ->
                RecentOrderCard(
                    order = order,
                    currencyFormat = currencyFormat,
                    onClick = { onNavigateToOrderDetail(order.id) }
                )
            }
        }
    }
}

@Composable
private fun HeroRevenueCard(
    revenue: Double,
    currencyFormat: NumberFormat,
    onDetailClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF228BE2),
                            Color(0xFF1565C0)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DOANH THU HÔM NAY",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                    // Detail button
                    IconButton(
                        onClick = onDetailClick,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.OpenInNew,
                            contentDescription = "Xem chi tiết",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = currencyFormat.format(revenue),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RecentOrderCard(
    order: Order,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val (statusText, statusColor, statusBgColor) = getOrderStatusInfo(order.status)
    val timeStr = formatOrderTime(order.createdAt)
    val orderNumber = order.id.takeLast(4).uppercase()
    val locationText = order.tableName ?: "Mang đi"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order number badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#$orderNumber",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Order info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = statusBgColor,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private data class StatusInfo(
    val text: String,
    val color: Color,
    val backgroundColor: Color
)

private fun getOrderStatusInfo(status: OrderStatus): StatusInfo {
    return when (status) {
        OrderStatus.PENDING -> StatusInfo(
            text = "Chờ xác nhận",
            color = Color(0xFFB8860B),
            backgroundColor = Color(0xFFFFF8E1)
        )
        OrderStatus.CONFIRMED -> StatusInfo(
            text = "Đã xác nhận",
            color = Color(0xFF1565C0),
            backgroundColor = Color(0xFFE3F2FD)
        )
        OrderStatus.PREPARING -> StatusInfo(
            text = "Đang chế biến",
            color = Color(0xFFE65100),
            backgroundColor = Color(0xFFFFF3E0)
        )
        OrderStatus.SERVED -> StatusInfo(
            text = "Đã phục vụ",
            color = Color(0xFF2E7D32),
            backgroundColor = Color(0xFFE8F5E9)
        )
        OrderStatus.CANCELLED -> StatusInfo(
            text = "Đã hủy",
            color = Color(0xFFC62828),
            backgroundColor = Color(0xFFFFEBEE)
        )
        OrderStatus.PAID -> StatusInfo(
            text = "Đã thanh toán",
            color = Color(0xFF1B5E20),
            backgroundColor = Color(0xFFE0F2F1)
        )
    }
}

private fun formatOrderTime(createdAt: String): String {
    return try {
        // Parse the string as OffsetDateTime, then convert to local timezone
        val offsetDateTime = java.time.OffsetDateTime.parse(createdAt)
        val localDateTime = offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm\ndd/MM/yyyy")
        localDateTime.format(formatter)
    } catch (e: Exception) {
        try {
            // Fallback if the string lacks timezone info
            val cleanDate = createdAt.replace(" ", "T")
            val localDateTime = java.time.LocalDateTime.parse(cleanDate)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm\ndd/MM/yyyy")
            localDateTime.format(formatter)
        } catch (e2: Exception) {
            createdAt // Return raw if both parsing attempts fail
        }
    }
}
