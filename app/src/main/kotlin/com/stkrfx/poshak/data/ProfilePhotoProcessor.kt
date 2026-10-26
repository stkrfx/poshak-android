package com.stkrfx.poshak.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.max

/**
 * Creates a reasonably small square JPEG data URI for the profile endpoint.
 * The API stores avatar_url as text, so a compressed data URI can be persisted without
 * requiring an additional image-upload service.
 */
object ProfilePhotoProcessor {
    private const val OUTPUT_SIZE = 384
    private const val MAX_SOURCE_EDGE = 2048
    private const val JPEG_QUALITY = 78
    private const val MAX_ENCODED_BYTES = 280 * 1024

    fun toDataUri(contentResolver: ContentResolver, uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = contentResolver.openInputStream(uri)
            ?: error("Could not open the selected photo.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "The selected file is not a readable image." }

        val largestEdge = max(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (largestEdge / sample > MAX_SOURCE_EDGE * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val source = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not decode the selected photo.")

        val square = Bitmap.createBitmap(OUTPUT_SIZE, OUTPUT_SIZE, Bitmap.Config.ARGB_8888)
        try {
            val edge = min(source.width, source.height)
            val left = (source.width - edge) / 2
            val top = (source.height - edge) / 2
            val canvas = Canvas(square)
            canvas.drawBitmap(
                source,
                Rect(left, top, left + edge, top + edge),
                Rect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE),
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG),
            )

            var quality = JPEG_QUALITY
            var bytes: ByteArray
            do {
                val output = ByteArrayOutputStream()
                square.compress(Bitmap.CompressFormat.JPEG, quality, output)
                bytes = output.toByteArray()
                quality -= 8
            } while (bytes.size > MAX_ENCODED_BYTES && quality >= 46)

            require(bytes.size <= MAX_ENCODED_BYTES) {
                "This photo could not be compressed enough. Please choose a smaller image."
            }
            return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally {
            source.recycle()
            square.recycle()
        }
    }
}
