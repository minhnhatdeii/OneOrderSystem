package com.example.oneorder_sm.ui.screens.table

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.data.model.Table

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTableDialog(
    table: Table? = null,
    onDismiss: () -> Unit,
    onSave: (tableName: String, capacity: Int, location: String?) -> Unit
) {
    var tableName by remember { mutableStateOf(table?.name ?: "") }
    var capacityText by remember { mutableStateOf(table?.capacity?.toString() ?: "4") }
    var location by remember { mutableStateOf(table?.location ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (table == null) "Thêm bàn" else "Sửa bàn") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tableName,
                    onValueChange = { tableName = it },
                    label = { Text("Số bàn / Tên bàn") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("VD: Bàn 1, A1, VIP1...") }
                )
                
                // Capacity input (editable number field)
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { newValue ->
                        // Allow empty or valid numbers only
                        if (newValue.isEmpty() || newValue.toIntOrNull() != null) {
                            capacityText = newValue
                        }
                    },
                    label = { Text("Số chỗ ngồi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("VD: 4, 6, 8...") },
                    supportingText = { Text("Nhập số từ 1-50") }
                )
                
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Vị trí (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("VD: Tầng 1, Sân vườn...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tableName.isNotBlank()) {
                        val capacity = capacityText.toIntOrNull()?.coerceIn(1, 50) ?: 4
                        onSave(tableName, capacity, location.ifBlank { null })
                    }
                }
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

