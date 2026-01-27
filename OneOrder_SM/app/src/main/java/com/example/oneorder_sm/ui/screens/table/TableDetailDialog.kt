package com.example.oneorder_sm.ui.screens.table

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.data.model.Table

@Composable
fun TableDetailDialog(
    table: Table,
    qrCode: Bitmap?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGenerateQR: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Đóng")
            }
        },
        title = { Text("Bàn ${table.name}") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Trạng thái: ${if (table.status == "free") "Trống" else "Có khách"}")

                if (qrCode != null) {
                    Image(
                        bitmap = qrCode.asImageBitmap(),
                        contentDescription = "QR Code cho bàn ${table.name}",
                        modifier = Modifier.size(200.dp)
                    )
                } else {
                    Button(onClick = onGenerateQR) {
                        Text("Tạo mã QR")
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("Sửa")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

