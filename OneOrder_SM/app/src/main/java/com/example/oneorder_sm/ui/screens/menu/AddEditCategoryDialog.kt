package com.example.oneorder_sm.ui.screens.menu

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.data.model.Category

@Composable
fun AddEditCategoryDialog(
    category: Category? = null,
    onDismiss: () -> Unit,
    onSave: (name: String) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(if (category == null) "Thêm Danh Mục" else "Sửa Danh Mục") 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên Danh Mục") },
                    placeholder = { Text("VD: Đồ uống, Món chính, Tráng miệng...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Helpful hints for common categories
                Text(
                    text = "Gợi ý: Đồ ăn, Đồ uống, Món chay, Khai vị, Món chính, Tráng miệng, Cà phê, Nước ép",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim())
                    }
                },
                enabled = name.isNotBlank()
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
