@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.stkrfx.poshak.ui.workspace

import android.content.Intent
import android.app.KeyguardManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
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
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private const val DEVELOPER_WHATSAPP = "918709782649"

private enum class MainTab { HOME, DOCUMENTS, UPDATES, PROFILE }
private enum class ProfilePage { OVERVIEW, EDIT, CHANGE_PASSWORD, DEVICES, SETTINGS, APP_LOCK, SET_PIN, SWITCH_ACCOUNT, AVATAR, EMAIL_OTP, SALARY_PIN }

private data class LocalAccount(val label: String, val subtitle: String, val isPrimary: Boolean = false)
private data class LocalNotice(val title: String, val body: String, val date: String, val unread: Boolean = true)
private data class LocalDevice(val name: String, val detail: String, val current: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AuthenticatedWorkspace(
    api: PoshakApi,
    profile: EmployeeProfile,
    themeMode: String,
    onThemeModeChanged: (String) -> Unit,
    onSessionExpired: () -> Unit,
    onLogout: () -> Unit,
    onForceUpdate: (com.stkrfx.poshak.data.AppRelease) -> Unit,
) {
    val context = LocalContext.current
    val localPreferences = remember { context.getSharedPreferences("poshak_local_data", android.content.Context.MODE_PRIVATE) }
    val localVault = remember { com.stkrfx.poshak.data.LocalDeviceVault(context) }
    var latestProfile by remember { mutableStateOf(profile) }
    var profileRefreshing by rememberSaveable { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var profilePage by rememberSaveable { mutableStateOf(ProfilePage.OVERVIEW) }
    var showSwitchOrLogout by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var showBlockedDialog by remember { mutableStateOf(latestProfile.isBlocked) }
    var profileName by rememberSaveable { mutableStateOf(latestProfile.name) }
    var username by rememberSaveable { mutableStateOf(localPreferences.getString("profile_username", latestProfile.username) ?: latestProfile.username) }
    var email by rememberSaveable { mutableStateOf(localPreferences.getString("profile_email", latestProfile.email.orEmpty()) ?: latestProfile.email.orEmpty()) }
    var address by rememberSaveable { mutableStateOf(localPreferences.getString("profile_address", latestProfile.address.orEmpty()) ?: latestProfile.address.orEmpty()) }
    var dob by rememberSaveable { mutableStateOf(localPreferences.getString("profile_dob", latestProfile.dateOfBirth.orEmpty()) ?: latestProfile.dateOfBirth.orEmpty()) }
    var avatar by rememberSaveable { mutableStateOf(localPreferences.getString("profile_avatar", latestProfile.avatarUrl.orEmpty()) ?: latestProfile.avatarUrl.orEmpty()) }
    var emailPending by rememberSaveable { mutableStateOf("") }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileSaved by rememberSaveable { mutableStateOf(false) }
    var currentPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var showCurrentPassword by rememberSaveable { mutableStateOf(false) }
    var showNewPassword by rememberSaveable { mutableStateOf(false) }
    var showConfirmPassword by rememberSaveable { mutableStateOf(false) }
    var appLock by rememberSaveable { mutableStateOf(localPreferences.getString("app_lock", "DEVICE") ?: "DEVICE") }
    var appLockVerified by rememberSaveable { mutableStateOf(false) }
    var deviceAuthUnavailable by rememberSaveable { mutableStateOf(false) }
    var weekStartsMonday by rememberSaveable { mutableStateOf(localPreferences.getBoolean("week_starts_monday", true)) }
    var dobLocked by rememberSaveable { mutableStateOf(localPreferences.getBoolean("dob_locked", latestProfile.isDateOfBirthLocked)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAvatarActions by remember { mutableStateOf(false) }
    var showDeviceOtp by remember { mutableStateOf(false) }
    var pendingDeviceAction by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var appPin by rememberSaveable { mutableStateOf(localVault.getString("app_pin").orEmpty()) }
    var showAppUnlock by rememberSaveable { mutableStateOf(localPreferences.getBoolean("app_lock_enabled", true) && localPreferences.getString("app_lock", "DEVICE") == "PIN" && localVault.getString("app_pin").orEmpty().isNotBlank()) }
    var unlockAttempt by rememberSaveable { mutableStateOf("") }
    var unlockError by rememberSaveable { mutableStateOf<String?>(null) }
    var salarySecretPin by rememberSaveable { mutableStateOf(localVault.getString("salary_pin").orEmpty()) }
    var salaryPin by rememberSaveable { mutableStateOf("") }
    var salary by rememberSaveable { mutableStateOf(localPreferences.getString("monthly_salary", "").orEmpty()) }
    var salaryVisible by rememberSaveable { mutableStateOf(false) }
    var salaryPinConfigured by rememberSaveable { mutableStateOf(localPreferences.getBoolean("salary_pin_configured", false)) }
    var showSalaryDialog by remember { mutableStateOf(false) }
    var showSalaryUnlock by remember { mutableStateOf(false) }
    var salaryError by rememberSaveable { mutableStateOf<String?>(null) }
    var pinError by rememberSaveable { mutableStateOf<String?>(null) }
    var themeExpanded by rememberSaveable { mutableStateOf(false) }
    var noticeList = remember {
        mutableStateListOf(
            LocalNotice("Attendance updated", "Your attendance records are ready to view.", "Today"),
            LocalNotice("Profile reminder", "Keep your email address up to date for account recovery.", "Yesterday", false),
            LocalNotice("Welcome to Poshak", "Your attendance and documents will appear here.", "Earlier", false),
        )
    }
    val accounts = remember { mutableStateListOf(LocalAccount(latestProfile.name, "${latestProfile.username} · Current account", true), LocalAccount("Add another account", "Sign in to another account")) }
    val devices = remember {
        mutableStateListOf(
            LocalDevice("This device", "Android · Current session", true),
            LocalDevice("Android phone", "Last active recently"),
            LocalDevice("Other device", "Last active 3 days ago"),
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deviceLockLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        appLockVerified = result.resultCode == android.app.Activity.RESULT_OK
        deviceAuthUnavailable = !appLockVerified
    }
    androidx.compose.runtime.LaunchedEffect(appLock, appPin) {
        showAppUnlock = appLock == "PIN" && appPin.length == 4
        if (appLock == "DEVICE" && !appLockVerified) {
            val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as KeyguardManager
            if (keyguard.isDeviceSecure) {
                val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Poshak", "Confirm your device screen lock")
                if (intent != null) deviceLockLauncher.launch(intent) else deviceAuthUnavailable = true
            } else deviceAuthUnavailable = true
        }
        if (appLock == "NONE") { appLockVerified = true; showAppUnlock = false }
    }

    BackHandler(enabled = showSwitchOrLogout || confirmLogout || showDeviceOtp || showSalaryDialog || showSalaryUnlock || themeExpanded || profilePage != ProfilePage.OVERVIEW || selectedTab != MainTab.HOME) {
        when {
            showSwitchOrLogout -> showSwitchOrLogout = false
            confirmLogout -> confirmLogout = false
            showDeviceOtp -> showDeviceOtp = false
            showSalaryDialog -> showSalaryDialog = false
            showSalaryUnlock -> showSalaryUnlock = false
            themeExpanded -> themeExpanded = false
            profilePage != ProfilePage.OVERVIEW -> profilePage = ProfilePage.OVERVIEW
            selectedTab != MainTab.HOME -> selectedTab = MainTab.HOME
        }
    }
    androidx.compose.runtime.LaunchedEffect(selectedTab, profilePage) {
        if (selectedTab == MainTab.PROFILE) {
            profileRefreshing = true
            try {
                val fresh = api.fetchProfile()
                latestProfile = fresh
                profileName = fresh.name
                username = fresh.username
                email = fresh.email.orEmpty()
                address = fresh.address.orEmpty()
                dob = fresh.dateOfBirth.orEmpty()
                avatar = fresh.avatarUrl.orEmpty()
                dobLocked = fresh.isDateOfBirthLocked
                showBlockedDialog = fresh.isBlocked
            } catch (error: com.stkrfx.poshak.data.ApiException) {
                if (error.statusCode == 401 || error.apiCode == "ACCOUNT_BLOCKED") onSessionExpired()
            } catch (_: Exception) {
                // Keep the existing profile visible if a background refresh fails.
            } finally {
                profileRefreshing = false
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(appLock, appPin, weekStartsMonday, dobLocked, salary, salarySecretPin, salaryPinConfigured, username, email, address, dob, avatar) {
        localPreferences.edit()
            .putString("app_lock", appLock)
            .putBoolean("app_lock_enabled", appLock != "NONE")
            .putBoolean("week_starts_monday", weekStartsMonday)
            .putBoolean("dob_locked", dobLocked)
            .putString("monthly_salary", salary)
            .putBoolean("salary_pin_configured", salaryPinConfigured)
            .putString("profile_username", username)
            .putString("profile_email", email)
            .putString("profile_address", address)
            .putString("profile_dob", dob)
            .putString("profile_avatar", avatar)
            .apply()
        localVault.putString("app_pin", appPin)
        localVault.putString("salary_pin", salarySecretPin)
    }
    if (appLock == "DEVICE" && !appLockVerified && deviceAuthUnavailable) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Device lock unavailable") },
            text = { Text("Set a screen lock in Android Settings, then reopen Poshak. Device lock cannot be replaced by your app PIN.") },
            confirmButton = { TextButton(onClick = {
                val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as KeyguardManager
                val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Poshak", "Confirm your device screen lock")
                if (intent != null) { deviceAuthUnavailable = false; deviceLockLauncher.launch(intent) }
            }) { Text("Try again") } },
            dismissButton = { TextButton(onClick = onLogout) { Text("Log out") } },
        )
    }
    if (showAppUnlock && appLock == "PIN") {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            title = { Text("Unlock Poshak") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your four-digit app PIN to open attendance and documents.")
                    PinCodeInput(value = unlockAttempt, onValueChange = { unlockAttempt = it; unlockError = null })
                    unlockError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (unlockAttempt == appPin && appPin.length == 4) {
                        showAppUnlock = false
                        unlockAttempt = ""
                        unlockError = null
                    } else unlockError = "That PIN is incorrect."
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = onLogout) { Text("Log out") } },
        )
    }

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = { Icon(Icons.Filled.Security, contentDescription = null) },
            title = { Text("Account access restricted") },
            text = { Text(latestProfile.blockedReason ?: "This account has been blocked by an administrator. Contact support or switch to another account.") },
            confirmButton = { TextButton(onClick = { showBlockedDialog = false; openWhatsApp(context) }) { Text("Contact support") } },
            dismissButton = { TextButton(onClick = { showBlockedDialog = false; profilePage = ProfilePage.SWITCH_ACCOUNT; selectedTab = MainTab.PROFILE }) { Text("Switch account") } },
        )
    }

    if (showSwitchOrLogout) {
        AlertDialog(
            onDismissRequest = { showSwitchOrLogout = false },
            title = { Text("Account options") },
            text = { Text("Switch to another account or sign out of this device.") },
            confirmButton = { TextButton(onClick = { showSwitchOrLogout = false; profilePage = ProfilePage.SWITCH_ACCOUNT; selectedTab = MainTab.PROFILE }) { Text("Switch account") } },
            dismissButton = { TextButton(onClick = { showSwitchOrLogout = false; confirmLogout = true }) { Text("Log out") } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out of this device?") },
            text = { Text("Your local session will be cleared. You can sign in again at any time.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }
    if (showDatePicker) {
        val initialMillis = runCatching { LocalDate.parse(dob).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }.getOrNull()
        val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        dob = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        dobLocked = true
                    }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = dateState) }
    }
    if (showAvatarActions) {
        AlertDialog(
            onDismissRequest = { showAvatarActions = false },
            title = { Text("Profile picture") },
            text = { Text("Choose a generated avatar or select a picture from your device. Cropping and resizing controls are shown in the next step.") },
            confirmButton = { TextButton(onClick = { avatar = listOf("🌿", "🌸", "🌞", "🪷", "🌼", "🌺").random(); showAvatarActions = false; profileSaved = true }) { Text("Generate random") } },
            dismissButton = { TextButton(onClick = { showAvatarActions = false; profilePage = ProfilePage.AVATAR }) { Text("Choose / crop photo") } },
        )
    }
    if (showDeviceOtp) {
        AlertDialog(
            onDismissRequest = { showDeviceOtp = false; otp = "" },
            title = { Text("Verify it’s you") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter the one-time code to ${if (pendingDeviceAction == "all") "log out other devices" else "manage this device"}. This screen is ready for backend OTP integration.")
                    OtpInput(value = otp, onValueChange = { otp = it })
                }
            },
            confirmButton = { TextButton(onClick = { if (otp.length == 6) { showDeviceOtp = false; otp = ""; scope.launch { snackbarHostState.showSnackbar("Verification UI complete — server action will be connected in the backend phase.") } } }) { Text("Verify") } },
            dismissButton = { TextButton(onClick = { showDeviceOtp = false; otp = "" }) { Text("Cancel") } },
        )
    }
    if (showSalaryDialog) {
        AlertDialog(
            onDismissRequest = { showSalaryDialog = false; salaryError = null },
            title = { Text(if (salary.isBlank()) "Set monthly salary" else "Update monthly salary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stored only on this device. Enter 4–5 digits, without decimals.", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it.filter(Char::isDigit).take(5); salaryError = null },
                        label = { Text("Monthly salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = salaryError != null,
                    )
                    salaryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = {
                if (salary.length !in 4..5) salaryError = "Enter an amount containing 4 or 5 digits."
                else { showSalaryDialog = false; if (!salaryPinConfigured) { profilePage = ProfilePage.SALARY_PIN; selectedTab = MainTab.PROFILE } else scope.launch { snackbarHostState.showSnackbar("Salary updated locally.") } }
            }) { Text("Save amount") } },
            dismissButton = { TextButton(onClick = { showSalaryDialog = false; salaryError = null }) { Text("Cancel") } },
        )
    }
    if (showSalaryUnlock) {
        AlertDialog(
            onDismissRequest = { showSalaryUnlock = false; salaryPin = "" },
            title = { Text("Unlock salary") },
            text = { Column { Text("Enter your salary PIN to reveal the amount."); Spacer(Modifier.height(12.dp)); PinCodeInput(value = salaryPin, onValueChange = { salaryPin = it }) } },
            confirmButton = { TextButton(onClick = { if (salaryPin.length == 4 && salaryPin == salarySecretPin) { salaryVisible = true; showSalaryUnlock = false; salaryPin = "" } else salaryError = "Incorrect PIN. Try again." }) { Text("Unlock") } },
            dismissButton = { TextButton(onClick = { showSalaryUnlock = false; salaryPin = "" }) { Text("Cancel") } },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (profilePage == ProfilePage.OVERVIEW && selectedTab != MainTab.PROFILE || selectedTab == MainTab.PROFILE && profilePage == ProfilePage.OVERVIEW) {
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == MainTab.HOME, onClick = { selectedTab = MainTab.HOME; profilePage = ProfilePage.OVERVIEW }, icon = { Icon(Icons.Filled.Home, contentDescription = null) }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedTab == MainTab.DOCUMENTS, onClick = { selectedTab = MainTab.DOCUMENTS; profilePage = ProfilePage.OVERVIEW }, icon = { Icon(Icons.Filled.Description, contentDescription = null) }, label = { Text("Documents") })
                    NavigationBarItem(selected = selectedTab == MainTab.UPDATES, onClick = { selectedTab = MainTab.UPDATES; profilePage = ProfilePage.OVERVIEW }, icon = {
                        Box {
                            Icon(Icons.Filled.Notifications, contentDescription = null)
                            if (noticeList.any { it.unread }) Box(Modifier.align(Alignment.TopEnd).padding(top = 1.dp, end = 1.dp).size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                        }
                    }, label = { Text("Updates") })
                    NavigationBarItem(
                        selected = selectedTab == MainTab.PROFILE,
                        onClick = { selectedTab = MainTab.PROFILE; profilePage = ProfilePage.OVERVIEW },
                        modifier = Modifier.combinedClickable(onClick = { selectedTab = MainTab.PROFILE; profilePage = ProfilePage.OVERVIEW }, onLongClick = { showSwitchOrLogout = true }),
                        icon = { AvatarMark(avatar, profileName) },
                        label = { Text("Profile") },
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        Box(Modifier.fillMaxSize().padding(scaffoldPadding)) {
            when {
                selectedTab == MainTab.HOME -> HomeScreen(
                    api = api,
                    profile = latestProfile,
                    onSessionExpired = onSessionExpired,
                    onLogout = onLogout,
                    onForceUpdate = onForceUpdate,
                    modifier = Modifier.fillMaxSize(),
                    salaryLabel = if (salary.isBlank()) null else if (salaryVisible) "₹$salary" else "₹ ••••",
                    onSalaryClick = { if (salary.isBlank()) showSalaryDialog = true else showSalaryUnlock = true },
                    onSalaryLongPress = { if (salary.isNotBlank()) showSalaryDialog = true },
                    weekStartsMonday = weekStartsMonday,
                )
                selectedTab == MainTab.DOCUMENTS -> DocumentsScreen(
                    onBack = { selectedTab = MainTab.HOME },
                    canUpload = latestProfile.canUploadDocuments,
                    onMessage = { scope.launch { snackbarHostState.showSnackbar(it) } },
                )
                selectedTab == MainTab.UPDATES -> UpdatesScreen(
                    notices = noticeList,
                    onMarkAllRead = { noticeList.indices.forEach { index -> noticeList[index] = noticeList[index].copy(unread = false) } },
                    onOpenNotice = { notice -> noticeList.indexOfFirst { it.title == notice.title && it.date == notice.date }.takeIf { it >= 0 }?.let { index -> noticeList[index] = noticeList[index].copy(unread = false) }; scope.launch { snackbarHostState.showSnackbar(notice.body) } },
                )
                else -> when (profilePage) {
                    ProfilePage.OVERVIEW -> ProfileOverviewScreen(
                        profile = profile,
                        displayName = profileName,
                        username = username,
                        email = email,
                        address = address,
                        dob = dob,
                        avatar = avatar,
                        dobLocked = dobLocked,
                        canEdit = latestProfile.canEditProfile,
                        onEdit = { profileError = null; profileSaved = false; profilePage = ProfilePage.EDIT },
                        onAvatar = { showAvatarActions = true },
                        onChangePassword = { currentPassword = ""; newPassword = ""; confirmPassword = ""; profilePage = ProfilePage.CHANGE_PASSWORD },
                        onDevices = { profilePage = ProfilePage.DEVICES },
                        onSettings = { profilePage = ProfilePage.SETTINGS },
                        onSwitchAccount = { profilePage = ProfilePage.SWITCH_ACCOUNT },
                        onLogout = { confirmLogout = true },
                        onSupport = { openWhatsApp(context) },
                    )
                    ProfilePage.EDIT -> EditProfileScreen(
                        profile = profile,
                        username = username,
                        email = email,
                        address = address,
                        dob = dob,
                        dobLocked = dobLocked,
                        error = profileError,
                        saved = profileSaved,
                        onUsernameChange = { username = it.take(40); profileError = null },
                        onEmailChange = { email = it.take(254); profileError = null },
                        onAddressChange = { address = it.take(250); profileError = null },
                        onDateClick = { if (!dobLocked) showDatePicker = true },
                        onSave = {
                            when {
                                username.trim().length < 3 -> profileError = "Username must be at least 3 characters."
                                email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> profileError = "Enter a valid email address."
                                else -> { profileSaved = true; profileError = null; scope.launch { snackbarHostState.showSnackbar("Profile changes saved locally for UI preview.") } }
                            }
                        },
                        onVerifyEmail = { emailPending = email; profilePage = ProfilePage.EMAIL_OTP },
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                        onContact = { openWhatsApp(context) },
                    )
                    ProfilePage.EMAIL_OTP -> EmailVerificationScreen(email = emailPending, value = otp, onValueChange = { otp = it }, onVerify = { if (otp.length == 6) { email = emailPending; otp = ""; profilePage = ProfilePage.EDIT; scope.launch { snackbarHostState.showSnackbar("Email verification UI complete; backend verification comes later.") } } }, onResend = { otp = ""; scope.launch { snackbarHostState.showSnackbar("A new code can be requested when email OTP is connected.") } }, onBack = { profilePage = ProfilePage.EDIT })
                    ProfilePage.CHANGE_PASSWORD -> ChangePasswordScreen(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmPassword = confirmPassword,
                        showCurrent = showCurrentPassword,
                        showNew = showNewPassword,
                        showConfirm = showConfirmPassword,
                        error = profileError,
                        onCurrentChange = { currentPassword = it.take(72); profileError = null },
                        onNewChange = { newPassword = it.take(72); profileError = null },
                        onConfirmChange = { confirmPassword = it.take(72); profileError = null },
                        onToggleCurrent = { showCurrentPassword = !showCurrentPassword },
                        onToggleNew = { showNewPassword = !showNewPassword },
                        onToggleConfirm = { showConfirmPassword = !showConfirmPassword },
                        onForgot = { profilePage = ProfilePage.EMAIL_OTP; emailPending = email },
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                        onSubmit = {
                            profileError = when {
                                currentPassword.isBlank() -> "Enter your current password."
                                newPassword.length < 8 -> "New password must contain at least 8 characters."
                                newPassword.toByteArray().size > 72 -> "Password is too long."
                                newPassword != confirmPassword -> "The passwords do not match."
                                currentPassword == newPassword -> "Choose a password different from your current one."
                                else -> null
                            }
                            if (profileError == null) { currentPassword = ""; newPassword = ""; confirmPassword = ""; scope.launch { snackbarHostState.showSnackbar("Password form validated. Backend update will be wired later.") }; profilePage = ProfilePage.OVERVIEW }
                        },
                    )
                    ProfilePage.DEVICES -> DevicesScreen(
                        devices = devices,
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                        onLogoutCurrent = { confirmLogout = true },
                        onLogoutOther = { pendingDeviceAction = "all"; showDeviceOtp = true },
                        onLogoutDevice = { device -> pendingDeviceAction = device.name; showDeviceOtp = true },
                    )
                    ProfilePage.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        themeExpanded = themeExpanded,
                        onThemeExpanded = { themeExpanded = it },
                        onThemeSelected = onThemeModeChanged,
                        weekStartsMonday = weekStartsMonday,
                        onWeekStartChanged = { weekStartsMonday = it },
                        onAppLock = { profilePage = ProfilePage.APP_LOCK },
                        onSetSalaryPin = { profilePage = ProfilePage.SALARY_PIN },
                        onDevices = { profilePage = ProfilePage.DEVICES },
                        onSwitchAccount = { profilePage = ProfilePage.SWITCH_ACCOUNT },
                        onSupport = { openWhatsApp(context) },
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                    )
                    ProfilePage.APP_LOCK -> AppLockScreen(
                        currentMode = appLock,
                        isPrimary = true,
                        onBack = { profilePage = ProfilePage.SETTINGS },
                        onSelect = {
                            appLock = it
                            appLockVerified = false
                            deviceAuthUnavailable = false
                            if (it == "PIN") {
                                pin = ""; confirmPin = ""; profilePage = ProfilePage.SET_PIN
                            } else {
                                localPreferences.edit().putString("app_lock", it).putBoolean("app_lock_enabled", it != "NONE").apply()
                                if (it == "DEVICE") {
                                    val keyguard = context.getSystemService(android.content.Context.KEYGUARD_SERVICE) as KeyguardManager
                                    val intent = keyguard.createConfirmDeviceCredentialIntent("Unlock Poshak", "Confirm your device screen lock")
                                    if (intent != null) deviceLockLauncher.launch(intent) else deviceAuthUnavailable = true
                                }
                            }
                        },
                    )
                    ProfilePage.SET_PIN -> SetPinScreen(
                        pin = pin,
                        confirmPin = confirmPin,
                        error = pinError,
                        title = "Set app PIN",
                        onPinChange = { pin = it; pinError = null },
                        onConfirmChange = { confirmPin = it; pinError = null },
                        onBack = { profilePage = ProfilePage.APP_LOCK },
                        onSave = { if (pin.length != 4 || confirmPin.length != 4) pinError = "Enter a four-digit PIN in both fields." else if (pin != confirmPin) pinError = "The PINs do not match." else { appPin = pin; appLock = "PIN"; appLockVerified = false; localPreferences.edit().putString("app_lock", "PIN").putBoolean("app_lock_enabled", true).apply(); pin = ""; confirmPin = ""; pinError = null; profilePage = ProfilePage.SETTINGS; scope.launch { snackbarHostState.showSnackbar("App PIN saved.") } } },
                    )
                    ProfilePage.SALARY_PIN -> SetPinScreen(
                        pin = pin,
                        confirmPin = confirmPin,
                        error = pinError,
                        title = "Set salary PIN",
                        onPinChange = { pin = it; pinError = null },
                        onConfirmChange = { confirmPin = it; pinError = null },
                        onBack = { profilePage = ProfilePage.SETTINGS },
                        onSave = { if (pin.length != 4 || confirmPin.length != 4) pinError = "Enter a four-digit PIN in both fields." else if (pin != confirmPin) pinError = "The PINs do not match." else { salarySecretPin = pin; salaryPinConfigured = true; pin = ""; confirmPin = ""; pinError = null; profilePage = ProfilePage.OVERVIEW; selectedTab = MainTab.HOME; scope.launch { snackbarHostState.showSnackbar("Salary PIN configured locally.") } } },
                    )
                    ProfilePage.SWITCH_ACCOUNT -> SwitchAccountScreen(
                        accounts = accounts,
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                        onSwitch = { account -> if (account.label == "Add another account") { accounts.add(0, LocalAccount("Secondary account", "PIN lock required · Local UI preview")); profilePage = ProfilePage.SET_PIN; appLock = "PIN" } else if (account.isPrimary) { profilePage = ProfilePage.OVERVIEW; selectedTab = MainTab.HOME } else { profilePage = ProfilePage.SET_PIN; appLock = "PIN" } },
                        onRemove = { account -> if (!account.isPrimary && account.label != "Add another account") accounts.remove(account) },
                        onLogout = { confirmLogout = true },
                    )
                    ProfilePage.AVATAR -> AvatarEditorScreen(
                        currentAvatar = avatar,
                        onBack = { profilePage = ProfilePage.OVERVIEW },
                        onChoose = { avatar = it; profilePage = ProfilePage.OVERVIEW; scope.launch { snackbarHostState.showSnackbar("Avatar selected for UI preview.") } },
                        onUpload = { scope.launch { snackbarHostState.showSnackbar("Photo picker and image cropper will be wired in the Android integration pass.") } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileOverviewScreen(
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
                    Text(avatar.ifBlank { displayName.firstOrNull()?.uppercase() ?: "P" }, style = MaterialTheme.typography.headlineLarge)
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
        ActionRow(Icons.Filled.HelpOutline, "Contact developer", "Get help on WhatsApp", onSupport)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Logout, null); Spacer(Modifier.width(8.dp)); Text("Log out") }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun EditProfileScreen(
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
private fun ChangePasswordScreen(
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
private fun DevicesScreen(devices: List<LocalDevice>, onBack: () -> Unit, onLogoutCurrent: () -> Unit, onLogoutOther: () -> Unit, onLogoutDevice: (LocalDevice) -> Unit) {
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
private fun SettingsScreen(
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
        ActionRow(Icons.Filled.HelpOutline, "Contact developer", "Open WhatsApp support", onSupport)
        Card { Column(Modifier.padding(14.dp)) { Text("Privacy", fontWeight = FontWeight.SemiBold); Text("Attendance and documents are sensitive screens. App-lock timeout and secure previews will be enforced as part of the platform integration.", style = MaterialTheme.typography.bodySmall) } }
    }
}

@Composable
private fun AppLockScreen(currentMode: String, isPrimary: Boolean, onBack: () -> Unit, onSelect: (String) -> Unit) {
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
private fun SetPinScreen(pin: String, confirmPin: String, error: String?, title: String, onPinChange: (String) -> Unit, onConfirmChange: (String) -> Unit, onBack: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Spacer(Modifier.width(6.dp)); Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
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
private fun SwitchAccountScreen(accounts: List<LocalAccount>, onBack: () -> Unit, onSwitch: (LocalAccount) -> Unit, onRemove: (LocalAccount) -> Unit, onLogout: () -> Unit) {
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
                        if (!account.isPrimary) IconButton(onClick = { removeTarget = account }) { Icon(Icons.Filled.Logout, "Remove account") }
                    }
                }
            }
        }
        Text("The first account remains the permanent primary account. Secondary accounts can be removed by the primary account only.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("Log out current account") }
    }
}

@Composable
private fun AvatarEditorScreen(currentAvatar: String, onBack: () -> Unit, onChoose: (String) -> Unit, onUpload: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PageHeader("Profile picture", "Choose an avatar or use a photo", onBack)
        Box(Modifier.size(130.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(currentAvatar.ifBlank { "P" }, style = MaterialTheme.typography.displayMedium) }
        Text("Preview", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🌿", "🌸", "🌞", "🪷").forEach { symbol -> AssistChip(onClick = { onChoose(symbol) }, label = { Text(symbol, style = MaterialTheme.typography.titleLarge) }) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("🌼", "🌺", "🌻", "🍀").forEach { symbol -> AssistChip(onClick = { onChoose(symbol) }, label = { Text(symbol, style = MaterialTheme.typography.titleLarge) }) } }
        }
        Button(onClick = onUpload, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(8.dp)); Text("Choose photo and crop") }
        Text("Photo selection, crop, resize and image upload will be connected after the UI is approved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DocumentsScreen(onBack: () -> Unit, canUpload: Boolean, onMessage: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Documents", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your documents will be available here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().padding(top = 34.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Description, null, Modifier.size(46.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) }
                Text("Coming soon", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("We’re preparing a secure place for your documents.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (canUpload) OutlinedButton(onClick = { onMessage("Document upload will be enabled when the document service is connected.") }) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp)); Text("Upload document") }
            }
        }
    }
}

@Composable
private fun UpdatesScreen(notices: List<LocalNotice>, onMarkAllRead: () -> Unit, onOpenNotice: (LocalNotice) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Updates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Notifications and account updates", color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = onMarkAllRead, enabled = notices.any { it.unread }) { Text("Mark all read") } }
        if (notices.isEmpty()) EmptyState("You're all caught up", "New notifications will appear here.", Icons.Filled.Notifications)
        notices.forEach { notice ->
            Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpenNotice(notice) }, onLongClick = { onOpenNotice(notice) }), colors = CardDefaults.cardColors(containerColor = if (notice.unread) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 5.dp).size(8.dp).clip(CircleShape).background(if (notice.unread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(notice.title, modifier = Modifier.weight(1f), fontWeight = if (notice.unread) FontWeight.Bold else FontWeight.Medium); Text(notice.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text(notice.body, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        Text("Notifications shown here are preview data until the notifications API is connected.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmailVerificationScreen(email: String, value: String, onValueChange: (String) -> Unit, onVerify: () -> Unit, onResend: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PageHeader("Verify email", "Enter the six-digit code", onBack)
        Box(Modifier.size(82.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Email, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer) }
        Text("Check your inbox", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("We’ll verify $email using a one-time code.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OtpInput(value, onValueChange)
        Button(onClick = onVerify, enabled = value.length == 6, modifier = Modifier.fillMaxWidth()) { Text("Verify email") }
        TextButton(onClick = onResend) { Text("Resend code") }
    }
}

@Composable
private fun PasswordField(label: String, value: String, onValueChange: (String) -> Unit, visible: Boolean, onToggle: () -> Unit) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = { IconButton(onClick = onToggle) { Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Hide password" else "Show password") } })
}

@Composable
private fun OtpInput(value: String, onValueChange: (String) -> Unit) = PinCodeInput(value = value, onValueChange = onValueChange, length = 6)

@Composable
private fun PinCodeInput(value: String, onValueChange: (String) -> Unit, length: Int = 4) {
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
private fun PageHeader(title: String, subtitle: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SectionTitle(title: String) { Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)) }

@Composable
private fun AvatarMark(avatar: String, label: String) {
    Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Text(avatar.ifBlank { label.firstOrNull()?.uppercase() ?: "P" }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer) }
}

@Composable
private fun EmptyState(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun openWhatsApp(context: android.content.Context) {
    val uri = Uri.parse("https://wa.me/$DEVELOPER_WHATSAPP")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}
