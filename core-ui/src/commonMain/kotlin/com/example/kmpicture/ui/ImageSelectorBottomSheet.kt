package com.example.kmpicture.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImagePickerController
import com.example.kmpicture.domain.ImagePickerError
import com.example.kmpicture.domain.ImageSelection
import com.example.kmpicture.domain.SelectionMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImageSelectorBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSelected: (ImageSelection) -> Unit,
    onSelectedMultiple: (List<ImageSelection>) -> Unit = {},
    controller: ImagePickerController,
    selectionMode: SelectionMode = SelectionMode.Single,
    pageSize: Int = 60,
) {
    val scope = rememberCoroutineScope()
    val stateHolder = remember(controller, pageSize, scope) {
        ImagePickerStateHolder(controller, pageSize, scope)
    }
    val gridState = rememberLazyGridState()

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        stateHolder.open()
    }

    if (!visible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            HeaderRow(onDismiss)
            Spacer(modifier = Modifier.height(12.dp))

            when (val state = stateHolder.uiState) {
                ImagePickerUiState.Idle,
                ImagePickerUiState.Loading -> LoadingState()
                ImagePickerUiState.Empty -> EmptyState()
                is ImagePickerUiState.Error -> ErrorState(state.error) {
                    stateHolder.retry()
                }
                is ImagePickerUiState.Ready -> ReadyState(
                    state = state,
                    selectionMode = selectionMode,
                    gridState = gridState,
                    isLoadingMore = stateHolder.isLoadingMore,
                    onSelect = { asset ->
                        stateHolder.onSelect(asset, selectionMode) { selection ->
                            onSelected(selection)
                            onDismiss()
                        }
                    },
                    onConfirmMultiSelect = {
                        stateHolder.confirmMultiSelect { selections ->
                            onSelectedMultiple(selections)
                            onDismiss()
                        }
                    },
                    onLoadMore = {
                        stateHolder.loadMore()
                    },
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Select image",
            style = MaterialTheme.typography.titleMedium,
        )
        TextButton(onClick = onDismiss) {
            Text(text = "Close")
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Loading...", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "No images found", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorState(error: ImagePickerError, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val message = when (error) {
            ImagePickerError.PermissionDenied -> "Permission required to access photos"
            ImagePickerError.SourceUnavailable -> "Image source unavailable"
            is ImagePickerError.PlatformError -> error.message
        }
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onRetry) {
            Text(text = "Retry")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReadyState(
    state: ImagePickerUiState.Ready,
    selectionMode: SelectionMode,
    gridState: LazyGridState,
    isLoadingMore: Boolean,
    onSelect: (ImageAsset) -> Unit,
    onConfirmMultiSelect: () -> Unit,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(state, gridState) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastIndex ->
            val triggerIndex = state.assets.size - 6
            if (lastIndex >= triggerIndex) {
                onLoadMore()
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(96.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.assets, key = { it.id }) { asset ->
            val isSelected = state.selectedIds.contains(asset.id)
            AssetTile(asset, isSelected) { onSelect(asset) }
        }
    }

    if (isLoadingMore) {
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                strokeWidth = 2.dp,
            )
        }
    }

    if (selectionMode == SelectionMode.Multiple) {
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onConfirmMultiSelect,
            enabled = state.selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Select ${state.selectedIds.size} images")
        }
    }
}

@Composable
private fun AssetTile(asset: ImageAsset, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ImageThumbnail(
            asset = asset,
            isSelected = isSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = asset.displayName ?: "",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
