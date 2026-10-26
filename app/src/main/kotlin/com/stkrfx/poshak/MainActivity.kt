package com.stkrfx.poshak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.stkrfx.poshak.data.PoshakApi
import com.stkrfx.poshak.ui.theme.PoshakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val api = remember { PoshakApi(context.applicationContext) }
            val preferences = remember {
                context.getSharedPreferences("poshak_preferences", android.content.Context.MODE_PRIVATE)
            }
            var themeMode by rememberSaveable {
                mutableStateOf(preferences.getString("theme_mode", "system") ?: "system")
            }
            val darkTheme = when (themeMode.lowercase()) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            PoshakTheme(themeMode = themeMode, dynamicColor = true) {
                val systemBarColor = MaterialTheme.colorScheme.background

                // Match the system bars to the active theme and use the correct icon contrast.
                // The decor background also fills the inset areas on edge-to-edge Android versions.
                SideEffect {
                    val color = systemBarColor.toArgb()
                    window.decorView.setBackgroundColor(color)
                    @Suppress("DEPRECATION")
                    run {
                        window.statusBarColor = color
                        window.navigationBarColor = color
                    }
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                // Apply system-bar and display-cutout insets once at the app root.
                // This keeps every route (including login and startup) clear of the
                // status bar and the three-button/gesture navigation area.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    PoshakApp(
                        api = api,
                        themeMode = themeMode,
                        onThemeModeChanged = { mode ->
                            themeMode = mode
                            preferences.edit().putString("theme_mode", mode).apply()
                        },
                    )
                }
            }
        }
    }
}
