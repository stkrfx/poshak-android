package com.stkrfx.poshak

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.stkrfx.poshak.data.PoshakApi
import com.stkrfx.poshak.ui.theme.PoshakTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val api = remember { PoshakApi(context.applicationContext) }
            PoshakTheme(dynamicColor = true) {
                PoshakApp(api)
            }
        }
    }
}
