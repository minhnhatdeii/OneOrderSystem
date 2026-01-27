package com.example.oneorder_sm.ui.screens.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderItem
import com.example.oneorder_sm.data.model.OrderStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    viewModel: OrderDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                     horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Error: ${uiState.error}")
                    Button(onClick = { viewModel.fetchOrderDetails() }) {
                        Text("Retry")
                    }
                }
            } else {
                uiState.order?.let { order ->
                    OrderDetailContent(
                        order = order,
                        onUpdateStatus = { viewModel.updateStatus(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailContent(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Text("Order ID: ${order.id}", style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Table: ${order.tableId ?: "N/A"}", style = MaterialTheme.typography.titleLarge)
        Text("Status: ${order.status.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Items:", style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(order.orderItems) { item ->
                OrderItemRow(item)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Total: $${order.totalAmount}", style = MaterialTheme.typography.headlineSmall)
        
        Spacer(modifier = Modifier.height(16.dp))
        StatusActionButtons(currentStatus = order.status, onUpdateStatus = onUpdateStatus)
    }
}

@Composable
fun OrderItemRow(item: OrderItem) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
             Text("Item ID: ${item.menuItemId}", style = MaterialTheme.typography.bodyLarge) // Replace with name if joined lookup available
             if (!item.note.isNullOrBlank()) {
                 Text("Note: ${item.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
             }
        }
        
        Row {
            Text("x${item.quantity}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Text("$${item.priceAtTime * item.quantity}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun StatusActionButtons(
    currentStatus: OrderStatus,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (currentStatus) {
            OrderStatus.PENDING -> {
                Button(onClick = { onUpdateStatus(OrderStatus.CONFIRMED) }, modifier = Modifier.weight(1f)) {
                    Text("Confirm")
                }
                OutlinedButton(onClick = { onUpdateStatus(OrderStatus.CANCELLED) }, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
            }
            OrderStatus.CONFIRMED -> {
                Button(onClick = { onUpdateStatus(OrderStatus.PREPARING) }, modifier = Modifier.weight(1f)) {
                    Text("Start Preparing")
                }
            }
            OrderStatus.PREPARING -> {
                 Button(onClick = { onUpdateStatus(OrderStatus.SERVED) }, modifier = Modifier.weight(1f)) {
                    Text("Serve")
                }
            }
            OrderStatus.SERVED -> {
                 Button(onClick = { onUpdateStatus(OrderStatus.PAID) }, modifier = Modifier.weight(1f)) {
                    Text("Mark Paid")
                }
            }
             else -> {
                 // No actions for Cancelled or Paid usually
                 if(currentStatus != OrderStatus.CANCELLED && currentStatus != OrderStatus.PAID) {
                      Button(onClick = { /* Handle logic */ }) { Text("Update") }
                 }
             }
        }
    }
}
