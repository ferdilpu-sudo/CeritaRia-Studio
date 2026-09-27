package com.flyonz.ceritaria.studio.core.upload

sealed interface VideoUploadTarget {
    data class Single(
        val url: String,
        val headers: Map<String, String>,
    ) : VideoUploadTarget

    data class Multipart(
        val partSizeBytes: Long,
        val partCount: Int,
    ) : VideoUploadTarget
}
