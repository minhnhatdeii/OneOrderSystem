package com.example.oneorder_sm.ui.screens.staff.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontStyle
import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.AttendanceStatus
import com.example.oneorder_sm.domain.model.DailyNote
import com.example.oneorder_sm.domain.model.Profile
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalAttendanceDetailDialog(
    selectedDate: LocalDate,
    staffList: List<Profile>,
    staffAttendance: Map<String, List<Attendance>>,
    onApprove: (String, String) -> Unit,
    onReject: (String, String) -> Unit,
    onCreateShift: (String, String, String, String) -> Unit, // staffId, staffName, date, content
    onDismiss: () -> Unit
) {
    val dateString = selectedDate.toString()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Ghi chú công - Ngày $dateString",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(staffList) { profile ->
                    val attendances = staffAttendance[profile.id]?.filter { it.date == dateString } ?: emptyList()

                    if (profile.isActive || attendances.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = profile.fullName ?: "Chưa có tên",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Render Attendance List
                                if (attendances.isNotEmpty()) {
                                    attendances.forEach { att ->
                                        AttendanceItem(
                                            attendance = att
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Chưa có ghi chú nào",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))

                                // Phân ca làm
                                var isEditingNote by remember { mutableStateOf(false) }
                                var noteContent by remember { mutableStateOf("") }

                                if (isEditingNote) {
                                    OutlinedTextField(
                                        value = noteContent,
                                        onValueChange = { noteContent = it },
                                        label = { Text("Ghi chú mới (bắt buộc)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        minLines = 2
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { isEditingNote = false }) {
                                            Text("Hủy")
                                        }
                                        Button(onClick = {
                                            if(noteContent.isNotBlank()) {
                                                onCreateShift(profile.id, profile.fullName ?: "", dateString, noteContent)
                                            }
                                            isEditingNote = false
                                        }) {
                                            Text("Lưu ghi chú")
                                        }
                                    }
                                } else {
                                    TextButton(
                                        onClick = { isEditingNote = true },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("+ Thêm ghi chú mới")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────
// Card hiển thị ghi chú
// ────────────────────────────────────────────────────────
@Composable
private fun AttendanceItem(
    attendance: Attendance
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .border(1.dp, androidx.compose.ui.graphics.Color(0xFFFFC107), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFFFF8E1))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attendance.note?.takeIf { it.isNotBlank() } ?: "Không có ghi chú",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}
