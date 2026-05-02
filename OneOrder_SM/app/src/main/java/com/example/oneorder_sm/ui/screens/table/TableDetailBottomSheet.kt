package com.example.oneorder_sm.ui.screens.table

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.Table
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailBottomSheet(
    table: Table,
    activeOrder: Order?,
    isLoadingOrder: Boolean,
    qrCode: Bitmap?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGenerateQR: () -> Unit,
    onNavigateToOrder: (String) -> Unit,
    onCheckout: (String) -> Unit,
    onToggleStatus: () -> Unit
) {
    var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header: Table Name and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bàn ${table.name}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sức chứa: ${table.capacity} khách ${if (!table.location.isNullOrBlank()) "• ${table.location}" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge acting as toggle switch
                    val isOccupied = table.status == "occupied"
                    val containerColor = if (isOccupied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    val contentColor = if (isOccupied) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = containerColor,
                        contentColor = contentColor,
                        onClick = onToggleStatus
                    ) {
                        Text(
                            text = if (isOccupied) "Có khách" else "Trống",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sửa bàn") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tạo mã QR bàn") },
                                onClick = { showMenu = false; onGenerateQR() },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bàn", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Divider()

            // Active Order Section
            if (table.status == "occupied") {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Danh sách gọi món",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Món đang phục vụ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (activeOrder != null) {
                            val statusText = when(activeOrder.status.name.lowercase()) {
                                "pending" -> "Chờ xác nhận"
                                "confirmed" -> "Đã xác nhận"
                                "preparing" -> "Đang chuẩn bị"
                                "served" -> "Đã phục vụ"
                                "cancelled" -> "Đã hủy"
                                "paid" -> "Đã thanh toán"
                                else -> activeOrder.status.name
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isLoadingOrder) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (activeOrder == null || activeOrder.orderItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có món nào được gọi.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Display Ordered Items
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val localeVN = Locale("vi", "VN")
                                val currencyFormatter = NumberFormat.getCurrencyInstance(localeVN)
                                
                                val displayItems = activeOrder.orderItems
                                val calculatedTotal = displayItems.filter { 
                                    it.status?.lowercase() != "cancelled" && it.status?.lowercase() != "đã hủy" 
                                }.sumOf { it.priceAtTime * it.quantity }

                                displayItems.forEachIndexed { index, item ->
                                    val isCancelled = item.status?.lowercase() == "cancelled" || item.status?.lowercase() == "đã hủy"
                                    val alpha = if (isCancelled) 0.5f else 1f

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${item.quantity}x",
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                                                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                )
                                                Text(
                                                    text = item.itemName ?: "Món không xác định",
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                )
                                                if (isCancelled) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.errorContainer
                                                    ) {
                                                        Text(
                                                            text = "Đã hủy",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (!item.status.isNullOrBlank()) {
                                                    val itemStatusText = when (item.status.lowercase()) {
                                                        "pending" -> "Chờ"
                                                        "preparing" -> "Đang làm"
                                                        "served" -> "Đã ra"
                                                        else -> item.status
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                                    ) {
                                                        Text(
                                                            text = itemStatusText,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            if (!item.note.isNullOrBlank()) {
                                                Text(
                                                    text = "Ghi chú: ${item.note}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                                                    modifier = Modifier.padding(start = 24.dp, top = 2.dp),
                                                    textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                                )
                                            }
                                        }
                                        Text(
                                            text = currencyFormatter.format(item.priceAtTime * item.quantity),
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                                            textDecoration = if (isCancelled) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                        )
                                    }
                                    if (index < displayItems.lastIndex) {
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(thickness = 2.dp)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Tổng tiền",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = currencyFormatter.format(calculatedTotal),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                Divider()
            }

            // QR Code Section
            if (qrCode != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mã QR Gọi Món",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = qrCode.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            if (table.status == "occupied" && activeOrder != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToOrder(activeOrder.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Xem đơn")
                    }
                    
                    Button(
                        onClick = { onCheckout(activeOrder.id) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Thanh toán")
                    }
                }
            }
        }
    }
}
