package com.example.kmpicture.ios

import com.example.kmpicture.domain.PermissionProvider

class IosPermissions : PermissionProvider {
    override suspend fun ensureReadAccess(): Boolean {
        return true
    }
}
