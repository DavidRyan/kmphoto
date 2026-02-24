package com.example.kmpicture.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.kmpicture.domain.ImageAsset

@Composable
expect fun ImageThumbnail(
    asset: ImageAsset,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
)
