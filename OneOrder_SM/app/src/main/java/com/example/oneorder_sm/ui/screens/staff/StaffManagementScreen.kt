package com.example.oneorder_sm.ui.screens.staff

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.domain.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: StaffManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    // Content only - no Scaffold/TopAppBar (handled by MainScreen)
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
            StaffList(
                staff = uiState.staff,
                onToggleActive = { staffId, isActive ->
                    if (isActive) viewModel.deactivateStaff(staffId)
                    else viewModel.reactivateStaff(staffId)
                }
            )
        }

        // FAB
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm nhân viên")
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

        // Success dialog showing password
        if (uiState.tempPassword != null) {
            SuccessDialog(
                email = uiState.createdEmail ?: "",
                password = uiState.tempPassword!!,
                onDismiss = { viewModel.clearTempPassword() }
            )
        }
    }
}

@Composable
fun StaffList(
    staff: List<Profile>,
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
                    onToggleActive = { onToggleActive(profile.id, profile.isActive) }
                )
            }
        }
    }
}

@Composable
fun StaffCard(
    profile: Profile,
    onToggleActive: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (!profile.isActive) 
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        else CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.fullName ?: "Chưa có tên",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (profile.role == "manager") "Quản lý" else "Nhân viên",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (!profile.isActive) {
                    Text(
                        text = "Đã vô hiệu hóa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onToggleActive) {
                Icon(
                    imageVector = if (profile.isActive) Icons.Default.PersonOff else Icons.Default.PersonAdd,
                    contentDescription = if (profile.isActive) "Vô hiệu hóa" else "Kích hoạt lại",
                    tint = if (profile.isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
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
    password: String, // Now contains message instead of password
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
