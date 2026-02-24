package com.example.kmpicture.domain

interface ImageAssetSource {
    suspend fun loadPage(page: Int, pageSize: Int): List<ImageAsset>
    fun hasMore(page: Int, pageSize: Int): Boolean
}

interface ImageSelectionResolver {
    suspend fun resolve(asset: ImageAsset): ImageSelection
}

interface PermissionProvider {
    suspend fun ensureReadAccess(): Boolean
}

interface ImagePickerController {
    val source: ImageAssetSource
    val resolver: ImageSelectionResolver
    val permission: PermissionProvider
}

data class DefaultImagePickerController(
    override val source: ImageAssetSource,
    override val resolver: ImageSelectionResolver,
    override val permission: PermissionProvider,
) : ImagePickerController
