package com.example.kmpicture.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.kmpicture.domain.ImageAsset

@Composable
actual fun ImageThumbnail(
    asset: ImageAsset,
    isSelected: Boolean,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.background(
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = asset.displayName ?: "Image",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
