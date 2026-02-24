package com.example.kmpicture.ios

import com.example.kmpicture.domain.ImageAsset
import com.example.kmpicture.domain.ImageSelection
import com.example.kmpicture.domain.ImageSelectionResolver

class IosImageResolver : ImageSelectionResolver {
    override suspend fun resolve(asset: ImageAsset): ImageSelection {
        throw NotImplementedError("iOS image resolver not wired yet")
    }
}
