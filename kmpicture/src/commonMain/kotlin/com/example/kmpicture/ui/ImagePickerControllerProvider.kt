package com.example.kmpicture.ui

import androidx.compose.runtime.Composable
import com.example.kmpicture.domain.ImagePickerController

@Composable
expect fun rememberImagePickerController(): ImagePickerController

@Composable
fun ImageSelectorBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelected: (com.example.kmpicture.domain.ImageSelection) -> Unit,
    onSelectedMultiple: (List<com.example.kmpicture.domain.ImageSelection>) -> Unit = {},
    selectionMode: com.example.kmpicture.domain.SelectionMode = com.example.kmpicture.domain.SelectionMode.Single,
    pageSize: Int = 60,
) {
    com.example.kmpicture.ui.ImageSelectorBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        onSelected = onSelected,
        onSelectedMultiple = onSelectedMultiple,
        controller = rememberImagePickerController(),
        selectionMode = selectionMode,
        pageSize = pageSize,
    )
}
