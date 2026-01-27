package com.example.oneorder_sm.ui.screens.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus

@Composable
fun OrderDetailScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle back press
    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }
    
    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }
    
    // Clear messages after showing
    LaunchedEffect(uiState.successMessage, uiState.error) {
        if (uiState.successMessage != null || uiState.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success/Error messages
        uiState.successMessage?.let { message ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        uiState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        uiState.order?.let { order ->
            OrderDetailContent(
                order = order,
                onStatusUpdate = { newStatus ->
                    viewModel.updateStatus(orderId, newStatus)
                }
            )
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: Order,
    onStatusUpdate: (OrderStatus) -> Unit
) {
    // Order Header Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bàn: ${order.tableName ?: "Chưa chọn"}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đơn hàng #${order.id.take(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                StatusBadge(status = order.status)
            }
            
            Divider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Tổng tiền:", fontWeight = FontWeight.Bold)
                Text(
                    "${String.format("%,.0f", order.totalAmount)} VNĐ",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Customer Note (if exists)
    order.note?.let { note ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.Note,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Ghi chú của khách",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(note, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    
    // Order Items Card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Chi tiết món",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            order.orderItems.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.itemName ?: "Món #${item.menuItemId}",
                            fontWeight = FontWeight.Medium
                        )
                        item.note?.let { itemNote ->
                            Text(
                                itemNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text("x${item.quantity}")
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "${String.format("%,.0f", item.priceAtTime)} VN Đ",
                        fontWeight = FontWeight.Medium
                    )
                }
                if (item != order.orderItems.last()) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
    
    // Status Action Button
    StatusActionButton(
        currentStatus = order.status,
        onStatusUpdate = onStatusUpdate
    )
}

@Composable
private fun StatusBadge(status: OrderStatus) {
    val (color, text, icon) = when (status) {
        OrderStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "Chờ xử lý",
            Icons.Default.Schedule
        )
        OrderStatus.CONFIRMED -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            "Đã xác nhận",
            Icons.Default.CheckCircle
        )
        OrderStatus.PREPARING -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "Đang nấu",
            Icons.Default.Restaurant
        )
        OrderStatus.SERVED -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "Đã phục vụ",
            Icons.Default.Done
        )
        OrderStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Đã hủy",
            Icons.Default.Cancel
        )
        OrderStatus.PAID -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "Hoàn tất",
            Icons.Default.CheckCircle
        )
    }
    
    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusActionButton(
    currentStatus: OrderStatus,
    onStatusUpdate: (OrderStatus) -> Unit
) {
    val statusAction = when (currentStatus) {
        OrderStatus.PENDING -> Triple(
            OrderStatus.PREPARING,
            "Bắt đầu nấu",
            Icons.Default.Restaurant
        )
        OrderStatus.PREPARING -> Triple(
            OrderStatus.SERVED,
            "Đã phục vụ",
            Icons.Default.Done
        )
        OrderStatus.SERVED -> Triple(
            OrderStatus.PAID,
            "Hoàn tất đơn",
            Icons.Default.CheckCircle
        )
        else -> null
    }
    
    if (statusAction != null) {
        val (nextStatus, buttonText, buttonIcon) = statusAction
        Button(
            onClick = { onStatusUpdate(nextStatus) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(buttonIcon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(buttonText, style = MaterialTheme.typography.titleMedium)
        }
    } else if (currentStatus == OrderStatus.PAID) {
        // Show completion badge
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Đơn hàng đã hoàn tất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
