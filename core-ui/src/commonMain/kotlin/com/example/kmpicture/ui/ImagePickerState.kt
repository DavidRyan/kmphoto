package com.example.kmpicture.ui

import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImagePickerError

sealed interface ImagePickerUiState {
    data object Idle : ImagePickerUiState
    data object Loading : ImagePickerUiState
    data object Empty : ImagePickerUiState
    data class Ready(
        val assets: List<ImageAsset>,
        val page: Int,
        val canLoadMore: Boolean,
        val selectedIds: Set<String>,
    ) : ImagePickerUiState

    data class Error(val error: ImagePickerError) : ImagePickerUiState
}
