package com.flyonz.ceritaria.studio.core.upload

data class VideoUploadPartAuthorization(
    val partNumber: Int,
    val sizeBytes: Long,
    val url: String,
)
