package com.example.oneorder.ui.screens.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.example.oneorder.R
import com.example.oneorder.utils.PasswordValidator

@Composable
fun RegisterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Password visibility toggles
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val registerState by viewModel.registerState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // Real-time validation
    val passwordValidation by remember(password) {
        derivedStateOf { PasswordValidator.validate(password, email) }
    }
    val emailError by remember(email) {
        derivedStateOf { PasswordValidator.getEmailError(email) }
    }
    val passwordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
    val canSubmit = fullName.isNotBlank() &&
                    emailError == null &&
                    passwordValidation.isValid &&
                    passwordsMatch &&
                    !registerState.isLoading

    val successMsg = stringResource(R.string.registration_success)

    LaunchedEffect(registerState.isSuccess) {
        if (registerState.isSuccess) {
            val message = registerState.successMessage ?: successMsg
            onNavigateToLogin(message)
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo_oo2),
            contentDescription = "OneOrder Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 16.dp)
        )
        
        // Styled OneOrder text
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)) {
                    append("One")
                }
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("Order")
                }
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Welcome message
        Text(
            text = stringResource(R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Full Name Field
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text(stringResource(R.string.full_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Email Field with validation
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            isError = emailError != null && email.isNotBlank(),
            supportingText = if (emailError != null && email.isNotBlank()) {
                { Text(emailError!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Password Field with strength indicator
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )

        // Password Strength Indicator
        if (password.isNotEmpty()) {
            PasswordStrengthIndicator(
                strength = passwordValidation.strength,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            
            // Password Requirements Checklist
            PasswordRequirementsList(
                requirements = PasswordValidator.getPasswordRequirements(password),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Xác nhận mật khẩu") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                { Text("Mật khẩu không khớp", color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )

        if (registerState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = {
                viewModel.register(email, password, fullName)
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = canSubmit
        ) {
            Text(stringResource(R.string.register))
        }

        if (registerState.error != null) {
            Text(
                text = registerState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { onNavigateBack() }) {
            Text(stringResource(R.string.back_to_login))
        }
    }
}

/**
 * Password Strength Indicator Bar
 */
@Composable
fun PasswordStrengthIndicator(
    strength: PasswordValidator.PasswordStrength,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = when (strength) {
            PasswordValidator.PasswordStrength.WEAK -> Color(0xFFE53935)
            PasswordValidator.PasswordStrength.MEDIUM -> Color(0xFFFFA726)
            PasswordValidator.PasswordStrength.STRONG -> Color(0xFF43A047)
        },
        label = "strengthColor"
    )
    
    val strengthLabel = when (strength) {
        PasswordValidator.PasswordStrength.WEAK -> "Yếu"
        PasswordValidator.PasswordStrength.MEDIUM -> "Trung bình"
        PasswordValidator.PasswordStrength.STRONG -> "Mạnh"
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Độ mạnh mật khẩu:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = strengthLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Strength bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(strength.progress)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

/**
 * Password Requirements Checklist
 */
@Composable
fun PasswordRequirementsList(
    requirements: List<PasswordValidator.PasswordRequirement>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Yêu cầu mật khẩu:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        requirements.forEach { requirement ->
            RequirementItem(requirement)
        }
    }
}

@Composable
private fun RequirementItem(requirement: PasswordValidator.PasswordRequirement) {
    val color by animateColorAsState(
        targetValue = if (requirement.isMet) Color(0xFF43A047) else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "requirementColor"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (requirement.isMet) color else Color.Transparent)
                .then(
                    if (!requirement.isMet) Modifier.background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (requirement.isMet) {
                Text(
                    text = "✓",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = requirement.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (requirement.isMet) color else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
