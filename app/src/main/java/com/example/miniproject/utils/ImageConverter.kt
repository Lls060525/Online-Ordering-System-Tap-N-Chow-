package com.example.miniproject.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import android.content.Context
import android.util.Log

class ImageConverter(private val context: Context) {

    suspend fun uriToBase64(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                // Resize bitmap to reduce file size (max 500px width/height)
                val resizedBitmap = resizeBitmap(bitmap, 500)
                return@withContext bitmapToBase64(resizedBitmap)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        var width = bitmap.width
        var height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()

        if (ratio > 1) {
            width = maxSize
            height = (maxSize / ratio).toInt()
        } else {
            height = maxSize
            width = (maxSize * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress image to reduce size (80% quality, JPEG format)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }

    fun base64ToBitmap(base64String: String): Bitmap? {
        return try {
            // Handle both formats: with and without data URI prefix
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            Log.d("ImageConverter", "Base64 string length: ${pureBase64.length}")

            val decodedBytes = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            if (bitmap == null) {
                Log.e("ImageConverter", "Failed to decode bitmap from Base64")
            } else {
                Log.d("ImageConverter", "Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")
            }

            bitmap
        } catch (e: Exception) {
            Log.e("ImageConverter", "Error decoding Base64: ${e.message}")
            e.printStackTrace()
            null
        }
    }



    fun base64ToImageBitmap(base64String: String): ImageBitmap? {
        return base64ToBitmap(base64String)?.asImageBitmap()
    }
}