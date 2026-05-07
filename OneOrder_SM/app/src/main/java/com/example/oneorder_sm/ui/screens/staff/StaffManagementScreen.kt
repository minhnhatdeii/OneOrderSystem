package com.example.oneorder_sm.ui.screens.staff

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.domain.model.Attendance
import com.example.oneorder_sm.domain.model.DailyNote
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.ui.screens.staff.components.AttendanceCalendar
import com.example.oneorder_sm.ui.screens.staff.components.AttendanceDetailDialog
import com.example.oneorder_sm.ui.screens.staff.components.GlobalAttendanceDetailDialog
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StaffManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.timekeepingSuccessMessage) {
        uiState.timekeepingSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTimekeepingMessage()
        }
    }

    LaunchedEffect(uiState.noteSuccessMessage) {
        uiState.noteSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearNoteSuccessMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.loadData() }) {
                        Text("Thử lại")
                    }
                }
            }
        } else {
            if (uiState.isManager) {
                ManagerDashboard(
                    staff = uiState.staff,
                    staffAttendance = uiState.staffAttendance,
                    staffDailyNotes = uiState.staffDailyNotes,
                    onFetchAttendance = { staffId, month, year ->
                        viewModel.fetchAttendance(staffId, month, year)
                        viewModel.fetchDailyNotes(staffId, month, year)
                    },
                    onFetchAllData = { month, year ->
                        viewModel.fetchAllDataForMonth(month, year)
                    },
                    onApproveAttendance = { staffId, attendanceId ->
                        viewModel.approveAttendance(staffId, attendanceId)
                    },
                    onRejectAttendance = { staffId, attendanceId ->
                        viewModel.rejectAttendance(staffId, attendanceId)
                    },
                    onSubmitAttendance = { staffId, staffName, date, note ->
                        viewModel.submitTimekeeping(staffId, staffName, date, note)
                    },
                    onSubmitDayOff = { staffId, staffName, date, reason ->
                        viewModel.submitDayOffRequest(staffId, staffName, date, reason)
                    },
                    onSaveNote = { staffId, staffName, noteDate, content ->
                        viewModel.saveNote(staffId, staffName, noteDate, content)
                    },
                    onDeleteNote = { staffId, noteId, noteDate ->
                        viewModel.deleteNote(staffId, noteId, noteDate)
                    },
                    onToggleActive = { staffId, isActive ->
                        if (isActive) viewModel.deactivateStaff(staffId)
                        else viewModel.reactivateStaff(staffId)
                    }
                )
            } else {
                if (uiState.staff.isNotEmpty()) {
                    val profile = uiState.staff.first()
                    StaffPersonalDashboard(
                        profile = profile,
                        attendanceList = uiState.staffAttendance[profile.id] ?: emptyList(),
                        dailyNotes = uiState.staffDailyNotes[profile.id] ?: emptyList(),
                        onFetchAttendance = { month, year ->
                            viewModel.fetchAttendance(profile.id, month, year)
                            viewModel.fetchDailyNotes(profile.id, month, year)
                        },
                        onSubmitAttendance = { date, note ->
                            viewModel.submitTimekeeping(profile.id, profile.fullName ?: "Nhân viên", date, note)
                        },
                        onSubmitDayOff = { date, reason ->
                            viewModel.submitDayOffRequest(profile.id, profile.fullName ?: "Nhân viên", date, reason)
                        }
                    )
                }
            }
        }

        // FAB
        if (uiState.isManager) {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm nhân viên")
            }
        }

        // Create staff dialog
        if (showCreateDialog) {
            CreateStaffDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { email, fullName, phone, role ->
                    viewModel.createStaff(email, fullName, phone, role)
                    showCreateDialog = false
                }
            )
        }

        // Success dialog showing result
        if (uiState.tempPassword != null) {
            SuccessDialog(
                email = uiState.createdEmail ?: "",
                password = uiState.tempPassword!!,
                onDismiss = { viewModel.clearTempPassword() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun StaffList(
    staff: List<Profile>,
    isManager: Boolean,
    staffAttendance: Map<String, List<Attendance>>,
    staffDailyNotes: Map<String, List<DailyNote>>,
    onFetchAttendance: (String, Int, Int) -> Unit,
    onApproveAttendance: (String, String) -> Unit,
    onRejectAttendance: (String, String) -> Unit,
    onSubmitAttendance: (String, String, String, String) -> Unit,
    onSubmitDayOff: (staffId: String, staffName: String, date: String, reason: String) -> Unit,
    onSaveNote: (staffId: String, staffName: String, noteDate: String, content: String) -> Unit,
    onDeleteNote: (staffId: String, noteId: String, noteDate: String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit
) {
    if (staff.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chưa có nhân viên nào")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(staff) { profile ->
                StaffCard(
                    profile = profile,
                    isManager = isManager,
                    attendanceList = staffAttendance[profile.id] ?: emptyList(),
                    dailyNotes = staffDailyNotes[profile.id] ?: emptyList(),
                    onFetchAttendance = { month, year -> onFetchAttendance(profile.id, month, year) },
                    onApproveAttendance = { attendanceId -> onApproveAttendance(profile.id, attendanceId) },
                    onRejectAttendance = { attendanceId -> onRejectAttendance(profile.id, attendanceId) },
                    onSubmitAttendance = { date, note ->
                        onSubmitAttendance(profile.id, profile.fullName ?: "Nhân viên", date, note)
                    },
                    onSubmitDayOff = { date, reason ->
                        onSubmitDayOff(profile.id, profile.fullName ?: "Nhân viên", date, reason)
                    },
                    onSaveNote = { noteDate, content ->
                        onSaveNote(profile.id, profile.fullName ?: "Nhân viên", noteDate, content)
                    },
                    onDeleteNote = { noteId, noteDate ->
                        onDeleteNote(profile.id, noteId, noteDate)
                    },
                    onToggleActive = { onToggleActive(profile.id, profile.isActive) }
                )
            }
        }
    }
}

@Composable
fun StaffCard(
    profile: Profile,
    isManager: Boolean,
    attendanceList: List<Attendance>,
    dailyNotes: List<DailyNote>,
    onFetchAttendance: (Int, Int) -> Unit,
    onApproveAttendance: (String) -> Unit,
    onRejectAttendance: (String) -> Unit,
    onSubmitAttendance: (String, String) -> Unit,
    onSubmitDayOff: (date: String, reason: String) -> Unit,
    onSaveNote: (noteDate: String, content: String) -> Unit,
    onDeleteNote: (noteId: String, noteDate: String) -> Unit,
    onToggleActive: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDateForDetail by remember { mutableStateOf<LocalDate?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                isExpanded = !isExpanded
                if (isExpanded) {
                    val now = LocalDate.now()
                    onFetchAttendance(now.monthValue, now.year)
                }
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = if (!profile.isActive)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (profile.avatarUrl != null) {
                    AsyncImage(
                        model = profile.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile.fullName ?: "Chưa có tên",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (profile.role == "manager") "Quản lý" else "Nhân viên",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!profile.isActive) {
                        Text(
                            text = "Đã vô hiệu hóa",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (isManager) {
                    IconButton(onClick = onToggleActive) {
                        Icon(
                            imageVector = if (profile.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                            contentDescription = if (profile.isActive) "Vô hiệu hóa" else "Kích hoạt lại",
                            tint = if (profile.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                AttendanceCalendar(
                    selectedStaffId = profile.id,
                    attendanceList = attendanceList,
                    onDateSelected = { selectedDateForDetail = it },
                    onMonthChanged = { month, year -> onFetchAttendance(month, year) },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    if (selectedDateForDetail != null) {
        val dateString = selectedDateForDetail.toString()
        val dayAttendance = attendanceList.filter { it.date == dateString }

        val dayNote = dailyNotes.find { it.noteDate == dateString }

        AttendanceDetailDialog(
            selectedDate = selectedDateForDetail!!,
            attendanceList = dayAttendance,
            dailyNote = dayNote,
            isManager = isManager,
            onApprove = onApproveAttendance,
            onReject = onRejectAttendance,
            onSubmitManual = onSubmitAttendance,
            onSubmitDayOff = onSubmitDayOff,
            onSaveNote = onSaveNote,
            onDismiss = { selectedDateForDetail = null }
        )
    }
}

@Composable
fun ManagerDashboard(
    staff: List<Profile>,
    staffAttendance: Map<String, List<Attendance>>,
    staffDailyNotes: Map<String, List<DailyNote>>,
    onFetchAttendance: (String, Int, Int) -> Unit,
    onFetchAllData: (Int, Int) -> Unit,
    onApproveAttendance: (String, String) -> Unit,
    onRejectAttendance: (String, String) -> Unit,
    onSubmitAttendance: (String, String, String, String) -> Unit,
    onSubmitDayOff: (String, String, String, String) -> Unit,
    onSaveNote: (String, String, String, String) -> Unit,
    onDeleteNote: (String, String, String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit
) {
    var selectedDateForDetail by remember { mutableStateOf<LocalDate?>(null) }
    val flattenedAttendance = remember(staffAttendance) { staffAttendance.values.flatten() }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Lịch Ghi Chú Công Chung",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    AttendanceCalendar(
                        selectedStaffId = "GLOBAL",
                        attendanceList = flattenedAttendance,
                        onDateSelected = { selectedDateForDetail = it },
                        onMonthChanged = { month, year -> onFetchAllData(month, year) }
                    )
                }
            }
        }

        item {
            Text(
                text = "Danh sách Nhân viên",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(staff) { profile ->
            StaffCard(
                profile = profile,
                isManager = true,
                attendanceList = staffAttendance[profile.id] ?: emptyList(),
                dailyNotes = staffDailyNotes[profile.id] ?: emptyList(),
                onFetchAttendance = { month, year -> onFetchAttendance(profile.id, month, year) },
                onApproveAttendance = { attendanceId -> onApproveAttendance(profile.id, attendanceId) },
                onRejectAttendance = { attendanceId -> onRejectAttendance(profile.id, attendanceId) },
                onSubmitAttendance = { date, note ->
                    onSubmitAttendance(profile.id, profile.fullName ?: "Nhân viên", date, note)
                },
                onSubmitDayOff = { date, reason ->
                    onSubmitDayOff(profile.id, profile.fullName ?: "Nhân viên", date, reason)
                },
                onSaveNote = { noteDate, content ->
                    onSaveNote(profile.id, profile.fullName ?: "Nhân viên", noteDate, content)
                },
                onDeleteNote = { noteId, noteDate ->
                    onDeleteNote(profile.id, noteId, noteDate)
                },
                onToggleActive = { onToggleActive(profile.id, profile.isActive) }
            )
        }
    }

    if (selectedDateForDetail != null) {
        GlobalAttendanceDetailDialog(
            selectedDate = selectedDateForDetail!!,
            staffList = staff,
            staffAttendance = staffAttendance,
            onApprove = onApproveAttendance,
            onReject = onRejectAttendance,
            onCreateShift = onSubmitAttendance,
            onDismiss = { selectedDateForDetail = null }
        )
    }
}

@Composable
fun StaffPersonalDashboard(
    profile: Profile,
    attendanceList: List<Attendance>,
    dailyNotes: List<DailyNote>,
    onFetchAttendance: (Int, Int) -> Unit,
    onSubmitAttendance: (String, String) -> Unit,
    onSubmitDayOff: (String, String) -> Unit
) {
    var selectedDateForDetail by remember { mutableStateOf<LocalDate?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Lịch Ghi Chú Công Cá Nhân",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                AttendanceCalendar(
                    selectedStaffId = profile.id,
                    attendanceList = attendanceList,
                    onDateSelected = { selectedDateForDetail = it },
                    onMonthChanged = { month, year -> onFetchAttendance(month, year) }
                )
            }
        }
    }

    if (selectedDateForDetail != null) {
        val dateString = selectedDateForDetail.toString()
        val dayAttendance = attendanceList.filter { it.date == dateString }
        val dayNote = dailyNotes.find { it.noteDate == dateString }

        AttendanceDetailDialog(
            selectedDate = selectedDateForDetail!!,
            attendanceList = dayAttendance,
            dailyNote = dayNote,
            isManager = false,
            onApprove = {},
            onReject = {},
            onSubmitManual = onSubmitAttendance,
            onSubmitDayOff = onSubmitDayOff,
            onDismiss = { selectedDateForDetail = null }
        )
    }
}

@Composable
fun CreateStaffDialog(
    onDismiss: () -> Unit,
    onCreate: (email: String, fullName: String, phone: String?, role: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isManager by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo tài khoản nhân viên") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Họ và tên *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isManager,
                        onCheckedChange = { isManager = it }
                    )
                    Text("Cấp quyền quản lý")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        email,
                        fullName,
                        phone.takeIf { it.isNotBlank() },
                        if (isManager) "manager" else "staff"
                    )
                },
                enabled = email.isNotBlank() && fullName.isNotBlank()
            ) {
                Text("Tạo tài khoản")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun SuccessDialog(
    email: String,
    password: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lời mời đã được gửi!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Email mời tham gia đã được gửi đến:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Email:", style = MaterialTheme.typography.labelMedium)
                        Text(email, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Nhân viên sẽ nhận email với link để tạo mật khẩu và kích hoạt tài khoản.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đã hiểu")
            }
        }
    )
}
