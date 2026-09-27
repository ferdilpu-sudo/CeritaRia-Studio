package com.flyonz.ceritaria.studio.core.upload

data class FinalizedVideoAsset(
    val assetId: String,
    val sizeBytes: Long,
    val etag: String?,
)
