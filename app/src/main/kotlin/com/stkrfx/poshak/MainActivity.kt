package com.stkrfx.poshak

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val colorScheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                    if (isSystemInDarkTheme()) dynamicDarkColorScheme(context)
                    else dynamicLightColorScheme(context)

                isSystemInDarkTheme() -> darkColorScheme()
                else -> lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var count by remember { mutableStateOf(0) }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Hello, Compose!", style = MaterialTheme.typography.headlineMedium)
                        Text("Count: $count")

                        CircularWavyProgressIndicator(
                            modifier = Modifier
                                .padding(vertical = 16.dp)
                                .size(52.dp)
                        )
                        Button(onClick = { count++ }) {
                            Text("Increment")
                        }
                    }
                }
            }
        }
    }
}   