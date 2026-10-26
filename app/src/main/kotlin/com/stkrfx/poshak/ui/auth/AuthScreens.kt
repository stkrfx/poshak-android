package com.stkrfx.poshak.ui.auth

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stkrfx.poshak.data.ApiException
import com.stkrfx.poshak.data.AppRelease
import com.stkrfx.poshak.data.EmployeeProfile
import com.stkrfx.poshak.data.PoshakApi
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    api: PoshakApi,
    initialMessage: String? = null,
    onLoginSuccess: (EmployeeProfile) -> Unit,
    onForgotPassword: () -> Unit,
    onForceUpdate: (AppRelease) -> Unit,
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf(initialMessage) }
    var submittedAttempt by remember { mutableStateOf<Pair<String, String>?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Poshak", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text("Your attendance, all in one place.", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = identifier,
            onValueChange = { identifier = it.take(150); errorMessage = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Mobile number, email or username") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it.take(72); errorMessage = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                    )
                }
            },
            enabled = !isLoading,
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(10.dp))
            Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.End),
            enabled = !isLoading,
        ) { Text("Forgot password?") }

        Button(
            onClick = {
                when {
                    identifier.trim().length < 3 -> errorMessage = "Enter your mobile number, email or username."
                    password.length < 6 -> errorMessage = "Password must be at least 6 characters long."
                    password.toByteArray(Charsets.UTF_8).size > 72 -> errorMessage = "Password must not exceed 72 bytes."
                    else -> {
                        isLoading = true
                        errorMessage = null
                        submittedAttempt = identifier.trim() to password
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Sign in")
        }
    }

    LaunchedEffect(submittedAttempt) {
        val attempt = submittedAttempt ?: return@LaunchedEffect
        try {
            val profile = api.login(attempt.first, attempt.second)
            submittedAttempt = null
            isLoading = false
            onLoginSuccess(profile)
        } catch (error: ApiException) {
            if (error.apiCode == "APP_UPDATE_REQUIRED" && error.appRelease != null) {
                onForceUpdate(error.appRelease)
            } else {
                errorMessage = error.message
            }
            isLoading = false
            submittedAttempt = null
        } catch (_: Exception) {
            errorMessage = "Something went wrong. Please try again."
            isLoading = false
            submittedAttempt = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    api: PoshakApi,
    onBack: () -> Unit,
    onOtpSent: (String) -> Unit,
    onForceUpdate: (AppRelease) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Forgot password") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Reset your password", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Enter the email address linked to your Poshak account. We'll send you a 6-digit verification code.")
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trimStart().take(150); error = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !loading,
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (!EMAIL_REGEX.matches(email.trim())) {
                        error = "Enter a valid email address."
                    } else {
                        loading = true
                        error = null
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading,
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Send verification code")
            }
        }
    }

    var submittedEmail by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(loading) {
        if (!loading || submittedEmail != null) return@LaunchedEffect
        submittedEmail = email.trim()
        try {
            api.sendPasswordResetOtp(email.trim())
            loading = false
            submittedEmail = null
            onOtpSent(email.trim())
        } catch (exception: ApiException) {
            if (exception.apiCode == "APP_UPDATE_REQUIRED" && exception.appRelease != null) {
                onForceUpdate(exception.appRelease)
            } else {
                error = exception.message
            }
            loading = false
            submittedEmail = null
        } catch (_: Exception) {
            error = "Could not send the code. Please try again."
            loading = false
            submittedEmail = null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailOtpScreen(
    api: PoshakApi,
    email: String,
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
    onForceUpdate: (AppRelease) -> Unit,
) {
    var otp by remember { mutableStateOf("") }
    var loading by rememberSaveable { mutableStateOf(false) }
    var resendSeconds by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1_000)
            resendSeconds -= 1
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Verify email") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Enter the 6-digit code", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("We sent a password-reset code to $email", textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            OtpInput(value = otp, onValueChange = { otp = it; error = null }, enabled = !loading)
            if (error != null) {
                Spacer(Modifier.height(12.dp))
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            if (message != null) {
                Spacer(Modifier.height(12.dp))
                Text(message.orEmpty(), color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (otp.length != 6 || !otp.all(Char::isDigit)) error = "Enter all 6 digits from the email."
                    else onContinue(otp)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading,
            ) { Text("Continue") }
            TextButton(
                onClick = {
                    loading = true
                    error = null
                    message = null
                },
                enabled = !loading && resendSeconds == 0,
            ) {
                Text(if (resendSeconds > 0) "Resend code in ${resendSeconds}s" else "Resend code")
            }
        }
    }

    var shouldResend by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (!loading || shouldResend) return@LaunchedEffect
        shouldResend = true
        try {
            message = api.sendPasswordResetOtp(email)
            otp = ""
            resendSeconds = 30
        } catch (exception: ApiException) {
            if (exception.apiCode == "APP_UPDATE_REQUIRED" && exception.appRelease != null) {
                onForceUpdate(exception.appRelease)
            } else {
                error = exception.message
            }
        } catch (_: Exception) {
            error = "Could not resend the code. Please try again."
        } finally {
            loading = false
            shouldResend = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordScreen(
    api: PoshakApi,
    email: String,
    otp: String,
    onBack: () -> Unit,
    onPasswordReset: () -> Unit,
    onForceUpdate: (AppRelease) -> Unit,
) {
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmVisible by rememberSaveable { mutableStateOf(false) }
    var loading by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var successMessage by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Set new password") }, navigationIcon = {
            TextButton(onClick = onBack) { Text("Back") }
        })
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Create a new password", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Choose a password with at least 6 characters.")
            Spacer(Modifier.height(22.dp))
            PasswordInput(
                value = newPassword,
                onValueChange = { newPassword = it.take(72); error = null },
                label = "New password",
                visible = passwordVisible,
                onToggleVisibility = { passwordVisible = !passwordVisible },
                enabled = !loading,
            )
            Spacer(Modifier.height(12.dp))
            PasswordInput(
                value = confirmPassword,
                onValueChange = { confirmPassword = it.take(72); error = null },
                label = "Confirm new password",
                visible = confirmVisible,
                onToggleVisibility = { confirmVisible = !confirmVisible },
                enabled = !loading,
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            if (successMessage != null) {
                Spacer(Modifier.height(10.dp))
                Text(successMessage.orEmpty(), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    when {
                        newPassword.length < 6 -> error = "Password must be at least 6 characters long."
                        newPassword.toByteArray(Charsets.UTF_8).size > 72 -> error = "Password must not exceed 72 bytes."
                        newPassword != confirmPassword -> error = "The passwords do not match."
                        else -> { loading = true; error = null }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading,
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Update password")
            }
        }
    }

    LaunchedEffect(loading) {
        if (!loading) return@LaunchedEffect
        try {
            successMessage = api.resetPasswordWithEmailOtp(email, otp, newPassword)
            onPasswordReset()
        } catch (exception: ApiException) {
            if (exception.apiCode == "APP_UPDATE_REQUIRED" && exception.appRelease != null) {
                onForceUpdate(exception.appRelease)
            } else {
                error = exception.message
            }
            loading = false
        } catch (_: Exception) {
            error = "Could not reset your password. Please try again."
            loading = false
        }
    }
}

@Composable
private fun PasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                )
            }
        },
    )
}

@Composable
private fun OtpInput(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    Box(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val digits = newValue.filter(Char::isDigit).take(6)
                onValueChange(digits)
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
            decorationBox = { innerTextField ->
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(6) { index ->
                            val digit = value.getOrNull(index)?.toString().orEmpty()
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        width = if (index == value.length && enabled) 2.dp else 1.dp,
                                        color = if (index == value.length && enabled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(digit, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                    // Keeps BasicTextField focused and receives the numeric keyboard while the six boxes are decorative.
                    Box(Modifier.fillMaxSize().padding(0.dp)) { innerTextField() }
                }
            },
        )
    }
}

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
