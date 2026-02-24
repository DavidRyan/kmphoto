package com.example.kmpicture.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.kmpicture.android.AndroidImageResolver
import com.example.kmpicture.android.AndroidImageSource
import com.example.kmpicture.domain.DefaultImagePickerController
import com.example.kmpicture.domain.ImagePickerController
import com.example.kmpicture.domain.PermissionProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@Composable
actual fun rememberImagePickerController(): ImagePickerController {
    val context = LocalContext.current
    val permissionProvider = rememberAndroidPermissionProvider()
    return remember(context, permissionProvider) {
        DefaultImagePickerController(
            source = AndroidImageSource(context),
            resolver = AndroidImageResolver(context),
            permission = permissionProvider,
        )
    }
}

@Composable
private fun rememberAndroidPermissionProvider(): PermissionProvider {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingResult = remember { PermissionRequestState() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        pendingResult.complete(granted)
    }
    val launcherState = rememberUpdatedState(launcher)

    return remember(context) {
        object : PermissionProvider {
            override suspend fun ensureReadAccess(): Boolean {
                val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_IMAGES
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) return true

                val deferred = CompletableDeferred<Boolean>()
                pendingResult.register(deferred)
                scope.launch {
                    launcherState.value.launch(permission)
                }
                return deferred.await()
            }
        }
    }
}

private class PermissionRequestState {
    private var deferred: CompletableDeferred<Boolean>? = null

    fun register(next: CompletableDeferred<Boolean>) {
        deferred = next
    }

    fun complete(value: Boolean) {
        deferred?.complete(value)
        deferred = null
    }
}
