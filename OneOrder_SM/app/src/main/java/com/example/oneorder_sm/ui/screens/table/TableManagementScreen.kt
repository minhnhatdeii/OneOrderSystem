package com.example.oneorder_sm.ui.screens.table

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Table

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableManagementScreen(
    onNavigateBack: () -> Unit = {},
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.tables) { table ->
                    TableCard(
                        table = table,
                        onToggleStatus = {
                            val newStatus = if (table.status == "free") "occupied" else "free"
                            viewModel.updateStatus(table.id!!, newStatus)
                        },
                        onLongClick = {
                            selectedTable = table
                            showDetailDialog = true
                            viewModel.generateQRCode(table.id!!)
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

        // Detail Dialog with QR Code
        if (showDetailDialog && selectedTable != null) {
            TableDetailDialog(
                table = selectedTable!!,
                qrCode = uiState.generatedQRCode,
                onDismiss = {
                    showDetailDialog = false
                    selectedTable = null
                    viewModel.clearQRCode()
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
                },
                onGenerateQR = {
                    viewModel.generateQRCode(selectedTable!!.id!!)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TableCard(table: Table, onToggleStatus: () -> Unit, onLongClick: () -> Unit) {
    val isOccupied = table.status == "occupied"
    val statusColor = if (isOccupied) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val cardColor = if (isOccupied) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggleStatus,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side: Table info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Table name
                Text(
                    text = table.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Capacity and location row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Capacity
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "👥",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${table.capacity} chỗ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Location (if available)
                    if (!table.location.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = table.location,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Right side: Status badge
            Surface(
                shape = MaterialTheme.shapes.small,
                color = statusColor,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = if (isOccupied) "BẬN" else "TRỐNG",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
