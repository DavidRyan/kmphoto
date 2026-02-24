package com.example.kmpicture.ios

import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImageAssetSource

class IosImageSource : ImageAssetSource {
    override suspend fun loadPage(page: Int, pageSize: Int): List<ImageAsset> {
        return emptyList()
    }

    override fun hasMore(page: Int, pageSize: Int): Boolean = false
}
