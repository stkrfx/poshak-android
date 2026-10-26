package com.stkrfx.poshak.ui.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.ContextWrapper

/** Hides protected app content from screenshots and Android Recents snapshots while enabled. */
@Composable
fun SensitiveWindowProtection(enabled: Boolean) {
    val context = LocalContext.current
    val activity = context.findActivity() ?: return
    DisposableEffect(activity, enabled) {
        val wasAlreadySecure = activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        if (enabled) activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else if (!wasAlreadySecure) activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

        onDispose {
            if (enabled && !wasAlreadySecure) {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
