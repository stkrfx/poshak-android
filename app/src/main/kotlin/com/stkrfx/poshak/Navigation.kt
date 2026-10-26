package com.stkrfx.poshak

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.stkrfx.poshak.data.ApiException
import com.stkrfx.poshak.data.AppRelease
import com.stkrfx.poshak.data.EmployeeProfile
import com.stkrfx.poshak.data.PoshakApi
import com.stkrfx.poshak.ui.auth.EmailOtpScreen
import com.stkrfx.poshak.ui.auth.ForgotPasswordScreen
import com.stkrfx.poshak.ui.auth.LoginScreen
import com.stkrfx.poshak.ui.auth.ResetPasswordScreen
import com.stkrfx.poshak.ui.home.HomeScreen
import kotlinx.coroutines.launch

@Composable
fun PoshakApp(api: PoshakApi) {
    val backStack = rememberNavBackStack(StartupRoute)
    var profile by remember { mutableStateOf<EmployeeProfile?>(null) }
    var resetEmail by rememberSaveable { mutableStateOf("") }
    var resetOtp by rememberSaveable { mutableStateOf("") }
    var startupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var requiredRelease by remember { mutableStateOf<AppRelease?>(null) }
    var optionalRelease by remember { mutableStateOf<AppRelease?>(null) }
    var updateDownloadError by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val requireUpdate: (AppRelease) -> Unit = { release ->
        requiredRelease = release
        optionalRelease = null
        updateDownloadError = null
        backStack.clear()
        backStack.add(ForceUpdateRoute)
    }

    LaunchedEffect(Unit) {
        // Check version policy before restoring a session or exposing any protected screen.
        try {
            val updateStatus = api.checkAppVersion()
            if (updateStatus.forceUpdateRequired) {
                val release = updateStatus.requiredRelease ?: updateStatus.latestRelease
                if (release != null) {
                    requireUpdate(release)
                    return@LaunchedEffect
                }
            }
            val newest = updateStatus.latestRelease
            if (newest != null && newest.versionCode > BuildConfig.APP_VERSION_CODE && !newest.isForceUpdate) {
                optionalRelease = newest
            }
        } catch (error: ApiException) {
            if (error.apiCode == "APP_UPDATE_REQUIRED" && error.appRelease != null) {
                requireUpdate(error.appRelease)
                return@LaunchedEffect
            }
            // If the version service is briefly unavailable, the actual API still enforces
            // mandatory versions. Do not strand the user on an empty startup screen.
        } catch (_: Exception) {
            // Login and normal API calls will display their own connectivity errors.
        }

        try {
            val restoredProfile = api.restoreSession()
            if (restoredProfile != null) {
                profile = restoredProfile
                backStack.clear()
                backStack.add(HomeRoute)
            } else {
                backStack.clear()
                backStack.add(LoginRoute)
            }
        } catch (exception: ApiException) {
            if (exception.apiCode == "APP_UPDATE_REQUIRED" && exception.appRelease != null) {
                requireUpdate(exception.appRelease)
            } else {
                startupMessage = exception.message
                backStack.clear()
                backStack.add(LoginRoute)
            }
        } catch (_: Exception) {
            startupMessage = "Could not reconnect to Poshak. Please sign in again."
            backStack.clear()
            backStack.add(LoginRoute)
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1 && backStack.lastOrNull() != ForceUpdateRoute) backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<StartupRoute> { StartupScreen() }
                entry<LoginRoute> {
                    LoginScreen(
                        api = api,
                        initialMessage = startupMessage,
                        onLoginSuccess = { signedInProfile ->
                            profile = signedInProfile
                            startupMessage = null
                            backStack.clear()
                            backStack.add(HomeRoute)
                        },
                        onForgotPassword = { backStack.add(ForgotPasswordRoute) },
                        onForceUpdate = requireUpdate,
                    )
                }
                entry<ForgotPasswordRoute> {
                    ForgotPasswordScreen(
                        api = api,
                        onBack = { backStack.removeLastOrNull() },
                        onOtpSent = { email ->
                            resetEmail = email
                            resetOtp = ""
                            backStack.add(EmailOtpRoute)
                        },
                        onForceUpdate = requireUpdate,
                    )
                }
                entry<EmailOtpRoute> {
                    EmailOtpScreen(
                        api = api,
                        email = resetEmail,
                        onBack = { backStack.removeLastOrNull() },
                        onContinue = { otp ->
                            resetOtp = otp
                            backStack.add(ResetPasswordRoute)
                        },
                        onForceUpdate = requireUpdate,
                    )
                }
                entry<ResetPasswordRoute> {
                    ResetPasswordScreen(
                        api = api,
                        email = resetEmail,
                        otp = resetOtp,
                        onBack = { backStack.removeLastOrNull() },
                        onPasswordReset = {
                            profile = null
                            backStack.clear()
                            backStack.add(LoginRoute)
                        },
                        onForceUpdate = requireUpdate,
                    )
                }
                entry<HomeRoute> {
                    val signedInProfile = profile
                    if (signedInProfile == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        HomeScreen(
                            api = api,
                            profile = signedInProfile,
                            onSessionExpired = {
                                profile = null
                                backStack.clear()
                                backStack.add(LoginRoute)
                            },
                            onLogout = {
                                // A failed network logout still clears local credentials in the API layer.
                                coroutineScope.launch {
                                    try {
                                        api.logoutCurrentDevice()
                                    } catch (_: Exception) {
                                        // Local credentials are cleared by logoutCurrentDevice's finally block.
                                    } finally {
                                        profile = null
                                        backStack.clear()
                                        backStack.add(LoginRoute)
                                    }
                                }
                            },
                            onForceUpdate = requireUpdate,
                        )
                    }
                }
                entry<ForceUpdateRoute> {
                    val release = requiredRelease
                    if (release == null) {
                        StartupScreen()
                    } else {
                        RequiredUpdateScreen(
                            release = release,
                            errorMessage = updateDownloadError,
                            onDownload = {
                                updateDownloadError = if (openReleaseDownload(context, release)) null
                                else "Could not open the download link. Check the APK URL and try again."
                            },
                        )
                    }
                }
            },
        )

        optionalRelease?.let { release ->
            AlertDialog(
                onDismissRequest = { optionalRelease = null },
                title = { Text("Poshak update available") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Version ${release.versionName} is available.")
                        if (release.releaseNotes.isNotBlank()) Text(release.releaseNotes)
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (openReleaseDownload(context, release)) optionalRelease = null
                    }) { Text("Update") }
                },
                dismissButton = {
                    TextButton(onClick = { optionalRelease = null }) { Text("Not now") }
                },
            )
        }
    }
}

@Composable
private fun StartupScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Preparing Poshak…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RequiredUpdateScreen(
    release: AppRelease,
    errorMessage: String?,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Update required", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text("Poshak ${release.versionName} must be installed before you can continue.", modifier = Modifier.padding(top = 16.dp))
        if (release.releaseNotes.isNotBlank()) {
            Text(release.releaseNotes, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (errorMessage != null) {
            Text(errorMessage, modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.error)
        }
        Button(onClick = onDownload, modifier = Modifier.padding(top = 24.dp)) { Text("Download required update") }
    }
}

private fun openReleaseDownload(context: Context, release: AppRelease): Boolean {
    val uri = runCatching { Uri.parse(release.apkDownloadUrl) }.getOrNull() ?: return false
    if (uri.scheme != "https" || uri.host.isNullOrBlank()) return false
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
