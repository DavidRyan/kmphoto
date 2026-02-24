package com.example.kmpicture.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImagePickerController
import com.example.kmpicture.domain.ImagePickerError
import com.example.kmpicture.domain.ImageSelection
import com.example.kmpicture.domain.SelectionMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ImagePickerStateHolder(
    private val controller: ImagePickerController,
    private val pageSize: Int,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf<ImagePickerUiState>(ImagePickerUiState.Idle)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    fun open() {
        scope.launch {
            uiState = ImagePickerUiState.Loading
            val hasPermission = controller.permission.ensureReadAccess()
            if (!hasPermission) {
                uiState = ImagePickerUiState.Error(ImagePickerError.PermissionDenied)
                return@launch
            }
            val assets = controller.source.loadPage(0, pageSize)
            uiState = if (assets.isEmpty()) {
                ImagePickerUiState.Empty
            } else {
                ImagePickerUiState.Ready(
                    assets = assets,
                    page = 0,
                    canLoadMore = controller.source.hasMore(0, pageSize),
                    selectedIds = emptySet(),
                )
            }
        }
    }

    fun retry() {
        open()
    }

    fun onSelect(
        asset: ImageAsset,
        selectionMode: SelectionMode,
        onSelected: (ImageSelection) -> Unit,
    ) {
        if (selectionMode == SelectionMode.Single) {
            scope.launch {
                runCatching { controller.resolver.resolve(asset) }
                    .onSuccess { selection ->
                        onSelected(selection)
                    }
                    .onFailure { error ->
                        uiState = ImagePickerUiState.Error(
                            ImagePickerError.PlatformError(error.message ?: "Selection failed"),
                        )
                    }
            }
        } else {
            val state = uiState
            if (state is ImagePickerUiState.Ready) {
                uiState = state.copy(
                    selectedIds = state.selectedIds
                        .toMutableSet()
                        .apply {
                            if (contains(asset.id)) remove(asset.id) else add(asset.id)
                        },
                )
            }
        }
    }

    fun confirmMultiSelect(onSelectedMultiple: (List<ImageSelection>) -> Unit) {
        val state = uiState
        if (state !is ImagePickerUiState.Ready) return
        scope.launch {
            val selections = state.assets
                .filter { state.selectedIds.contains(it.id) }
                .mapNotNull { asset ->
                    runCatching { controller.resolver.resolve(asset) }.getOrNull()
                }
            if (selections.isNotEmpty()) {
                onSelectedMultiple(selections)
            }
        }
    }

    fun loadMore() {
        val state = uiState
        if (state !is ImagePickerUiState.Ready) return
        if (isLoadingMore || !state.canLoadMore) return
        isLoadingMore = true
        scope.launch {
            val nextPage = state.page + 1
            val nextAssets = controller.source.loadPage(nextPage, pageSize)
            val merged = state.assets + nextAssets
            uiState = if (merged.isEmpty()) {
                ImagePickerUiState.Empty
            } else {
                state.copy(
                    assets = merged,
                    page = nextPage,
                    canLoadMore = controller.source.hasMore(nextPage, pageSize),
                    selectedIds = state.selectedIds,
                )
            }
            isLoadingMore = false
        }
    }
}
