package com.example.oneorder.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.example.oneorder.R
import com.example.oneorder.utils.RateLimitResult
import com.example.oneorder.utils.SecurityUtils
import com.example.oneorder.utils.PasswordValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToMfa: (String) -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    successMessage: String? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    
    // Password visibility
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Rate limiting state
    var rateLimitMessage by remember { mutableStateOf<String?>(null) }
    var retryCountdown by remember { mutableStateOf(0L) }
    
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    
    // Load saved credentials if available
    LaunchedEffect(Unit) {
        val preferences = viewModel.getSavedCredentials().first()
        if (preferences.rememberMe && preferences.email.isNotBlank()) {
            email = preferences.email
            password = preferences.password
            rememberMe = true
        }
    }

    // Set success message from navigation if provided
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            viewModel.setLoginMessage(successMessage)
        }
    }

    LaunchedEffect(loginState.isSuccess) {
        if (loginState.isSuccess) {
            onNavigateToHome()
            viewModel.resetState()
        }
    }

    // Navigate to MFA verification when new device detected
    LaunchedEffect(loginState.requiresMfa) {
        if (loginState.requiresMfa && loginState.newDeviceDetected) {
            onNavigateToMfa(email)
            viewModel.resetState()
        }
    }
    
    // Countdown timer for rate limiting
    LaunchedEffect(retryCountdown) {
        if (retryCountdown > 0) {
            delay(1000)
            retryCountdown -= 1000
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

        // Success message from registration
        if (loginState.successMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = loginState.successMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                rateLimitMessage = null // Clear error when user types
            },
            label = { Text(stringResource(R.string.email)) },
            singleLine = true,
            isError = email.isNotBlank() && PasswordValidator.getEmailError(email) != null,
            supportingText = if (email.isNotBlank() && PasswordValidator.getEmailError(email) != null) {
                { Text(PasswordValidator.getEmailError(email)!!, color = MaterialTheme.colorScheme.error) }
            } else null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                rateLimitMessage = null // Clear error when user types
            },
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
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { 
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank()) {
                    handleLogin(viewModel, email, password, rememberMe, context) { msg, countdown ->
                        rateLimitMessage = msg
                        retryCountdown = countdown
                    }
                }
            }),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        // Rate limit warning message
        if (rateLimitMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rateLimitMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    if (retryCountdown > 0) {
                        Text(
                            text = "${retryCountdown / 1000}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Remember me checkbox
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )
            Text(
                text = stringResource(R.string.remember_me),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (loginState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
        }

        Button(
            onClick = {
                handleLogin(viewModel, email, password, rememberMe, context) { msg, countdown ->
                    rateLimitMessage = msg
                    retryCountdown = countdown
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !loginState.isLoading && email.isNotBlank() && password.isNotBlank() && retryCountdown == 0L
        ) {
            Text(stringResource(R.string.login))
        }

        if (loginState.error != null) {
            Text(
                text = loginState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.dont_have_account_register),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Quên mật khẩu?",
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.clickable { onNavigateToForgotPassword() }
        )
    }
}

/**
 * Handle login with rate limiting
 */
private fun handleLogin(
    viewModel: AuthViewModel,
    email: String,
    password: String,
    rememberMe: Boolean,
    context: android.content.Context,
    onRateLimit: (String?, Long) -> Unit
) {
    // Create SecurityUtils instance
    val securityUtils = SecurityUtils(context)
    
    // Check rate limiting
    when (val result = securityUtils.isLoginAllowed(email)) {
        is RateLimitResult.Blocked -> {
            onRateLimit(result.reason, result.retryAfterMs)
            return
        }
        is RateLimitResult.RateLimited -> {
            onRateLimit(result.message, result.retryAfterMs)
            return
        }
        is RateLimitResult.Allowed -> {
            onRateLimit(null, 0)
        }
    }
    
    // Attempt login
    viewModel.login(email, password, rememberMe)
}
