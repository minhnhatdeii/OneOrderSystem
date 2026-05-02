package com.example.oneorder.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder.R
import kotlinx.coroutines.delay

/**
 * MFA Verification Screen
 * Shown after successful login when MFA is enabled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaVerificationScreen(
    email: String,
    onVerified: (String) -> Unit,
    onBack: () -> Unit,
    onUseBackupCode: () -> Unit,
    viewModel: MfaViewModel = hiltViewModel()
) {
    val mfaState by viewModel.mfaState.collectAsState()
    var otpCode by remember { mutableStateOf("") }

    // Send OTP on screen load
    LaunchedEffect(Unit) {
        viewModel.sendOtp(email)
    }

    // Handle verification success - pass email to register device
    LaunchedEffect(mfaState.isVerified) {
        if (mfaState.isVerified) {
            delay(500) // Brief delay for success message
            onVerified(email)
        }
    }
    
    // Auto-submit when 6 digits entered
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6 && !mfaState.isLoading) {
            viewModel.verifyOtp(email, otpCode)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Xác thực hai yếu tố") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Lock icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Nhập mã xác thực",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = "Mã xác thực đã được gửi đến\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // OTP Input
            OutlinedTextField(
                value = otpCode,
                onValueChange = { 
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpCode = it
                    }
                },
                label = { Text("Mã xác thực (6 chữ số)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (otpCode.length == 6) {
                            viewModel.verifyOtp(email, otpCode)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Error message
            AnimatedVisibility(visible = mfaState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = mfaState.error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Success message
            AnimatedVisibility(visible = mfaState.isVerified) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = mfaState.message ?: "Xác thực thành công!",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Attempts remaining
            if (mfaState.attemptsRemaining < 3 && !mfaState.isVerified) {
                Text(
                    text = "Còn ${mfaState.attemptsRemaining} lần thử",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Resend button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Không nhận được mã?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { viewModel.resendOtp(email) },
                    enabled = mfaState.resendCooldown == 0 && !mfaState.isLoading
                ) {
                    if (mfaState.resendCooldown > 0) {
                        Text("Gửi lại sau ${mfaState.resendCooldown}s")
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Gửi lại")
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Loading indicator
            if (mfaState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Verify button
            Button(
                onClick = { viewModel.verifyOtp(email, otpCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = otpCode.length == 6 && !mfaState.isLoading && !mfaState.isLocked
            ) {
                Text("Xác thực")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Use backup code
            OutlinedButton(
                onClick = onUseBackupCode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sử dụng mã dự phòng")
            }
            
            // Lockout warning
            if (mfaState.isLocked) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Đã khóa do nhập sai quá nhiều. Vui lòng thử lại sau 5 phút.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * MFA Backup Code Verification Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MfaBackupCodeScreen(
    email: String,
    onVerified: (String) -> Unit,
    onBack: () -> Unit,
    onBackToOtp: () -> Unit,
    viewModel: MfaViewModel = hiltViewModel()
) {
    val mfaState by viewModel.mfaState.collectAsState()
    var backupCode by remember { mutableStateOf("") }

    LaunchedEffect(mfaState.isVerified) {
        if (mfaState.isVerified) {
            delay(500)
            onVerified(email)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mã dự phòng") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Lock icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Nhập mã dự phòng",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = "Mã dự phòng được cung cấp khi bạn bật xác thực hai yếu tố",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Backup Code Input
            OutlinedTextField(
                value = backupCode,
                onValueChange = { backupCode = it.uppercase().take(8) },
                label = { Text("Mã dự phòng") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Error message
            AnimatedVisibility(visible = mfaState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = mfaState.error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Loading indicator
            if (mfaState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            // Verify button
            Button(
                onClick = { viewModel.verifyBackupCode(email, backupCode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = backupCode.length == 8 && !mfaState.isLoading
            ) {
                Text("Xác thực")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Back to OTP
            TextButton(onClick = onBackToOtp) {
                Text("Quay lại xác thực bằng mã OTP")
            }
        }
    }
}
