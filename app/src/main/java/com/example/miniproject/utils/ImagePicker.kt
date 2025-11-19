package com.example.miniproject.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberImagePicker(
    onImageSelected: (Uri?) -> Unit
): ImagePicker {
    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            onImageSelected(uri)
        }
    )

    return remember {
        ImagePicker(
            context = context,
            galleryLauncher = galleryLauncher
        )
    }
}

class ImagePicker(
    private val context: Context,
    private val galleryLauncher: androidx.activity.result.ActivityResultLauncher<String>
) {
    fun pickImageFromGallery() {
        galleryLauncher.launch("image/*")
    }

    companion object {
        fun getImageUriFromIntent(data: Intent?): Uri? {
            return data?.data
        }
    }
}