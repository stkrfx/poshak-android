package com.stkrfx.poshak.ui.workspace

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun AvatarMark(avatar: String, label: String) {
    Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
        AvatarContent(avatar, label, Modifier.fillMaxSize(), MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun AvatarContent(value: String, fallbackLabel: String, modifier: Modifier, textStyle: TextStyle) {
    val bitmap = androidx.compose.runtime.remember(value) {
        runCatching {
            if (!value.startsWith("data:image/", ignoreCase = true)) null
            else {
                val comma = value.indexOf(',')
                if (comma < 0) null else {
                    val metadata = value.substring(0, comma)
                    val payload = value.substring(comma + 1)
                    val bytes = if (metadata.contains(";base64", ignoreCase = true)) Base64.decode(payload, Base64.DEFAULT) else java.net.URLDecoder.decode(payload, Charsets.UTF_8.name()).toByteArray(Charsets.ISO_8859_1)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = "Profile picture", modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        val fallback = fallbackLabel.firstOrNull()?.uppercase() ?: "P"
        Text(value.takeIf { it.isNotBlank() && !it.startsWith("data:image/") && !it.startsWith("http://") && !it.startsWith("https://") } ?: fallback, modifier = modifier, style = textStyle, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
