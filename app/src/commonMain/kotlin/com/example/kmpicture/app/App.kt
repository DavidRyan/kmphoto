package com.example.kmpicture.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.kmpicture.domain.ImageSelection
import com.example.kmpicture.ui.ImageSelectorBottomSheet

@Composable
fun KmpictureApp(onSelected: (ImageSelection) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Button(onClick = { showPicker = true }) {
                    Text(text = "Pick image")
                }
            }

            ImageSelectorBottomSheet(
                visible = showPicker,
                onDismiss = { showPicker = false },
                onSelected = { selection ->
                    showPicker = false
                    onSelected(selection)
                },
            )
        }
    }
}
