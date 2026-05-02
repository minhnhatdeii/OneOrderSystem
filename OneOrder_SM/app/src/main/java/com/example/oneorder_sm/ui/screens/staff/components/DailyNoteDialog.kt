package com.example.oneorder_sm.ui.screens.staff.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.AttendanceStatus
import com.example.oneorder_sm.domain.model.DailyNote
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeFormatter

/**
 * Dialog hiển thị thông tin ngày công của một nhân viên:
 * - Phần 1: Danh sách ca làm việc (attendance) với trạng thái
 * - Phần 2: Bản ghi chú ngày (DailyNote) — manager có thể thêm/sửa/xóa
 */
@Composable
fun DailyNoteDialog(
    selectedDate: LocalDate,
    staffName: String,
    attendanceList: List<Attendance>,
    dailyNote: DailyNote?,      // Ghi chú của ngày này (nếu có)
    isManager: Boolean,
    onApproveAttendance: (String) -> Unit,
    onSaveNote: (content: String) -> Unit,
    onDeleteNote: (noteId: String) -> Unit,
    onSubmitManualAttendance: (checkIn: String, checkOut: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val isToday = selectedDate == LocalDate.now()
    val canAddAttendance = isManager || isToday

    val sortedAttendance = remember(attendanceList) {
        attendanceList.sortedBy { it.checkIn }
    }

    val totalHours = remember(attendanceList) {
        attendanceList.sumOf { att ->
            if (att.checkOut != null) {
                try {
                    val start = LocalTime.parse(att.checkIn)
                    val end = LocalTime.parse(att.checkOut)
                    Duration.between(start, end).toMinutes() / 60.0
                } catch (e: Exception) { 0.0 }
            } else 0.0
        }
    }

    // State cho Note editor
    var isEditingNote by remember { mutableStateOf(dailyNote == null && isManager) }
    var noteContent by remember(dailyNote) { mutableStateOf(dailyNote?.content ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // State cho form thêm ca
    var showAddShiftForm by remember { mutableStateOf(false) }
    var checkInTime by remember { mutableStateOf("") }
    var checkOutTime by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header gradient ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Column {
                        Text(
                            text = selectedDate.format(
                                DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy",
                                    java.util.Locale("vi"))
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = staffName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        if (totalHours > 0) {
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = " ⏱ Tổng: ${String.format("%.1f", totalHours)}h ",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    // Nút đóng
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    // ── Section 1: Ca làm việc ────────────────────────────────
                    SectionHeader(
                        icon = Icons.Default.AccessTime,
                        title = "Ca làm việc",
                        tint = MaterialTheme.colorScheme.primary
                    )

                    if (sortedAttendance.isEmpty()) {
                        EmptyStateCard(message = "Chưa có ca làm việc nào")
                    } else {
                        sortedAttendance.forEach { attendance ->
                            AttendanceShiftCard(
                                attendance = attendance,
                                isManager = isManager,
                                onApprove = { onApproveAttendance(attendance.id ?: "") }
                            )
                        }
                    }

                    // Form thêm ca (manager hoặc hôm nay)
                    if (canAddAttendance) {
                        if (!showAddShiftForm) {
                            OutlinedButton(
                                onClick = { showAddShiftForm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isManager) "Thêm ca" else "Chấm công hôm nay")
                            }
                        } else {
                            AddShiftForm(
                                checkInTime = checkInTime,
                                checkOutTime = checkOutTime,
                                isManager = isManager,
                                onCheckInChange = { checkInTime = it },
                                onCheckOutChange = { checkOutTime = it },
                                onSubmit = {
                                    if (checkInTime.isNotBlank()) {
                                        onSubmitManualAttendance(
                                            checkInTime,
                                            if (checkOutTime.isBlank()) null else checkOutTime
                                        )
                                        checkInTime = ""
                                        checkOutTime = ""
                                        showAddShiftForm = false
                                    }
                                },
                                onCancel = {
                                    showAddShiftForm = false
                                    checkInTime = ""
                                    checkOutTime = ""
                                }
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // ── Section 2: Ghi chú ngày công ─────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(
                            icon = Icons.Default.StickyNote2,
                            title = "Ghi chú ngày công",
                            tint = Color(0xFFFF9800)
                        )

                        if (isManager) {
                            Row {
                                if (dailyNote != null && !isEditingNote) {
                                    IconButton(onClick = {
                                        noteContent = dailyNote.content
                                        isEditingNote = true
                                    }) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Sửa ghi chú",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(onClick = { showDeleteConfirm = true }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Xóa ghi chú",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hiển thị note nếu có và không đang edit
                    AnimatedVisibility(
                        visible = dailyNote != null && !isEditingNote,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (dailyNote != null) {
                            NoteCard(note = dailyNote)
                        }
                    }

                    // Form thêm/sửa note
                    AnimatedVisibility(
                        visible = isEditingNote && isManager,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        NoteEditorCard(
                            content = noteContent,
                            isExisting = dailyNote != null,
                            onContentChange = { noteContent = it },
                            onSave = {
                                if (noteContent.isNotBlank()) {
                                    onSaveNote(noteContent)
                                    isEditingNote = false
                                }
                            },
                            onCancel = {
                                isEditingNote = false
                                noteContent = dailyNote?.content ?: ""
                            }
                        )
                    }

                    // Placeholder nếu không có note và không edit
                    if (dailyNote == null && !isEditingNote) {
                        if (isManager) {
                            OutlinedButton(
                                onClick = {
                                    noteContent = ""
                                    isEditingNote = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFFF9800)
                                )
                            ) {
                                Icon(
                                    Icons.Default.NoteAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Thêm ghi chú ngày này")
                            }
                        } else {
                            EmptyStateCard(message = "Chưa có ghi chú nào cho ngày này")
                        }
                    }

                    // Nút đóng ở dưới
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Đóng")
                    }
                }
            }
        }
    }

    // Dialog xác nhận xóa note
    if (showDeleteConfirm && dailyNote != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Xóa ghi chú?") },
            text = { Text("Bạn có chắc muốn xóa ghi chú ngày này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteNote(dailyNote.id ?: "")
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Sub-components
// ────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            fontStyle = FontStyle.Italic
        )
    }
}

@Composable
private fun AttendanceShiftCard(
    attendance: Attendance,
    isManager: Boolean,
    onApprove: () -> Unit
) {
    val statusColor = when (attendance.status) {
        AttendanceStatus.APPROVED -> Color(0xFF4CAF50)
        AttendanceStatus.PENDING -> Color(0xFFFFC107)
        AttendanceStatus.REJECTED -> MaterialTheme.colorScheme.error
        AttendanceStatus.DAY_OFF -> Color(0xFF2196F3)
    }
    val statusText = when (attendance.status) {
        AttendanceStatus.APPROVED -> "Đã duyệt"
        AttendanceStatus.PENDING -> "Chờ duyệt"
        AttendanceStatus.REJECTED -> "Từ chối"
        AttendanceStatus.DAY_OFF -> "Xin nghỉ"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thanh màu trạng thái
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${attendance.checkIn}  →  ${attendance.checkOut ?: "Đang làm việc"}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
                attendance.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            if (isManager && attendance.status == AttendanceStatus.PENDING) {
                Button(
                    onClick = onApprove,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Duyệt", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun AddShiftForm(
    checkInTime: String,
    checkOutTime: String,
    isManager: Boolean,
    onCheckInChange: (String) -> Unit,
    onCheckOutChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (isManager) "Thêm ca làm việc" else "Chấm công hôm nay",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = onCheckInChange,
                    label = { Text("Giờ vào") },
                    placeholder = { Text("08:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF4CAF50)
                        )
                    }
                )
                OutlinedTextField(
                    value = checkOutTime,
                    onValueChange = onCheckOutChange,
                    label = { Text("Giờ ra") },
                    placeholder = { Text("17:00") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFFF44336)
                        )
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Hủy") }
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.weight(1f),
                    enabled = checkInTime.isNotBlank()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (isManager) "Thêm" else "Gửi yêu cầu")
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: DailyNote) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1),
                        Color(0xFFFFF3CD)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.StickyNote2,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ghi chú",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFF57C00),
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF5D4037),
                lineHeight = 22.sp
            )
            note.updatedAt?.let { updated ->
                Text(
                    text = "Cập nhật: $updated",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8D6E63),
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun NoteEditorCard(
    content: String,
    isExisting: Boolean,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color(0xFFFFCC02)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (isExisting) "Sửa ghi chú" else "Thêm ghi chú",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF57C00)
                )
            }
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 200.dp),
                placeholder = {
                    Text(
                        "Thêm lưu ý, nhận xét hoặc ghi chú về ngày công này...",
                        color = Color(0xFF8D6E63).copy(alpha = 0.7f)
                    )
                },
                maxLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFF9800),
                    unfocusedBorderColor = Color(0xFFFFCC02),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Hủy") }
                Button(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    enabled = content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        Icons.Default.Save,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Lưu ghi chú")
                }
            }
        }
    }
}
