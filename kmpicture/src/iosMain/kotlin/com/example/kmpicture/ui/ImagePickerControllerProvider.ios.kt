package com.example.kmpicture.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.kmpicture.domain.DefaultImagePickerController
import com.example.kmpicture.domain.ImagePickerController
import com.example.kmpicture.ios.IosImageResolver
import com.example.kmpicture.ios.IosImageSource
import com.example.kmpicture.ios.IosPermissions

@Composable
actual fun rememberImagePickerController(): ImagePickerController {
    return remember {
        DefaultImagePickerController(
            source = IosImageSource(),
            resolver = IosImageResolver(),
            permission = IosPermissions(),
        )
    }
}
