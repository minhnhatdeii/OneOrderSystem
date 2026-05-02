package com.example.oneorder_sm.ui.screens.table

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Table

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToOrder: (String) -> Unit = {},
    viewModel: TableManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedTable by remember { mutableStateOf<Table?>(null) }

    // Auto-refresh when screen becomes visible
    DisposableEffect(Unit) {
        android.util.Log.d("TableManagement", "Screen visible - refreshing tables")
        viewModel.loadTables()
        onDispose {
            android.util.Log.d("TableManagement", "Screen disposed")
        }
    }

    // Show messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            viewModel.clearMessages()
        }
    }

    // Content only - no Scaffold/TopAppBar (handled by MainScreen)
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null && uiState.tables.isEmpty()) {
            Text(
                text = uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.tables.isEmpty()) {
            Text(
                text = "Chưa có bàn. Thêm bàn mới để bắt đầu!",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uiState.tables) { table ->
                    TableCard(
                        table = table,
                        onClick = {
                            selectedTable = table
                            if (table.status == "occupied") {
                                viewModel.loadOrderForTable(table.id!!)
                            }
                            showDetailDialog = true
                        }
                    )
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedTable = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm bàn")
        }

        // Add/Edit Dialog
        if (showAddEditDialog) {
            AddEditTableDialog(
                table = selectedTable,
                onDismiss = {
                    showAddEditDialog = false
                    selectedTable = null
                },
                onSave = { tableName, capacity, location ->
                    viewModel.saveTable(
                        id = selectedTable?.id,
                        tableName = tableName,
                        capacity = capacity,
                        location = location
                    )
                    showAddEditDialog = false
                    selectedTable = null
                }
            )
        }

        // Detail Bottom Sheet
        if (showDetailDialog && selectedTable != null) {
            TableDetailBottomSheet(
                table = selectedTable!!,
                activeOrder = uiState.selectedTableOrder,
                isLoadingOrder = uiState.isLoadingOrder,
                qrCode = uiState.generatedQRCode,
                onDismiss = {
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearQRCode()
                    viewModel.clearSelectedOrder()
                },
                onEdit = {
                    showDetailDialog = false
                    showAddEditDialog = true
                },
                onDelete = {
                    viewModel.deleteTable(selectedTable!!.id!!)
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearQRCode()
                    viewModel.clearSelectedOrder()
                },
                onGenerateQR = {
                    viewModel.generateQRCode(selectedTable!!.id!!)
                },
                onNavigateToOrder = { orderId ->
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearSelectedOrder()
                    onNavigateToOrder(orderId)
                },
                onCheckout = { orderId ->
                    viewModel.checkoutTable(selectedTable!!.id!!, orderId)
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearSelectedOrder()
                },
                onToggleStatus = {
                    val newStatus = if (selectedTable!!.status == "free") "occupied" else "free"
                    viewModel.updateStatus(selectedTable!!.id!!, newStatus)
                    // The bottom sheet receives the updated table from the uiState if we re-read selectedTable 
                    // To ensure immediate UI feedback, we can update the selectedTable manually or wait for the recomposition logic.
                    // Instead of full recomposition here, closing sheet is safer, or we just rely on Flow update.
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearSelectedOrder()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableCard(table: Table, onClick: () -> Unit) {
    val isOccupied = table.status == "occupied"
    val statusColor = if (isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val cardColor = if (isOccupied) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top: Table Name and Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Status indicator
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusColor,
                    modifier = Modifier.size(12.dp).padding(top = 4.dp)
                ) {}
            }

            // Bottom: Info (Capacity & Location)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Capacity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Sức chứa",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${table.capacity} khách",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Location
                if (!table.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Vị trí",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = table.location,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Status Text Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = if (isOccupied) "Có khách" else "Trống",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
