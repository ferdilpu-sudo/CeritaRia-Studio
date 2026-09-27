package com.flyonz.ceritaria.studio.core.upload

data class UploadProgress(
    val uploadedBytes: Long,
    val totalBytes: Long,
) {
    val percent: Int
        get() = if (totalBytes <= 0L) {
            0
        } else {
            ((uploadedBytes.coerceIn(0L, totalBytes) * 100L) / totalBytes).toInt()
        }
}
