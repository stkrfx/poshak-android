package com.stkrfx.poshak.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// A deliberate dark palette keeps text, surfaces, borders and input fields readable
// regardless of the wallpaper palette configured on the device.
private val PoshakDarkColors = darkColorScheme(
    primary = Color(0xFF9BD7B2),
    onPrimary = Color(0xFF003920),
    primaryContainer = Color(0xFF155234),
    onPrimaryContainer = Color(0xFFB7F3CA),
    secondary = Color(0xFFB6CCBA),
    onSecondary = Color(0xFF223529),
    secondaryContainer = Color(0xFF394D3E),
    onSecondaryContainer = Color(0xFFD2E8D7),
    tertiary = Color(0xFFA3CEDC),
    onTertiary = Color(0xFF003640),
    tertiaryContainer = Color(0xFF1F4C58),
    onTertiaryContainer = Color(0xFFBFE9F7),
    background = Color(0xFF101410),
    onBackground = Color(0xFFE1E5DF),
    surface = Color(0xFF101410),
    onSurface = Color(0xFFE1E5DF),
    surfaceVariant = Color(0xFF414940),
    onSurfaceVariant = Color(0xFFC1C9BF),
    outline = Color(0xFF8B9489),
    outlineVariant = Color(0xFF414940),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun PoshakTheme(
    themeMode: String = "system",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val darkTheme = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // Preserve the existing dynamic light appearance. Dark mode deliberately uses
    // the app's own complete Material 3 palette for consistent contrast.
    val colors = when {
        darkTheme -> PoshakDarkColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        else -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colors, content = content)
}
