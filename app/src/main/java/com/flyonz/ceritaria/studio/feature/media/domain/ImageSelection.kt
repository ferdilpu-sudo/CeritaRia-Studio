package com.flyonz.ceritaria.studio.feature.media.domain

data class ImageSelection(
    val uri: String,
    val mimeType: String,
    val sizeBytes: Long,
    val extension: String,
)
