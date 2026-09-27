package com.flyonz.ceritaria.studio.core.upload

fun MultipartUploadState.completedBytes(totalBytes: Long): Long =
    completedParts.sumOf { part ->
        val offset = (part.partNumber - 1L) * partSizeBytes
        (totalBytes - offset).coerceIn(0L, partSizeBytes)
    }

object UploadProgressPolicy {
    private const val PERSIST_STEP_BYTES = 512L * 1024L

    fun shouldPersist(
        lastPersisted: Long,
        current: Long,
        total: Long,
    ): Boolean =
        current == total || current - lastPersisted >= PERSIST_STEP_BYTES
}
