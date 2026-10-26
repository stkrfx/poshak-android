@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.stkrfx.poshak.ui.workspace

import android.content.Intent
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.stkrfx.poshak.data.EmployeeProfile
import com.stkrfx.poshak.data.PoshakApi
import com.stkrfx.poshak.ui.home.HomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.stkrfx.poshak.data.ProfilePhotoProcessor
import com.stkrfx.poshak.ui.security.SensitiveWindowProtection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
internal fun ProfileOverviewScreen(
    profile: EmployeeProfile, displayName: String, username: String, email: String, address: String, dob: String,
    avatar: String, dobLocked: Boolean, canEdit: Boolean, onEdit: () -> Unit, onAvatar: () -> Unit,
    onChangePassword: () -> Unit, onDevices: () -> Unit, onSettings: () -> Unit, onSwitchAccount: () -> Unit,
    onLogout: () -> Unit, onSupport: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) { Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Your personal details and account security", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onSettings) { Icon(Icons.Filled.Settings, contentDescription = "Settings") }
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).combinedClickable(onClick = onAvatar, onLongClick = onAvatar), contentAlignment = Alignment.Center) {
                    AvatarContent(avatar, displayName, Modifier.size(76.dp), MaterialTheme.typography.headlineLarge)
                }
                Column(Modifier.weight(1f)) {
                    Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("@$username", color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(profile.mobileNumber, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                IconButton(onClick = onAvatar) { Icon(Icons.Filled.PhotoCamera, contentDescription = "Edit profile picture") }
            }
        }
        SectionTitle("Personal information")
        InfoRow(Icons.Filled.Badge, "Name", displayName, editable = false)
        InfoRow(Icons.Filled.PhoneAndroid, "Mobile number", profile.mobileNumber, editable = false)
        InfoRow(Icons.Filled.Person, "Gender", profile.gender, editable = false)
        InfoRow(Icons.Filled.CalendarMonth, "Date of birth", dob.ifBlank { "Not set" } + if (dobLocked) " · Locked" else "", editable = !dobLocked)
        InfoRow(Icons.Filled.AccountCircle, "Username", "@$username", editable = true)
        InfoRow(Icons.Filled.Email, "Email address", email.ifBlank { "Not set" }, editable = true)
        InfoRow(Icons.Filled.Home, "Address", address.ifBlank { "Not set" }, editable = true)
        if (canEdit) Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit profile") }
        else Card { Column(Modifier.padding(14.dp)) { Text("Profile editing is restricted", fontWeight = FontWeight.SemiBold); Text("Contact support if you believe this is a mistake."); TextButton(onClick = onSupport) { Text("Contact support") } } }
        SectionTitle("Security and devices")
        ActionRow(Icons.Filled.Lock, "Change password", "Update your account password", onChangePassword)
        ActionRow(Icons.Filled.Devices, "Logged-in devices", "Review and manage active sessions", onDevices)
        ActionRow(Icons.Filled.Tune, "Settings", "Theme, app lock and preferences", onSettings)
        ActionRow(Icons.Filled.SwapHoriz, "Switch account", "Manage accounts on this device", onSwitchAccount)
        ActionRow(Icons.AutoMirrored.Filled.HelpOutline, "Contact developer", "Get help on WhatsApp", onSupport)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.AutoMirrored.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun EditProfileScreen(
    profile: EmployeeProfile, username: String, email: String, address: String, dob: String, dobLocked: Boolean,
    error: String?, saved: Boolean, onUsernameChange: (String) -> Unit, onEmailChange: (String) -> Unit,
    onAddressChange: (String) -> Unit, onDateClick: () -> Unit, onSave: () -> Unit,
    onVerifyEmail: () -> Unit, onBack: () -> Unit, onContact: () -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Edit profile", "Only approved fields can be changed", onBack)
        if (!profile.canEditProfile) {
            Card { Column(Modifier.padding(16.dp)) { Text("Profile editing is restricted", fontWeight = FontWeight.Bold); Text("An administrator has disabled profile changes for this account."); TextButton(onClick = onContact) { Text("Contact support") } } }
        } else {
            OutlinedTextField(value = username, onValueChange = onUsernameChange, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth(), supportingText = { Text("3–40 characters. Choose a unique, appropriate username.") })
            OutlinedTextField(value = email, onValueChange = onEmailChange, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), trailingIcon = { Icon(Icons.Filled.Email, null) })
            if (email != profile.email.orEmpty()) OutlinedButton(onClick = onVerifyEmail, modifier = Modifier.fillMaxWidth()) { Text("Verify new email with OTP") }
            OutlinedTextField(value = dob.ifBlank { "Not set" }, onValueChange = {}, readOnly = true, enabled = !dobLocked, label = { Text("Date of birth") }, modifier = Modifier.fillMaxWidth(), trailingIcon = { IconButton(onClick = onDateClick, enabled = !dobLocked) { Icon(Icons.Filled.CalendarMonth, null) } }, supportingText = { Text(if (dobLocked) "Date of birth can only be set once." else "Choose carefully; it becomes locked after setting.") })
            OutlinedTextField(value = address, onValueChange = onAddressChange, label = { Text("Address") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            Text("Name, mobile number and gender are read-only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (saved) Text("Changes validated. Backend save will be connected in the next phase.", color = MaterialTheme.colorScheme.primary)
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Save changes") }
        }
    }
}

@Composable
internal fun ChangePasswordScreen(
    currentPassword: String, newPassword: String, confirmPassword: String, showCurrent: Boolean, showNew: Boolean,
    showConfirm: Boolean, error: String?, onCurrentChange: (String) -> Unit, onNewChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit, onToggleCurrent: () -> Unit, onToggleNew: () -> Unit,
    onToggleConfirm: () -> Unit, onForgot: () -> Unit, onBack: () -> Unit, onSubmit: () -> Unit,
) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Change password", "Use a strong password you have not used before", onBack)
        PasswordField("Current password", currentPassword, onCurrentChange, showCurrent, onToggleCurrent)
        PasswordField("New password", newPassword, onNewChange, showNew, onToggleNew)
        PasswordField("Confirm new password", confirmPassword, onConfirmChange, showConfirm, onToggleConfirm)
        Text("Use at least 8 characters. Passwords are never saved in this screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onForgot, modifier = Modifier.align(Alignment.End)) { Text("Forgot password?") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("Update password") }
    }
}

@Composable
internal fun DevicesScreen(devices: List<LocalDevice>, onBack: () -> Unit, onLogoutCurrent: () -> Unit, onLogoutOther: () -> Unit, onLogoutDevice: (LocalDevice) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Logged-in devices", "Sessions connected to your account", onBack)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Filled.Security, null); Column { Text("Protect your account", fontWeight = FontWeight.SemiBold); Text("Logging out other devices requires OTP verification.") } } }
        devices.forEach { device ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(if (device.current) Icons.Filled.PhoneAndroid else Icons.Filled.Devices, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) { Text(device.name, fontWeight = FontWeight.SemiBold); Text(device.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (device.current) AssistChip(onClick = {}, label = { Text("This device") }) }
                    if (device.current) TextButton(onClick = onLogoutCurrent) { Text("Log out") } else TextButton(onClick = { onLogoutDevice(device) }) { Text("Revoke") }
                }
            }
        }
        Button(onClick = onLogoutOther, modifier = Modifier.fillMaxWidth()) { Text("Log out all other devices") }
        Text("Device details and revocation actions will be loaded from the backend in the integration phase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SettingsScreen(
    themeMode: String, themeExpanded: Boolean, onThemeExpanded: (Boolean) -> Unit, onThemeSelected: (String) -> Unit,
    weekStartsMonday: Boolean, onWeekStartChanged: (Boolean) -> Unit, onAppLock: () -> Unit,
    onSetSalaryPin: () -> Unit, onDevices: () -> Unit, onSwitchAccount: () -> Unit, onSupport: () -> Unit, onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageHeader("Settings", "Make Poshak work the way you prefer", onBack)
        SectionTitle("Appearance")
        Card { Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("App theme", fontWeight = FontWeight.SemiBold); Text("Use system, light or dark appearance", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { onThemeExpanded(!themeExpanded) }) { Text(themeMode.replaceFirstChar { it.uppercase() }) } }
            if (themeExpanded) listOf("system", "light", "dark").forEach { mode -> Row(Modifier.fillMaxWidth().combinedClickable(onClick = { onThemeSelected(mode); onThemeExpanded(false) }).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = themeMode == mode, onClick = { onThemeSelected(mode); onThemeExpanded(false) }); Text(mode.replaceFirstChar { it.uppercase() }) } }
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Start week on Monday", fontWeight = FontWeight.Medium); Text("Turn off to start on Sunday", style = MaterialTheme.typography.bodySmall) }; Switch(checked = weekStartsMonday, onCheckedChange = onWeekStartChanged) }
        } }
        SectionTitle("Security")
        ActionRow(Icons.Filled.Fingerprint, "App lock", "Device credential or four-digit PIN", onAppLock)
        ActionRow(Icons.Filled.Lock, "Salary PIN", "Change the local PIN protecting salary", onSetSalaryPin)
        ActionRow(Icons.Filled.Devices, "Devices", "Manage active sessions", onDevices)
        SectionTitle("Accounts and help")
        ActionRow(Icons.Filled.SwapHoriz, "Switch account", "Switch or add another account", onSwitchAccount)
        ActionRow(Icons.AutoMirrored.Filled.HelpOutline, "Contact developer", "Open WhatsApp support", onSupport)
        Card { Column(Modifier.padding(14.dp)) { Text("Privacy", fontWeight = FontWeight.SemiBold); Text("Attendance and documents are sensitive screens. App-lock timeout and secure previews will be enforced as part of the platform integration.", style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
internal fun AppLockScreen(currentMode: String, isPrimary: Boolean, onBack: () -> Unit, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("App lock", "Protect attendance and documents", onBack)
        Text(if (isPrimary) "Primary account" else "Secondary account", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        LockOption("DEVICE", "Device lock", "Use Android system PIN, password or biometric", Icons.Filled.Fingerprint, currentMode == "DEVICE", enabled = isPrimary, onClick = { onSelect("DEVICE") })
        LockOption("PIN", "Four-digit PIN", "Set a PIN used when opening this account", Icons.Filled.Lock, currentMode == "PIN", enabled = true, onClick = { onSelect("PIN") })
        LockOption("NONE", "No lock", "Open without an app-specific lock", Icons.Filled.VerifiedUser, currentMode == "NONE", enabled = isPrimary, onClick = { onSelect("NONE") })
        Text(if (isPrimary) "The primary account defaults to Android device credentials." else "Secondary accounts default to a four-digit PIN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun SetPinScreen(pin: String, confirmPin: String, error: String?, title: String, onPinChange: (String) -> Unit, onConfirmChange: (String) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }; Spacer(Modifier.width(6.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
        Box(Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Text("Choose a four-digit PIN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Avoid easy-to-guess combinations such as 1234 or your birth year.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("New PIN", fontWeight = FontWeight.Medium); PinCodeInput(pin, onPinChange)
            Text("Confirm PIN", fontWeight = FontWeight.Medium); PinCodeInput(confirmPin, onConfirmChange)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text("Save PIN") }
    }
}

@Composable
internal fun SwitchAccountScreen(accounts: List<LocalAccount>, onBack: () -> Unit, onSwitch: (LocalAccount) -> Unit, onRemove: (LocalAccount) -> Unit, onLogout: () -> Unit) {
    var removeTarget by remember { mutableStateOf<LocalAccount?>(null) }
    if (removeTarget != null) AlertDialog(onDismissRequest = { removeTarget = null }, title = { Text("Remove secondary account?") }, text = { Text("Remove ${removeTarget?.label} from this device? The server session will also be revoked when connected.") }, confirmButton = { TextButton(onClick = { removeTarget?.let(onRemove); removeTarget = null }) { Text("Remove account") } }, dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageHeader("Switch account", "Choose an account on this device", onBack)
        accounts.forEach { account ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarMark("", account.label)
                    Column(Modifier.weight(1f)) { Text(account.label, fontWeight = FontWeight.SemiBold); Text(account.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); if (account.isPrimary) AssistChip(onClick = {}, label = { Text("Primary") }) }
                    if (account.label == "Add another account") IconButton(onClick = { onSwitch(account) }) { Icon(Icons.Filled.Add, "Add account") } else {
                        TextButton(onClick = { onSwitch(account) }) { Text("Open") }
                        if (!account.isPrimary) IconButton(onClick = { removeTarget = account }) { Icon(Icons.AutoMirrored.Filled.Logout, "Remove account") }
                    }
                }
            }
        }
        Text("The first account remains the permanent primary account. Secondary accounts can be removed by the primary account only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out current account") }
    }
}

@Composable
internal fun AvatarEditorScreen(currentAvatar: String, onBack: () -> Unit, onChoose: (String) -> Unit, onUpload: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader("Profile picture", "Choose an avatar or use a photo", onBack)
        Box(Modifier.size(130.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { AvatarContent(currentAvatar, "P", Modifier.fillMaxSize(), MaterialTheme.typography.displayMedium) }
        Text("Preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🌿", "🌸", "🌞", "🪷").forEach { symbol -> AssistChip(onClick = { onChoose(symbol) }, label = { Text(symbol, style = MaterialTheme.typography.titleLarge) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🌼", "🌺", "🌻", "🍀").forEach { symbol -> AssistChip(onClick = { onChoose(symbol) }, label = { Text(symbol, style = MaterialTheme.typography.titleLarge) }) } }
        }
        Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Choose photo and crop") }
        Text("Selected photos are center-cropped and resized before saving, so your picture fits the avatar.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { IconButton(onClick = onToggle) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password") } })
}

@Composable
internal fun OtpInput(value: String, onValueChange: (String) -> Unit) = PinCodeInput(value = value, onValueChange = onValueChange, length = 6)

@Composable
internal fun PinCodeInput(value: String, onValueChange: (String) -> Unit, length: Int = 4) {
    Box {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            textStyle = MaterialTheme.typography.titleLarge.copy(color = androidx.compose.ui.graphics.Color.Transparent),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            decorationBox = { inner ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(length) { index ->
                        val char = value.getOrNull(index)?.toString().orEmpty()
                        Box(Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer).border(1.dp, if (index == value.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Text(if (char.isNotEmpty()) "•" else "", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                inner()
            },
        )
    }
}

@Composable
private fun LockOption(key: String, title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(enabled = enabled, onClick = onClick), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String, editable: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        if (editable) Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ActionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer) }
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun PageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }

