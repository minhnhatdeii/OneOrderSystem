package com.example.oneorder.ui.screens.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder.data.model.OrderItemWithDetails
import com.example.oneorder.utils.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết đơn hàng") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                }
                uiState.order != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Order header
                        item {
                            OrderHeader(
                                orderId = uiState.order!!.id ?: "",
                                status = uiState.order!!.status,
                                paymentStatus = uiState.order!!.paymentStatus,
                                createdAt = uiState.order!!.createdAt,
                                totalAmount = uiState.order!!.totalAmount
                            )
                        }

                        // Items header
                        item {
                            Text(
                                "Món đã đặt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Order items
                        items(uiState.items) { item ->
                            OrderItemCard(item)
                        }

                        // Total
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Tổng cộng:",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    CurrencyFormatter.formatVND(uiState.order!!.totalAmount),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHeader(
    orderId: String,
    status: String,
    paymentStatus: String,
    createdAt: String?,
    totalAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Mã đơn: ${orderId.takeLast(8)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Trạng thái:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        getStatusText(status),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getDetailStatusColor(status)
                    )
                }

                Column {
                    Text("Thanh toán:", style = MaterialTheme.typography.bodySmall)
                    Text(
                        getPaymentStatusText(paymentStatus),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = getDetailPaymentStatusColor(paymentStatus)
                    )
                }
            }

            createdAt?.let {
                Text(
                    "Thời gian: ${formatDate(it)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(item: OrderItemWithDetails) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image
            AsyncImage(
                model = item.menuItemImage,
                contentDescription = item.menuItemName,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.menuItemName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    "Số lượng: ${item.orderItem.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                item.orderItem.note?.let { note ->
                    Text(
                        "Ghi chú: $note",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Text(
                    CurrencyFormatter.formatVND(item.orderItem.priceAtTime * item.orderItem.quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun getDetailStatusColor(status: String) = when (status.lowercase()) {
    "pending" -> MaterialTheme.colorScheme.tertiary
    "preparing" -> MaterialTheme.colorScheme.secondary
    "served" -> MaterialTheme.colorScheme.primary
    "paid" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun getDetailPaymentStatusColor(paymentStatus: String) = when (paymentStatus.lowercase()) {
    "paid" -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.error
}

fun getStatusText(status: String) = when (status.lowercase()) {
    "pending" -> "Chờ xác nhận"
    "preparing" -> "Đang chuẩn bị"
    "served" -> "Đã phục vụ"
    "paid" -> "Đã thanh toán"
    else -> status
}

fun getPaymentStatusText(paymentStatus: String) = when (paymentStatus.lowercase()) {
    "paid" -> "Đã thanh toán"
    "unpaid" -> "Chưa thanh toán"
    else -> paymentStatus
}

fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}
