package com.stkrfx.poshak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.stkrfx.poshak.data.PoshakApi
import com.stkrfx.poshak.ui.theme.PoshakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val api = remember { PoshakApi(context.applicationContext) }
            val preferences = remember { context.getSharedPreferences("poshak_preferences", android.content.Context.MODE_PRIVATE) }
            var themeMode by rememberSaveable { mutableStateOf(preferences.getString("theme_mode", "system") ?: "system") }
            PoshakTheme(themeMode = themeMode, dynamicColor = true) {
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
